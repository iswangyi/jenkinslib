import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(defaultnamespace="") {
    def branchname = getGitBranchName()
    def istag = true

    def projectfinal = "cloudstar/rightcloud-schedule"
    echo "projectfinal is: ${projectfinal}"

    def namespace
    if (!defaultnamespace.trim()) {
        namespace = splited[-2] + "-project"
    } else {
        namespace = defaultnamespace
    }

    node {
        def scheduletagname

        //def PARAM_GIT_TAGS = "release-test3.0";

        properties([parameters([
            string(name: "PARAM_GIT_TAGS", defaultValue: "", description: "tags"),

            string(name: "NAMESPACE", defaultValue: "${namespace}", description: "namespace for build"),
        ])
        ])
        
        def dockerserver = getDockerBuildServer()

        def mail;
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
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[credentialsId: "bdfdd9e4-1bd4-4332-a46e-b78347a61eb2", url: "http://10.68.6.20:8082/${projectfinal}.git"]], branches: [[name: branchname]]], poll: false

                echo "use branch for build: ${branchname}"

                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            /*compileSchedule()*/
            scheduletagname = stageBuildScheduleImage(dockerserver,
                                                      projectfinal,
                                                      branchname.split('/')[-1],
                                                      params.NAMESPACE,
                                                      istag, null, false)
            container('kubectl') {
                maxiaoTestDeploySchedule(Constants.PRODUCT_K8S_SERVER,
                                    params.NAMESPACE,
                                    scheduletagname, true)
            }
        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
