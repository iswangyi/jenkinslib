import cn.com.rightcloud.jenkins.Constants

// groovy的数组截取是左开右开
def filterGroup(repo) {
    return repo ? repo.split('/')[0..-2].join('/') : "error"
}

def filterProject(repo) {
    return repo ? repo.split('/')[-1] : "error"
}


def call(isDeploy=false) {
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
             booleanParam(name: "QUALITY_ANALYSIS", defaultValue: false, description: "是否进行代码质量检测"),
             choice(name: "BUILD_CPU_ARCH", choices: 'AMD64\nARM64\nBOTH_AMD64_ARM64', description: 'cpu架构', defaultValue: "AMD64_ARM64"),
         ])
         ])

        def namespace = env.NAMESPACE_INJECT
        // def kubectl = getKubectl(getK8sEnv(namespace))


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

        // 获取Dockerfile 和 db-resource
         def branchname = "${PARAM_GIT_TAGS}"
         checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
         def arm64_image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-dbmigration:v.arm64.${branchname}.${env.BUILD_NUMBER}"
         def arm64_image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-dbmigration:v.arm64.${branchname}.${env.BUILD_NUMBER}"
         
         def amd64_image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-dbmigration:v.amd64.${branchname}.${env.BUILD_NUMBER}"
         def amd64_image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-dbmigration:v.amd64.${branchname}.${env.BUILD_NUMBER}"
         

         try {
            parallel(
                failFast: true,
                "build & deploy": {
                    
                    stage("build db migration image") {
                        //准备Dockerfile所需的--build-arg参数

                        container('docker') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                                sh 'ls -al'
                                sh 'pwd'

                                sh "echo ${arch_tips}"
                                if (params.BUILD_CPU_ARCH=="ARM64"){
                                    sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                    sh "docker build -t ${arm64_image_name} --no-cache -f docker/Dockerfile.arm64 . "
                                    sh "docker push ${arm64_image_name}"
                                    sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${arm64_image_name}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${arm64_image_name} ${arm64_image_name_huaweiyun}"
                                    sh "docker push ${arm64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${arm64_image_name_huaweiyun}'"
                                }

                                if (params.BUILD_CPU_ARCH=="AMD64"){
                                    sh "docker build --platform=linux/amd64 -t ${amd64_image_name} --no-cache -f docker/Dockerfile . "
                                    sh "docker push ${amd64_image_name}"
                                    sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${amd64_image_name}'"

                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${amd64_image_name} ${amd64_image_name_huaweiyun}"
                                    sh "docker push ${amd64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取AMD64-------tag：${amd64_image_name_huaweiyun}'"
                                }

                                if (params.BUILD_CPU_ARCH=="BOTH_AMD64_ARM64"){
                                  
                                    sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                    sh "docker build -t ${arm64_image_name} --no-cache -f docker/Dockerfile.arm64 . "
                                    sh "docker push ${arm64_image_name}"
                                    sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${arm64_image_name}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${arm64_image_name} ${arm64_image_name_huaweiyun}"
                                    sh "docker push ${arm64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${arm64_image_name_huaweiyun}'"


                                    sh "docker build --platform=linux/amd64 -t ${amd64_image_name} --no-cache -f docker/Dockerfile . "
                                    sh "docker push ${amd64_image_name}"
                                    sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${amd64_image_name}'"

                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${amd64_image_name} ${amd64_image_name_huaweiyun}"
                                    sh "docker push ${amd64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取AMD64-------tag：${amd64_image_name_huaweiyun}'"

                                }


                            }
                        }
                    }

//                    if (isDeploy) {
//
//                        stageK8ServiceDeploy("dbmigration", namespace, kubectl, image_name, true)
//                    }
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
