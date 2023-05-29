import cn.com.rightcloud.jenkins.Constants

def call(namespace="boss-test", k8sEnv="dev") {

    node {
      
        try {
            def kubectl = getKubectl(k8sEnv)
            stage('run auto ui test') {
                container('kubectl') {
                    def uitestPod = sh(script: """
                        echo \$(${kubectl} -n ${namespace} get pod|grep uitest|awk '{print \$1}');
                      """,returnStdout: true).trim();
                    sh "${kubectl} -n ${namespace} exec ${uitestPod} -- sh -c 'sh /usr/src/app/autotest-ui/run_work.sh'"
                }
                notifyDingDing(true, Constants.TEST_ENV, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
            
        }   catch (e) {
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
