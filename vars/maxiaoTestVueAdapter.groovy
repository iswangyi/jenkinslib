import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(defaultnamespace="") {
    def branchname = getGitBranchName()
    def istag = true

    // def projectfinal = projectpath.replace('.git', '')
    def projectfinal = "cloudstar/rightcloud-adapter"
    echo "projectfinal is: ${projectfinal}"

    def namespace = defaultnamespace

    echo "namespace is: ${namespace}"

    node {
        def dbmigrationtagname
        def servertagname
        def adaptertagname

        //def PARAM_GIT_TAGS = "release-test3.0";
        properties([parameters([
            string(name: "PARAM_GIT_TAGS", defaultValue: "", description: "tags"),

            string(name: "NAMESPACE", defaultValue: "$namespace", description: "namespace for build"),

            string(name: "VERSION_PREFIX", defaultValue: "3.2.0", description: "版本号前缀"),
        ])
        ])

        def mail;
        def dockerserver = getDockerBuildServer()
        try {
            if (!namespace.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            // 下载kubectl认证相关配置和证书，放入pod共享存储empty dir(/home/kubectl-admin)
            // pod共享存储是在jenkins系统配置下的cloud下的pod template中配置好的
            stage('git kubectl config and cert') {
                git credentialsId: 'maxiao_gitlab', url: 'http://10.68.6.20:8082/mx/kubectl-admin.git', branch: "master"
                sh 'mv ./* /home/kubectl-admin/'
            }

            stage('Prepare source code') {
                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[credentialsId: "bdfdd9e4-1bd4-4332-a46e-b78347a61eb2", url: "http://10.68.6.20:8082/${projectfinal}.git"]], branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false

                echo "use branch for build: ${branchname}"
                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }


            adaptertagname = stageBuildAdapterImage(dockerserver,
                                                    projectfinal,
                                                    branchname.split('/')[-1],
                                                    params.NAMESPACE,
                                                    istag, null, false)

            container('kubectl') {
                maxiaoTestDeployAdapter(Constants.PRODUCT_K8S_SERVER,
                                   params.NAMESPACE,
                                   adaptertagname, true)
            }

        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
