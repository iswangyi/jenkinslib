import cn.com.rightcloud.jenkins.Constants

// env  project、dev、demo、self_test
def call(namespace="", k8sEnv="project", repo_paths, isDeploy=true) {
    def kubectl = getKubectl(k8sEnv)

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
         ])
         ])

         try {
            // parallel(
                // "sonarQube task": {
                //     stageCodeQualityAnalysis()
                // },
                // "build & deploy": {

                    // tag_name
                    def branchname = "${PARAM_GIT_TAGS}"
                    def image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-gateway:v.${branchname}.${env.BUILD_NUMBER}"

                    stage("build") {
                        // 获取Dockerfile
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false

                        // 准备Dockerfile所需的--build-arg参数
                        def gateway_repo_path=repo_paths.get("gateway")
                        def module_support_repo_path=repo_paths.get("moduleSupport")

                        // groovy的数组截取是左开右开
                        def GATEWAY_GROUP = gateway_repo_path.split('/')[0..-2].join('/')
                        def GATEWAY_PROJECT = gateway_repo_path.split('/')[-1]
                        def MODULE_SUPPORT_GROUP = module_support_repo_path.split('/')[0..-2].join('/')
                        def MODULE_SUPPORT_PROJECT = module_support_repo_path.split('/')[-1]

                        def  build_args = " --network=host --build-arg   BRANCH=${branchname} " +
                                         "--build-arg GATEWAY_GROUP=${GATEWAY_GROUP} " +
                                         "--build-arg GATEWAY_PROJECT=${GATEWAY_PROJECT} " +
                                         "--build-arg MODULE_SUPPORT_GROUP=${MODULE_SUPPORT_GROUP} " +
                                         "--build-arg MODULE_SUPPORT_PROJECT=${MODULE_SUPPORT_PROJECT} "

                        container('docker') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                                sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile --no-cache . ${build_args}"
                                sh "docker push ${image_name}"
                            }
                        }
                    }

                    if (isDeploy) {
                        container('kubectl') {
                            dir("kubernetes-yml") {
                                stage('clone yaml template') {
                                    git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
                                }
                                stage('run in k8s') {
                                    def templateFile = "rightcloud/app/rightcloud-gateway-zhuxian-template.yaml"
                                    def list1 = ['vue-project', 'rightcloud-master', 'oem-project', 'guanwang-project']
                                    if (list1.contains(namespace)) {
                                        templateFile = "rightcloud/app/rightcloud-gateway-zhuxian-template396.yaml"
                                    }
                                    sh " sed -i 's|\$gatewayTag|'${image_name}'|g'   ${templateFile}"
                                    sh " sed -i 's|\$namespace|'${namespace}'|g' ${templateFile}"
                                    sh "${kubectl} apply -f  ./${templateFile}"
                                }
                            }
                        }
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