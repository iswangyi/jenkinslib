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
             choice(name: "BUILD_CPU_ARCH", choices: 'AMD64\nARM64\nAMD64_ARM64', description: 'cpu架构', defaultValue: "AMD64_ARM64"),

        ])
         ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))
        // 获取Dockerfile 和 db-resource
        def branchname = "${PARAM_GIT_TAGS}"
        echo scm.userRemoteConfigs()
        checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
        sh 'rm -rf .git && rm -rf .idea'
        def runtime_image_name = "imagev2.rightcloud.com.cn/${namespace}/rightcloud-server-runtime:v.${branchname}.${env.BUILD_NUMBER}"
        def image_name = "imagev2.rightcloud.com.cn/${namespace}/rightcloud-server:v.${branchname}.${env.BUILD_NUMBER}"

         try {
            parallel(
                failFast: true,
                "build & deploy": {

                    stage("build server image") {
                        //准备Dockerfile所需的--build-arg参数

                        def build_args =   "" +
                                         " --network=host  --build-arg SERVER_PROJECT=./ "
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


                                sh "echo ${arch_tips}"
                                if (params.BUILD_CPU_ARCH=="ARM64"){
                                    //sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
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
                                    //sh "docker run --rm --privileged imagev2.rightcloud.com.cn/library/qemu-user-static:register"
                                    sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile.arm64 . ${build_args}"
                                    sh "docker push ${image_name}"
                                    sh "echo '>>>>>>>>>ARM64镜像构建完成并上传成功，ARM镜像号为：${image_name}'"
                                    sh "echo 'AMD64/ARM64镜像都构建完成！！'"
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
