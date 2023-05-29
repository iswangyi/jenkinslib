import cn.com.rightcloud.jenkins.Constants

def call(namespace="yuncong1-project", k8sEnv="oem") {
    def kubectl = getKubectl(k8sEnv)

    node {
        properties([parameters([
           string(name: "SERVER_TAG", defaultValue: "", description: "server镜像"),
           string(name: "DBUPGRADE_TAG", defaultValue: "", description: "db_migration_image镜像"),
           string(name: "GATEWAY_TAG", defaultValue: "", description: "gateway_image镜像"),
           string(name: "ADAPTER_TAG", defaultValue: "", description: "adapter_image镜像"),
           string(name: "SCHEDULE_TAG", defaultValue: "", description: "schedule_image镜像"),
           string(name: "CONSOLE_TAG", defaultValue: "", description: "console_image镜像"),
           string(name: "ANSIBLE_TAG", defaultValue: "", description: "ansible_image镜像"),
           string(name: "release_description", defaultValue: "", description: "本次更新的描述信息"),
        ])
        ])

        try {
            container('kubectl') {
                dir("kubernetes-yml") {
                    stage('clone yaml template') {
                        git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
                    }
                    def templateFile
                    stage('run in k8s') {
                        // adapter
                        if (ADAPTER_TAG) {
                            templateFile = "rightcloud/app/rightcloud-adapter-template.yaml"
                            sh " sed -i -e  's|\$adapterTag|'${ADAPTER_TAG}'|g' -e 's|\$namespace|'${namespace}'|g' ${templateFile}"
                            sh "${kubectl} apply -f ${templateFile}"
                        }
                        // server
                        if (SERVER_TAG && DBUPGRADE_TAG) {
                            templateFile = "rightcloud/app/rightcloud-server-template-4c4g.yaml"
                            sh " sed -i 's|\$serverTag|'${SERVER_TAG}'|g'   ${templateFile}"
                            sh " sed -i 's|\$dbmigrationTag|'${DBUPGRADE_TAG}'|g'   ${templateFile}"
                            sh " sed -i 's|\$namespace|'${namespace}'|g' ${templateFile}"
                            sh "${kubectl} apply -f ${templateFile}"
                        }
                        // schedule
                        if (SCHEDULE_TAG) {
                            templateFile = "rightcloud/app/rightcloud-schedule-template.yaml"
                            sh " sed -i -e  's|\$scheduleTag|'${SCHEDULE_TAG}'|g' -e 's|\$namespace|'${namespace}'|g' ${templateFile}"
                            sh "${kubectl} apply -f ${templateFile}"
                        }
                        // gateway
                        if (GATEWAY_TAG) {
                          templateFile = "rightcloud/app/rightcloud-gateway-zhuxian-template.yaml"
                          sh " sed -i -e  's|\$gatewayTag|'${GATEWAY_TAG}'|g' -e 's|\$namespace|'${namespace}'|g' ${templateFile}"
                          sh "${kubectl} apply -f ${templateFile}"
                        }
                        // ansible
                        if (ANSIBLE_TAG) {
                            templateFile = "rightcloud/app/rightcloud-ansible-template.yaml"
                            sh " sed -i -e  's|\$ansibleTag|'${ANSIBLE_TAG}'|g' -e 's|\$namespace|'${namespace}'|g' ${templateFile}"
                            sh "${kubectl} apply -f ${templateFile}"
                        }
                        // console
                        if (CONSOLE_TAG) {
                            templateFile = "rightcloud/app/rightcloud-console-template.yaml"
                            sh " sed -i -e  's|\$consoleTag|'${CONSOLE_TAG}'|g' -e 's|\$namespace|'${namespace}'|g' ${templateFile}"
                            sh "${kubectl} apply -f ${templateFile}"
                        }
                        
                    }
                }
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
        }finally{
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}