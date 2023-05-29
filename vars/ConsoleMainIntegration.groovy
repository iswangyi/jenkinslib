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
             choice(name: "NAMESPACE",choices:'cmp-dev-a\ncmp-dev-b\ncmp-dev-c', defaultValue: "$params.NAMESPACE", description: "集群namespace"),
//             choice(name: "K8S_ENV", choices:'dev\nproject\nself_env\nprod\noem', defaultValue: "$params.K8S_ENV", description: "k8s集群环境"),
         ])
         ])
//        def kubectl = getKubectl(params.K8S_ENV)
        def namespace = params.NAMESPACE
        def kubectl = getKubectl(getK8sEnv(namespace))

         try {
            // parallel(
                // "sonarQube task": {
                //     stageCodeQualityAnalysis()
                // },
                // "build & deploy": {

                    // tag_name
                    def branchname = "${PARAM_GIT_TAGS}"
                    def image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-console:v.${branchname}.${env.BUILD_NUMBER}"

                    stage("build") {
                        // 获取Dockerfile
                        stage('clone console') {
                            dir('/home/jenkins/workspace/rightcloud-console'){
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                            }
                        }
                        stage('clone doc') {
                            dir('/home/jenkins/workspace/rightcloud-doc'){
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-v4/rightcloud-doc.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                            }
                        }
                        stage('clone backup') {
                            dir('/home/jenkins/workspace/rightcloud-console-cloudbackup'){
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-backup/rightcloud-console-cloudbackup.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                            }
                        }

                        sh 'ls -al'
                        sh 'pwd'
                        sh 'ls -al ../'
                        // 准备Dockerfile所需的--build-arg参数
                        def  build_args = " --network=host --build-arg   CONSOLE_PROJECT=./rightcloud-console " +
                                         "--build-arg DOC_PROJECT=./rightcloud-doc " +
                                         "--build-arg BACKUP_PROJECT=./rightcloud-console-cloudbackup "


                        container('docker') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                                dir('/home/jenkins/workspace'){
                                    sh "docker build -t ${image_name} --no-cache -f rightcloud-console/docker/Dockerfile.integration --no-cache . ${build_args}"
                                    sh "docker push ${image_name}"
                                }
                            }
                        }
                    }

                    if (isDeploy) {
                        stageK8ServiceDeploy("console", namespace, kubectl, image_name, false)
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
