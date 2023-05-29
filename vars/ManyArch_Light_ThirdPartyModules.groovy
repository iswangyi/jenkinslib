import cn.com.rightcloud.jenkins.Constants


def call(isDeploy = false) {
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
                choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'), string(name: "VERSION_PREFIX", defaultValue: "$params.VERSION_PREFIX", description: "版本号前缀"),
                choice(name: "BUILD_CPU_ARCH", choices: 'AMD64\nARM64\nAMD64_ARM64', description: 'cpu架构', defaultValue: "AMD64_ARM64"),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        // 获取Dockerfile 和 db-resource
        def branchname = "${PARAM_GIT_TAGS}"
        parallel(
                failFast: true,
                "clone third party": {
                    dir('/home/jenkins/workspace/third-party') {
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                },
                "clone backup": {
                    dir('/home/jenkins/workspace/backup') {
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-backup/rightcloud-module-cloudbackup.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                },
                "clone container": {
                    dir('/home/jenkins/workspace/container') {
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-container/rightcloud-module-cloudcontainer.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                }
        )
        def runtime_image_name = "imagev2.rightcloud.com.cn/${namespace}/third-party-runtime:v.${branchname}.${env.BUILD_NUMBER}"
        def image_name = "imagev2.rightcloud.com.cn/${namespace}/third-party:v.${branchname}.${env.BUILD_NUMBER}"


        def db_image_name = "imagev2.rightcloud.com.cn/${namespace}/third-party-dbmigration:v.${branchname}.${env.BUILD_NUMBER}"
        //准备Dockerfile所需的--build-arg参数
        def build_args = " --network=host --build-arg BACKUP_PROJECT=./backup " +
                "--build-arg CONTAINER_PROJECT=./container " +
                "--build-arg SUMMARY_PROJECT=./third-party "
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
        sh "echo ${arch_tips}"
        try {

            stage("build dbmigration image") {
                container('docker') {
                    dir('/home/jenkins/workspace') {
                        withDockerRegistry([credentialsId: "harbor_buildarch", url: "https://imagev2.rightcloud.com.cn"]) {
                            if (params.BUILD_CPU_ARCH=="ARM64"){
                                sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                sh "docker build -t ${db_image_name} --no-cache -f third-party/docker/Dockerfile.db.arm64 . ${build_args}"
                                sh "docker push ${db_image_name}"
                                sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${db_image_name}'"
                            }

                            if (params.BUILD_CPU_ARCH=="AMD64"){
                                sh "docker build --platform=linux/amd64 -t ${db_image_name} --no-cache -f third-party/docker/Dockerfile.db . ${build_args}"
                                sh "docker push ${db_image_name}"
                                sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${db_image_name}'"
                            }

                            if (params.BUILD_CPU_ARCH=="AMD64_ARM64"){
                                sh "docker build --platform=linux/amd64 -t ${db_image_name} --no-cache -f third-party/docker/Dockerfile.db . ${build_args}"
                                sh "docker push ${db_image_name}"
                                sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${db_image_name}'"
                                sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                sh "docker build -t ${db_image_name} --no-cache -f third-party/docker/Dockerfile.db.arm64 . ${build_args}"
                                sh "docker push ${db_image_name}"
                                sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${db_image_name}'"
                                sh "echo 'AMD64/ARM64镜像都构建完成！！'"
                            }



                        }
                    }
                }
            }
            if (params.QUALITY_ANALYSIS == "true") {
                stageCodeQualityAnalysis()
            }
            sh "echo ${arch_tips}"
            stage("build server image") {
                container('docker') {
                    dir('/home/jenkins/workspace') {
                        withDockerRegistry([credentialsId: "harbor_buildarch", url: "https://imagev2.rightcloud.com.cn"]) {

                            if (params.BUILD_CPU_ARCH=="ARM64"){
                                sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                sh "docker build -t ${image_name} --no-cache -f third-party/docker/Dockerfile.arm64 . ${build_args}"
                                sh "docker push ${image_name}"
                                sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_name}'"
                            }

                            if (params.BUILD_CPU_ARCH=="AMD64"){
                                sh "docker build -t ${image_name} --target mainjar --no-cache -f third-party/docker/Dockerfile.light . ${build_args}"
                                sh "docker push ${image_name}"
                                sh "echo '>>>>>>>>>lgtag:应用包AMD64镜像构建完成并上传成功，应用包AMD镜像号为：${image_name}'"
                                sh "docker build -t ${runtime_image_name} --target runtime -f third-party/docker/Dockerfile.light . ${build_args}"
                                sh "docker push ${runtime_image_name}"
                                sh "echo '>>>>>>>>>lgtag:运行时AMD64镜像构建完成并上传成功，运行时AMD镜像号为：${runtime_image_name}'"

                            }

                            if (params.BUILD_CPU_ARCH=="AMD64_ARM64"){
                                sh "docker build --platform=linux/amd64 -t ${image_name} --no-cache -f third-party/docker/Dockerfile . ${build_args}"
                                sh "docker push ${image_name}"
                                sh "echo '>>>>>>>>>AMD64镜像构建完成并上传成功，AMD镜像号为：${image_name}'"
                                sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                sh "docker build -t ${image_name} --no-cache -f third-party/docker/Dockerfile.arm64 . ${build_args}"
                                sh "docker push ${image_name}"
                                sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_name}'"
                                sh "echo 'AMD64/ARM64镜像都构建完成！！'"
                            }

                        }
                    }
                }
                if (isDeploy) {
                    def group = 'rightcloud-v4';
                    def service_name = 'third-party';
                    def delete  = false;
                    stage('deploy service') {
                        container('kubectl') {
                            dir("/home/jenkins/workspace/kubernetes-yml") {
                                stage('clone yaml template') {
                                    git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
                                }
                                stage('run in k8s') {
                                    sh " cd ./${group}/app/${service_name}/overlays/${namespace} && kustomize edit set image ${service_name}=${image_name} && kustomize edit set image dbmigration=${db_image_name} && kustomize edit set namespace ${namespace}"
                                    //先删除
                                    if (delete) {
                                        try {
                                            sh " kustomize build ./${group}/app/${service_name}/overlays/${namespace} | ${kubectl} delete -f -"
                                        }catch(e){
                                            println(e.getMessage())
                                        }
                                    }
                                    sh " kustomize build ./${group}/app/${service_name}/overlays/${namespace} | ${kubectl} apply -f -"
                                }
                            }
                        }
                    }
                }
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
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
