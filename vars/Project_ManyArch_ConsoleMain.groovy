import cn.com.rightcloud.jenkins.Constants

def call(isDeploy = false) {
//    def kubectl = getKubectl(k8sEnv)

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
                choice(name: "BUILD_CPU_ARCH", choices: 'AMD64\nARM64\nBOTH_AMD64_ARM64', description: 'cpu架构', defaultValue: "AMD64_ARM64"),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        // def kubectl = getKubectl(getK8sEnv(namespace))

        try {
            // tag_name
            def branchname = "${PARAM_GIT_TAGS}"
            def arm64_image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-console:v.arm64.${branchname}.${env.BUILD_NUMBER}"
            def arm64_image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-console:v.arm64.${branchname}.${env.BUILD_NUMBER}"
            
            def amd64_image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-console:v.amd64.${branchname}.${env.BUILD_NUMBER}"
            def amd64_image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-console:v.amd64.${branchname}.${env.BUILD_NUMBER}"

            stage("build") {
                // 获取Dockerfile
                parallel(
                        failFast: true,
                        "clone console": {
                            dir('/home/jenkins/workspace/rightcloud-console') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }
                        },
                        "clone modules": {
                                println('clone backup module')
                                dir('/home/jenkins/workspace/rightcloud-console-cloudbackup') {
                                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.182.0.74:8443/rightcloud-modules/cloud-backup/rightcloud-console-cloudbackup.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                    sh 'rm -rf .git && rm -rf .idea'
                                }
                                println('clone container module')
                                dir('/home/jenkins/workspace/rightcloud-console-container') {
                                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.182.0.74:8443/rightcloud-modules/cloud-container/rightcloud-console-cloudcontainer.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                    sh 'rm -rf .git && rm -rf .idea'
                                }
                        }
                )
//                def third_party_build_args = "";
//                stage('prepare dockerfile') {
//                    def docker_file_template = '/home/jenkins/workspace/rightcloud-console/docker/Dockerfile.template'
//                    def docker_file_content = readFile(file: docker_file_template)
//                    def replace_content_1 = ''
//                    def replace_content_2 = ''
//                    def replace_content_3 = ' && ls '
//                    def replace_content_4 = ''
//
//                    if (params.BUILD_THIRD_PARTY_MODULES == 'true') {
//                        // 云备份部分
//                        replace_content_1 += "ARG BACKUP_PROJECT\n"
//                        replace_content_2 += 'COPY ${BACKUP_PROJECT} /user/share/backup/ \n'
//                        replace_content_3 += '\\ \n && cd /user/share/backup/ && cnpm install && npm run build & '
//                        replace_content_4 += 'COPY --from=builder /user/share/backup/dist/ /usr/share/nginx/html/plugin/backup\n'
//                        third_party_build_args += " --build-arg BACKUP_PROJECT=./rightcloud-console-cloudbackup "
//                        // 云容器部分
//                        replace_content_1 += "ARG CONTAINER_PROJECT\n"
//                        replace_content_2 += 'COPY ${CONTAINER_PROJECT} /user/share/container/ \n'
//                        replace_content_3 += '\\ \n  cd /user/share/container/ && cnpm install && npm run build & '
//                        replace_content_4 += 'COPY --from=builder /user/share/container/dist/ /usr/share/nginx/html/plugin/container\n'
//                        third_party_build_args += " --build-arg CONTAINER_PROJECT=./rightcloud-console-container "
//
//                        replace_content_3 += ' wait && echo "done" '
//                    }
//                    docker_file_content = docker_file_content.replace("#hook1", replace_content_1)
//                    docker_file_content = docker_file_content.replace("#hook2", replace_content_2)
//                    docker_file_content = docker_file_content.replace("#hook3", replace_content_3)
//                    docker_file_content = docker_file_content.replace("#hook4", replace_content_4)
//                    println(docker_file_content)
//                    writeFile(file: docker_file_template, text: docker_file_content)
//                }
                stage('build console image') {
                    // 准备Dockerfile所需的--build-arg参数
                    def third_party_build_args = " --build-arg CONTAINER_PROJECT=./rightcloud-console-container " +
                            " --build-arg BACKUP_PROJECT=./rightcloud-console-cloudbackup "
                    def build_args = " --build-arg CONSOLE_PROJECT=./rightcloud-console " +
                            " --build-arg DOC_PROJECT=./rightcloud-doc " + third_party_build_args
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
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            dir('/home/jenkins/workspace') {

                                sh "echo ${arch_tips}"
                                if (params.BUILD_CPU_ARCH=="ARM64"){
                                    sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                    sh "docker build -t ${arm64_image_name} --no-cache -f rightcloud-console/docker/Dockerfile.arm64 . ${build_args}"
                                    sh "docker push ${arm64_image_name}"
                                    sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${arm64_image_name}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${arm64_image_name} ${arm64_image_name_huaweiyun}"
                                    sh "docker push ${arm64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${arm64_image_name_huaweiyun}'"
                                }

                                if (params.BUILD_CPU_ARCH=="AMD64"){
                                    sh "docker build --platform=linux/amd64 -t ${amd64_image_name} --no-cache -f rightcloud-console/docker/Dockerfile . ${build_args}"
                                    sh "docker push ${amd64_image_name}"
                                    sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${amd64_image_name}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${amd64_image_name} ${amd64_image_name_huaweiyun}"
                                    sh "docker push ${amd64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${amd64_image_name_huaweiyun}'"
                                }

                                if (params.BUILD_CPU_ARCH=="BOTH_AMD64_ARM64"){
                                  
                                    sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                    sh "docker build -t ${arm64_image_name} --no-cache -f rightcloud-console/docker/Dockerfile.arm64 . ${build_args}"
                                    sh "docker push ${arm64_image_name}"
                                    sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${arm64_image_name}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${arm64_image_name} ${arm64_image_name_huaweiyun}"
                                    sh "docker push ${arm64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${arm64_image_name_huaweiyun}'"


                                    sh "docker build --platform=linux/amd64 -t ${amd64_image_name} --no-cache -f rightcloud-console/docker/Dockerfile . ${build_args}"
                                    sh "docker push ${amd64_image_name}"
                                    sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${amd64_image_name}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${amd64_image_name} ${amd64_image_name_huaweiyun}"
                                    sh "docker push ${amd64_image_name_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${amd64_image_name_huaweiyun}'"

                                }


                            }
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
        } finally {
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
