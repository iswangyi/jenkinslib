import cn.com.rightcloud.jenkins.Constants

// groovy的数组截取是左开右开
def filterGroup(repo) {
    return repo ? repo.split('/')[0..-2].join('/') : "error"
}

def filterProject(repo) {
    return repo ? repo.split('/')[-1] : "error"
}


def call(isDeploy=false) {
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
                choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'), string(name: "VERSION_PREFIX", defaultValue: "$params.VERSION_PREFIX", description: "版本号前缀"),
                choice(name: "BUILD_CPU_ARCH", choices: 'ARM64\nAMD64\nBOTH_AMD64_ARM64', description: 'cpu架构', defaultValue: "ARM64"),
                choice(choices: 'kunpeng\nfeiteng', description: '芯片类型', name: 'arch_name'),
        ])
        ])
        def scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
        def prject_group_scm_url = scmUrl.split('/')[0..-2].join('/')
        def parent_scm_url = prject_group_scm_url + '/rightcloud-parent.git'

        def namespace = env.NAMESPACE_INJECT
        // def kubectl = getKubectl(getK8sEnv(namespace))
        // 获取Dockerfile 和 db-resource
        // def branchname = "${PARAM_GIT_TAGS}"
        // checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
        // sh 'rm -rf .git && rm -rf .idea'
        // def dockerfilename="Dockerfile.light.jenkins"
        // // def dockerfilename="Dockerfile.light.arm"
        // def arch_bz = "amd64"
        // if (BUILD_CPU_ARCH == "ARM64") {
        //   // 统一用 feiteng版即jdk版非jre
        //   dockerfilename="Dockerfile.light.arm"
        //   arch_bz = "arm64"
        // }

        // def image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-server-main:v.${branchname}.${arch_bz}.${env.BUILD_NUMBER}"
        // def image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-server-main:v.${branchname}.${arch_bz}.${env.BUILD_NUMBER}"
        // def image_runtime = "image.rightcloud.com.cn/${namespace}/rightcloud-server-runtime:v.${branchname}.${arch_bz}.${env.BUILD_NUMBER}"
        // def image_runtime_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-server-runtime:v.${branchname}.${arch_bz}.${env.BUILD_NUMBER}"



                    def branchname = "${PARAM_GIT_TAGS}"
                    def amd64_image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-server-main:v.${branchname}.amd64.${env.BUILD_NUMBER}"
                    def amd64_image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-server-main:v.${branchname}.amd64.${env.BUILD_NUMBER}"
                    def amd64_image_runtime = "image.rightcloud.com.cn/${namespace}/rightcloud-server-runtime:v.${branchname}.amd64.${env.BUILD_NUMBER}"
                    def amd64_image_runtime_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-server-runtime:v.${branchname}.amd64.${env.BUILD_NUMBER}"
                    
                    def arm64_image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-server-main:v.${branchname}.arm64.${env.BUILD_NUMBER}"
                    def arm64_image_name_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-server-main:v.${branchname}.arm64.${env.BUILD_NUMBER}"
                    def arm64_image_runtime = "image.rightcloud.com.cn/${namespace}/rightcloud-server-runtime:v.${branchname}.arm64.${env.BUILD_NUMBER}"
                    def arm64_image_runtime_huaweiyun = "swr.cn-east-3.myhuaweicloud.com/cmp/${namespace}/rightcloud-server-runtime:v.${branchname}.arm64.${env.BUILD_NUMBER}"


        try {
            parallel(
                    failFast: true,
                    "build & deploy": {

                        stage("build server image") {

                        stage("clone parent") {
                            dir("/home/jenkins/workspace/parent/") {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "${parent_scm_url}", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }
                        }
                        stage("clone current app component") {
                            dir("/home/jenkins/workspace/app_component/") {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                                sh 'ls -al'
                                sh 'pwd'
                            }
                        }

                            //准备Dockerfile所需的--build-arg参数

                            def  build_args = " --network=host --build-arg   SERVER_PROJECT=./app_component --build-arg PARENT_PROJECT=./parent  --build-arg COMPILER_IMAGE=swr.cn-east-3.myhuaweicloud.com/distroless/rightcloud-lib:4.2-jenkins "
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
                                dir("/home/jenkins/workspace/") {
                                  // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {


                                    sh "echo ${arch_tips}"
                                    // if (params.BUILD_CPU_ARCH=="ARM64"){
                                    //     sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                    //     sh "docker build -t ${image_name} --no-cache --target mainjar -f app_component/docker/${dockerfilename} . ${build_args}"
                                    //     sh "docker push ${image_name}"
                                    //     sh "docker build -t ${image_runtime} --no-cache --target runtime -f app_component/docker/${dockerfilename} . ${build_args}"
                                    //     sh "docker push ${image_runtime}"
                                    //     sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_name}'"
                                    //     sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_runtime}'"

                                    //     sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    //     sh "docker tag ${image_name} ${image_name_huaweiyun}"
                                    //     sh "docker push ${image_name_huaweiyun}"
                                    //     sh "docker tag ${image_runtime} ${image_runtime_huaweiyun}"
                                    //     sh "docker push ${image_runtime_huaweiyun}"
                                    //     sh "echo '---------镜像外网拉取arm-------tag：${image_name_huaweiyun}'"
                                    //     sh "echo '---------镜像外网拉取arm-------tag：${image_runtime_huaweiyun}'"
                                    // }






                                  def build_arm64_image = {
                                      sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                      sh "docker build -t ${arm64_image_name} --no-cache --target mainjar -f app_component/docker/Dockerfile.light.arm . ${build_args}"
                                      sh "docker push ${arm64_image_name}"
                                      sh "docker build -t ${arm64_image_runtime} --no-cache --target runtime -f app_component/docker/Dockerfile.light.arm . ${build_args}"
                                      sh "docker push ${arm64_image_runtime}"
                                      sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${arm64_image_name}'"
                                      sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${arm64_image_runtime}'"
                                      
                                    sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                    sh "docker tag ${arm64_image_name} ${arm64_image_name_huaweiyun}"
                                    sh "docker push ${arm64_image_name_huaweiyun}"
                                    sh "docker tag ${arm64_image_runtime} ${arm64_image_runtime_huaweiyun}"
                                    sh "docker push ${arm64_image_runtime_huaweiyun}"
                                    sh "echo '---------镜像外网拉取arm-------tag：${arm64_image_name_huaweiyun}'"
                                    sh "echo '---------镜像外网拉取arm-------tag：${arm64_image_runtime_huaweiyun}'"
                                  }

                                  def build_amd64_image = {
                                    //   sh "docker run --rm --privileged swr.cn-east-3.myhuaweicloud.com/distroless/qemu-user-static:register || :"
                                      sh "docker build -t ${amd64_image_name} --no-cache --target mainjar -f app_component/docker/Dockerfile.light.jenkins . ${build_args}"
                                      sh "docker push ${amd64_image_name}"
                                      sh "docker build -t ${amd64_image_runtime} --no-cache --target runtime -f app_component/docker/Dockerfile.light.jenkins . ${build_args}"
                                      sh "docker push ${amd64_image_runtime}"
                                      sh "echo '>>>>>>>>>X86镜像构建完成并上传成功，X86镜像号为：${amd64_image_name}'"
                                      sh "echo '>>>>>>>>>X86镜像构建完成并上传成功，X86镜像号为：${amd64_image_runtime}'"

                                      sh "docker login -u cn-east-3@1O7QMGSILSLHET7KI8HB -p 47e997ac5e65555a875e9d145ac6bacca639504a49d88f2f90d85b480d7c3d53 swr.cn-east-3.myhuaweicloud.com"
                                      sh "docker tag ${amd64_image_name} ${amd64_image_name_huaweiyun}"
                                      sh "docker push ${amd64_image_name_huaweiyun}"
                                      sh "docker tag ${amd64_image_runtime} ${amd64_image_runtime_huaweiyun}"
                                      sh "docker push ${amd64_image_runtime_huaweiyun}"
                                      sh "echo '---------镜像外网拉取amd64-------tag：${amd64_image_name_huaweiyun}'"
                                      sh "echo '---------镜像外网拉取amd64-------tag：${amd64_image_runtime_huaweiyun}'"
                                  }

                                  if (params.BUILD_CPU_ARCH == "ARM64"){
                                    build_arm64_image.call()

                                  }

                                  if (params.BUILD_CPU_ARCH == "AMD64"){
                                    build_amd64_image.call()

                                  }

                                  if (params.BUILD_CPU_ARCH == "BOTH_AMD64_ARM64"){
                                    build_arm64_image.call()
                                    build_amd64_image.call()
                                  }






                                  }
                                }
                            }
                        }

                        if (isDeploy) {
                            container('kubectl') {
//                            echo "updating version information"
//                                def versioninfo=params.VERSION_PREFIX.trim()
//                                def updateversionstdout = sh (
//                                        script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); mysqluser=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(${kubectl} -n ${namespace} get pod |grep rightcloud-mysql | grep Running | awk '{print \$1}'); ${kubectl} -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h127.0.0.1 -u\${mysqluser} -p\${mysqlpwd} rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
//                                        returnStdout: true
//                                ).trim();
//                            echo "update version stdout: ${updateversionstdout}"

                                dir("kubernetes-yml") {
                                    stageK8ServiceDeploy("server", namespace, kubectl, image_name, false)
                                }
                            }
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
