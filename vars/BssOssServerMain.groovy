import cn.com.rightcloud.jenkins.Constants


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
                string(name: "VERSION_PREFIX", defaultValue: "$params.VERSION_PREFIX", description: "版本号前缀"),
                choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        // 获取Dockerfile 和 db-resource
        def branchname = "${PARAM_GIT_TAGS}"
        dir('/home/jenkins/workspace/rightcloud-oss') {
            checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
            sh 'rm -rf .git && rm -rf .idea'
        }

        def db_image_name = "image.rightcloud.com.cn/${namespace}/oss-db-migration:v.${branchname}.${env.BUILD_NUMBER}"
        def image_name = "image.rightcloud.com.cn/${namespace}/oss-server:v.${branchname}.${env.BUILD_NUMBER}"
        //准备Dockerfile所需的--build-arg参数
        def  build_args = " --network=host --build-arg   OSS_PROJECT=./rightcloud-oss " +
                "--build-arg COMMON_PROJECT=./rightcloud-common "

        try {
            stage("build & validate dbmigration & sonar scan") {

                stage("build dbmigration image") {
                    container('docker') {
                        dir('/home/jenkins/workspace') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                                sh "docker build -t ${db_image_name} --no-cache -f rightcloud-oss/docker/Dockerfile.db --no-cache . ${build_args}"
                                sh "docker push ${db_image_name}"
                            }
                        }
                    }
                }
                if (params.QUALITY_ANALYSIS == "true") {
                    stageCodeQualityAnalysis()
                }
            }
            stage("build & deploy") {
                stage("build server image") {
                    stage('clone common') {
                        dir('/home/jenkins/workspace/rightcloud-common') {
                            checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/cloudstar/cloud-boss/rightcloud-common.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                            sh 'rm -rf .git && rm -rf .idea'
                        }
                    }
                    container('docker') {
                        dir('/home/jenkins/workspace') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                                sh "docker build -t ${image_name} --no-cache -f rightcloud-oss/docker/Dockerfile --no-cache . ${build_args}"
                                sh "docker push ${image_name}"
                            }
                        }
                    }
                }
                if (isDeploy && params.IS_DEPLOY == "true") {
//                    container('kubectl') {
//                        echo "updating version information"
//                        def versioninfo = params.VERSION_PREFIX.trim()
//                        def updateversionstdout
//                        if (namespace.trim() == "bss" && getK8sEnv(namespace).trim() == "dev") {
//                            updateversionstdout = sh(
//                                    script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); mysqluser=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(${kubectl} -n ${namespace} get pod |grep pxc-cluster-pxc-0 | grep Running | awk '{print \$1}'); ${kubectl} -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h10.67.7.2 -P 31001 -u\${mysqluser} -p\${mysqlpwd}     rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
//                                    returnStdout: true
//                            ).trim();
//                        } else {
//                            updateversionstdout = sh(
//                                    script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); mysqluser=\$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(${kubectl} -n ${namespace} get pod |grep rightcloud-mysql | grep Running | awk '{print \$1}'); ${kubectl} -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h127.0.0.1 -u\${mysqluser} -p\${mysqlpwd}     rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
//                                    returnStdout: true
//                            ).trim();
//                        }
//                        echo "update version stdout: ${updateversionstdout}"
//                    }
                    def group = 'bss';
                    def service_name = 'oss';
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
