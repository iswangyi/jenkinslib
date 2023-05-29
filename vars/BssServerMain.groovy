import cn.com.rightcloud.jenkins.Constants

// env  project、dev、demo、self_test
def call(isDeploy = true) {

    node {
        properties([parameters([
                [$class            : 'GitParameterDefinition',
                 name              : 'PARAM_GIT_TAGS',
                 description       : ' tags',
                 type              : 'PT_BRANCH_TAG',
                 branch            : '',
                 branchFilter      : '.*',
                 tagFilter         : '*',
                 sortMode          : 'DESCENDING_SMART',
                 defaultValue      : 'release-test3.0',
                 selectedValue     : 'NONE',
                 quickFilterEnabled: true],
                 choice(name: "IS_DEPLOY", choices: 'true\nfalse', description: '是否部署到环境,测试人员使用时需要选择False'),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        try {
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/bss-server:v.${branchname}.${env.BUILD_NUMBER}"

            stage("build") {
                parallel(
                        "clone bss": {
                            dir('/home/jenkins/workspace/rightcloud-bss') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }
                        },
                        "clone common": {
                            dir('/home/jenkins/workspace/rightcloud-common') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/cloudstar/cloud-boss/rightcloud-common.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }
                        },
                        "clone oss": {
                            dir('/home/jenkins/workspace/rightcloud-oss') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/cloudstar/cloud-boss/rightcloud-oss.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }
                        }
                )
                def  build_args = " --network=host --build-arg   BSS_PROJECT=./rightcloud-bss " +
                        "--build-arg COMMON_PROJECT=./rightcloud-common " +
                        "--build-arg OSS_PROJECT=./rightcloud-oss "

                container('docker') {
                    dir('/home/jenkins/workspace') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            sh "docker build -t ${image_name} --no-cache -f rightcloud-bss/docker/Dockerfile --no-cache . ${build_args}"
                            sh "docker push ${image_name}"
                        }
                    }
                }
            }

            if (isDeploy && params.IS_DEPLOY == "true") {
                stageK8ServiceDeploy("bss", namespace, kubectl, image_name, false, 'bss')
            }
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
        } finally {
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
