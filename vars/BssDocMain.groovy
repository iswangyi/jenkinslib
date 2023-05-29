import cn.com.rightcloud.jenkins.Constants

def call(isDeploy="true") {
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
                 choice(name: "IS_DEPLOY", choices: 'true\nfalse', description: '是否部署到环境,测试人员使用时需要选择False'),

        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        try {
            // tag_name
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/bss-doc:v.${branchname}.${env.BUILD_NUMBER}"

            stage("build") {
                // 获取Dockerfile
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                sh 'rm -rf .git && rm -rf .idea'
                stage('build doc image'){
                    // 准备Dockerfile所需的--build-arg参数
                    def  build_args = " --network=host --build-arg   DOC_PROJECT=./ "
                    container('docker') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile --no-cache . ${build_args}"
                            sh "docker push ${image_name}"
                        }
                    }
                }
            }
            if (isDeploy && params.IS_DEPLOY == "true") {
                stageK8ServiceDeploy("bss-doc", namespace, kubectl, image_name, false, 'bss')
            }
            notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
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
