import cn.com.rightcloud.jenkins.Constants

def call( isDeploy=false) {
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
             booleanParam(name: "QUALITY_ANALYSIS", defaultValue: true, description: "是否进行代码质量检测"),
             choice(name: "BUILD_CPU_ARCH", choices: 'AMD64\nARM64\nAMD64_ARM64', description: 'cpu架构', defaultValue: "AMD64_ARM64"),
         ])
         ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))
         try {
            parallel(
                "sonarQube task": {
                    if(params.QUALITY_ANALYSIS == "true"){
                        stageCodeQualityAnalysis()
                    }
                },
                "build & deploy": {
                    // tag_name
                    def branchname = "${PARAM_GIT_TAGS}"
                    def runtime_image_name = "imagev2.rightcloud.com.cn/${namespace}/rightcloud-schedule-runtime:v.${branchname}.${env.BUILD_NUMBER}"
                    def image_name = "imagev2.rightcloud.com.cn/${namespace}/rightcloud-schedule:v.${branchname}.${env.BUILD_NUMBER}"



                    stage("builder") {
                        // 获取Dockerfile
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                        sh 'ls -al'
                        sh 'pwd'

                        def build_args = " --network=host --build-arg SCHEDULE_PROJECT=./ "
                        def cpu_arch="linux/amd64"
                        def arch_tips=""
                        if (params.BUILD_CPU_ARCH=="AMD64"){
                            arch_tips="Build Tips:当前只构建x86镜像，请保证存在X86基础镜像"
                            cpu_arch="linux/amd64"
                        }

                        if (params.BUILD_CPU_ARCH=="ARM64"){
                            arch_tips="Build Tips:当前只构建ARM64镜像，请保证存在ARM64基础镜像"
                            cpu_arch="linux/arm64"
                        }

                        if (params.BUILD_CPU_ARCH=="AMD64_ARM64"){
                            arch_tips="Build Tips:当前同时构建X86和ARM64镜像，请保证同时存在X86和ARM64基础镜像"
                            cpu_arch="linux/amd64,linux/arm64"
                        }

                        container('docker') {
                              withDockerRegistry([credentialsId: "harbor_buildarch", url: "https://imagev2.rightcloud.com.cn"]) {
//                                sh "pwd"
//                                  sh "docker info|grep 'Experimental'"
                                //sh "docker version"
                                //sh "systemctl status docker"
                                //sh "echo '{ \"insecure-registries\":[\"10.69.5.169:8089\"] }' > /etc/docker/daemon.json"
                                //sh "docker login -u admin -p Harbor#starForever123 10.69.5.169:8089"
//                                sh "echo '安装QEMU'"
//                                sh "docker run --rm --privileged multiarch/qemu-user-static --reset -p yes"
//                                sh "echo '快速测试'"
//                                sh "docker run --rm arm64v8/alpine uname -a"
//                                sh "docker-buildx version"
//                                sh "docker-buildx create --use --name multiarhbuilder --driver-opt private-ca-cert=/etc/docker/certs.d/imagev2.rightcloud.com.cn/ca.crt"
//                                sh "docker buildx inspect multiarhbuilder --bootstrap"
//                                sh "echo ${arch_tips}"
//                                sh "docker-buildx build --platform  ${cpu_arch}  -t ${image_name} --no-cache -f docker/Dockerfile.arch . ${build_args} --output type=image,name=imagev2.rightcloud.com.cn:443,push=true"
//                                sh "echo 'Success Build Image Tag: ${image_name}'"
                                  sh "echo ${arch_tips}"
                                  if (params.BUILD_CPU_ARCH=="ARM64"){
                                      sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                      sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile.arm64 . ${build_args}"
                                      sh "docker push ${image_name}"
                                      sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_name}'"
                                  }

                                  if (params.BUILD_CPU_ARCH=="AMD64"){
                                      sh "docker build -t ${image_name} --target mainjar --no-cache -f docker/Dockerfile.light . ${build_args}"
                                      sh "docker push ${image_name}"
                                      sh "echo '>>>>>>>>>lgtag:应用包AMD64镜像构建完成并上传成功，应用包AMD镜像号为：${image_name}'"
                                      sh "docker build -t ${runtime_image_name} --target runtime -f docker/Dockerfile.light . ${build_args}"
                                      sh "docker push ${runtime_image_name}"
                                      sh "echo '>>>>>>>>>lgtag:运行时AMD64镜像构建完成并上传成功，运行时AMD镜像号为：${runtime_image_name}'"
                                  }

                                  if (params.BUILD_CPU_ARCH=="AMD64_ARM64"){
                                      sh "docker build --platform=linux/amd64 -t ${image_name} --no-cache -f docker/Dockerfile . ${build_args}"
                                      sh "docker push ${image_name}"
                                      sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${image_name}'"
                                      sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                      sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile.arm64 . ${build_args}"
                                      sh "docker push ${image_name}"
                                      sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_name}'"
                                      sh "echo 'AMD64/ARM64镜像都构建完成！！'"
                                  }





                            }
                        }
                    }

                    if (isDeploy) {
                        stageK8ServiceDeploy("adapter", namespace, kubectl, image_name, false)
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
