import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(defaultnamespace="", isDeploy=true) {
    def branchname = getGitBranchName()
    def istag = true

    def scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
    def splited = scmUrl.split('/')
    echo "scmUrl is: ${scmUrl}"
    echo "splited is: ${splited}"
    def projectpath = splited[3..-1].join('/')
    echo "projectpath is: ${projectpath}"
    def projectfinalServer = projectpath.replace('-server', '')
    def projectfinal = projectfinalServer.replace('.git', '')
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
            [$class: 'GitParameterDefinition',
             name: 'PARAM_GIT_TAGS',
             description: ' tags',
             type:'PT_BRANCH_TAG',
             branch: '',
             branchFilter: '.*',
             tagFilter:'*',
             sortMode:'DESCENDING_SMART',
             defaultValue: 'release-test3.0',
             selectedValue:'NONE',
             quickFilterEnabled: true],

            string(name: "NAMESPACE", defaultValue: "$namespace", description: "namespace for build"),

            string(name: "VERSION_PREFIX", defaultValue: "3.2.0", description: "版本号前缀"),
            choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'),
        ])
        ])

        def mail;
//        def dockerserver = getDockerBuildServer()
        def dockerserver;
        try {
            if (!namespace.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            // 下载kubectl认证相关配置和证书，放入pod共享存储empty dir(/home/kubectl-admin)
            // pod共享存储是在jenkins系统配置下的cloud下的pod template中配置好的
//            stage('git kubectl config and cert') {
//                git credentialsId: 'maxiao_gitlab', url: 'http://10.68.6.20:8082/mx/kubectl-admin.git', branch: "master"
//                sh 'mv ./* /home/kubectl-admin/'
//            }

            stage('Prepare source code') {
                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false

                echo "use branch for build: ${branchname}"
                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            /*stage('Build server') {
                compileServer()
            }*/
            parallel (
                    failFast: true,
                    "build & validate dbmigration & sonar scan": {
                        dbmigrationtagname = stageCreateDBMigration(dockerserver,
                                projectfinal,
                                branchname.split('/')[-1],
                                params.NAMESPACE,
                                istag, null, false)
                        if(params.QUALITY_ANALYSIS == "true"){
                            stageCodeQualityAnalysis()
                        }

                    },
                    "build server image": {
                        servertagname = stageBuildServerImage(dockerserver,
                                projectfinal,
                                branchname.split('/')[-1],
                                params.NAMESPACE,
                                istag, null, false)

                        if (isDeploy) {

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
                                        script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(kubectl --kubeconfig=/home/kubectl-cer/config -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); mysqluser=\$(kubectl --kubeconfig=/home/kubectl-cer/config -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(kubectl --kubeconfig=/home/kubectl-cer/config -n ${namespace} get pod |grep rightcloud-mysql | grep Running | awk '{print \$1}'); kubectl --kubeconfig=/home/kubectl-cer/config -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h127.0.0.1 -u\${mysqluser} -p\${mysqlpwd} rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
                                        returnStdout: true
                                ).trim();
                                echo "update version stdout: ${updateversionstdout}"

                                // inject version info

                                vueDeployServer(Constants.PRODUCT_K8S_SERVER,
                                        params.NAMESPACE,
                                        dbmigrationtagname,
                                        servertagname, true)
                            }

                            notifyDingDing(true, Constants.TEST_ENV, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
                        }
                    }
            )
            
        }  catch (e) {
            def errorMessage = e.getMessage()
            println(errorMessage)
            println("current result:" + currentBuild.result)
//            notifyFailed(mail)
            if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
                println("FAILURE send message info")
                notifyDingDing(false, Constants.TEST_ENV, Constants.BUILD_FAILURE_MESSAGE + "\n" + errorMessage, Constants.BUILD_NOTIFY_PEOPEL)
            } else {
                println("ABORTED not send info")
            }
            error("Failed build as " + e.getMessage())
        }finally{
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, Constants.TEST_ENV, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
