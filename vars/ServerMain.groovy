import cn.com.rightcloud.jenkins.Constants

// groovy的数组截取是左开右开
def filterGroup(repo) {
    return repo ? repo.split('/')[0..-2].join('/') : "error"
}

def filterProject(repo) {
    return repo ? repo.split('/')[-1] : "error"
}


def call(isDeploy="true") {
    node {
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
            choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'), string(name: "VERSION_PREFIX", defaultValue: "$params.VERSION_PREFIX", description: "版本号前缀"),
        ])
         ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))
        // 获取Dockerfile 和 db-resource
        def branchname = "${PARAM_GIT_TAGS}"
        checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
        sh 'rm -rf .git && rm -rf .idea'
        def image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-server:v.${branchname}.${env.BUILD_NUMBER}"

         try {
            parallel(
                failFast: true,
                "build & deploy": {

                    stage("build server image") {
                        //准备Dockerfile所需的--build-arg参数

                        def build_args =   "" +
                                         "--build-arg SERVER_PROJECT=./ "

                        container('docker') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {

                                sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile --no-cache . ${build_args}"

                                sh "docker push ${image_name}"
                            }
                        }
                    }

                    if (isDeploy) {
                        container('kubectl') {
//                            echo "updating version information"
//                                def versioninfo=params.VERSION_PREFIX.trim()
//                                def updateversionstdout = sh (
//                                        script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); mysqluser=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(${kubectl} -n ${namespace} get pod |grep rightcloud-mysql | grep Running | awk '{print \$1}'); ${kubectl} -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h127.0.0.1 -u\${mysqluser} -p\${mysqlpwd} rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
//                                        returnStdout: true
//                                ).trim();
//                            echo "update version stdout: ${updateversionstdout}"

                            dir("kubernetes-yml") {
                                stageK8ServiceDeploy("server", namespace, kubectl, image_name, false)
                            }
                        }
                    }
                    notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
                }
            )
         } catch (e) {
            def errorMessage = e.getMessage()
            println(errorMessage)
            println("current result:" + currentBuild.result)
            if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
                println("FAILURE send message info")
                notifyDingDing(false, namespace, Constants.BUILD_FAILURE_MESSAGE + "\n" + errorMessage, Constants.BUILD_NOTIFY_PEOPEL)
            } else {
                println("ABORTED not send info")
            }
            error("Failed build as " + e.getMessage())
        }finally{
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
