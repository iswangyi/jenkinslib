import cn.com.rightcloud.jenkins.Constants
def call() {
    stage('SonarQube analysis') {
        container("kubectl"){
            withSonarQubeEnv('clustersonar') {
                sh "sonar-scanner"
            }
        }
    }
//    stage("Quality gate") {
//        timeout(time: 5, unit: 'MINUTES') {
//            def qg = waitForQualityGate(abortPipeline: false)
//            if (qg.status != 'OK') {
//                error "Pipeline aborted due to quality gate failure: ${qg.status}"
////                notifyDingDing(false, Constants.TEST_ENV, Constants.QUALITY_GATE_FAILURE_MESSAGE, Constants.QUALITY_BUILD_NOTIFY_PEOPEL)
//            }
//            echo "Quality Gate Pass: ${qg.status}"
//        }
//    }
}
