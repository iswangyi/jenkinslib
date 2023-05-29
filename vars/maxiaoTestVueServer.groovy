import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(defaultnamespace="") {
    def branchname = getGitBranchName()
    def istag = true

    def projectfinal = "cloudstar/rightcloud"
    echo "projectfinal is: ${projectfinal}"

    def namespace
    if (!defaultnamespace.trim()) {
        namespace = splited[-2] + "-project"
    } else {
        namespace = defaultnamespace
    }

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
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[credentialsId: "bdfdd9e4-1bd4-4332-a46e-b78347a61eb2", url: "http://10.68.6.20:8082/cloudstar/rightcloud-server.git"]], branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false

                echo "use branch for build: ${branchname}"
                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            /*stage('Build server') {
                compileServer()
            }*/

            dbmigrationtagname = stageCreateDBMigration(dockerserver,
                                                        projectfinal,
                                                        branchname.split('/')[-1],
                                                        params.NAMESPACE,
                                                        istag, null, false)

            servertagname = stageBuildServerImage(dockerserver,
                                                  projectfinal,
                                                  branchname.split('/')[-1],
                                                  params.NAMESPACE,
                                                  istag, null, false)

            container('kubectl') {
                // def mysqlport=getMysqlPort(Constants.PRODUCT_K8S_SERVER,
                //                            params.NAMESPACE)
                // if (!mysqlport.trim()) {
                //     error("failed to get mysql port from ${params.NAMESPACE} @ ${Constants.PRODUCT_K8S_SERVER}")
                // }

                if (!params.VERSION_PREFIX.trim()) {
                    error("version prefix is empty")
                }

                echo "updating version information"
                def versioninfo=params.VERSION_PREFIX.trim()
                def k8sserver="${Constants.PRODUCT_K8S_SERVER}"
                def updateversionstdout = sh (
                    script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(kubectl -s ${k8sserver} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(kubectl -s ${k8sserver} -n ${namespace} get pod |grep rightcloud-mysql | grep Running | awk '{print \$1}'); kubectl -s ${k8sserver} -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h127.0.0.1 -uroot -p\${mysqlpwd} rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
                    returnStdout: true
                ).trim();
                echo "update version stdout: ${updateversionstdout}"

                // inject version info

                maxiaoTestDeployServer(Constants.PRODUCT_K8S_SERVER,
                                  params.NAMESPACE,
                                  dbmigrationtagname,
                                  servertagname, true)
            }

        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
