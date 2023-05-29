import cn.com.rightcloud.jenkins.Constants

// env  project、dev、demo、self_test
def call(isDeploy=true) {
//    def kubectl = getKubectl(k8sEnv)

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
//                choice(name: "NAMESPACE",choices:'cmdb', defaultValue: "$params.NAMESPACE", description: "集群namespace"),
//             choice(name: "K8S_ENV", choices:'dev\nproject\nself_env\nprod\noem', defaultValue: "$params.K8S_ENV", description: "k8s集群环境"),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))
//        def kubectl = getKubectl(getK8sEnv(namespace))

        try {
            // parallel(
            // "sonarQube task": {
            //     stageCodeQualityAnalysis()
            // },
            // "build & deploy": {

            // tag_name
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/cmdb-gateway:v.${branchname}.${env.BUILD_NUMBER}"

            stage("build") {
                // 获取Dockerfile
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                sh 'rm -rf .git && rm -rf .idea'
                def build_args = " --build-arg GATEWAY_PROJECT=./  "

                container('docker') {
                    // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                        sh "docker build -t ${image_name} --no-cache -f Dockerfile --no-cache . ${build_args}"
                        sh "docker push ${image_name}"
                    }
                }
            }

            if (isDeploy) {
                stageK8ServiceDeploy("cmdb-gateway", namespace, kubectl, image_name, false, 'cmdb')
            }
            //     }
            // )
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
