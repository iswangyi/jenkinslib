import cn.com.rightcloud.jenkins.Constants

def handle_image(image, tgz_name, tag_key) {
    if (!image) {
        return
    }
    def tag = "${tag_key}=${image}"
    sh "ssh -T -o StrictHostKeyChecking=no -l root 10.68.8.30 \" cd /cloudStar/upgrade && docker pull ${image} && docker save ${image} | gzip > ${tgz_name} && echo ${tag} >> /cloudStar/upgrade/version_info \" "
}

def call() {
    node {
        properties([parameters([
           string(name: "server_image", defaultValue: "", description: "server镜像"),
           string(name: "db_migration_image", defaultValue: "", description: "db_migration_image镜像"),
           string(name: "adapter_image", defaultValue: "", description: "adapter_image镜像"),
           string(name: "gateway_image", defaultValue: "", description: "gateway_image镜像"),
           string(name: "schedule_image", defaultValue: "", description: "schedule_image镜像"),
           string(name: "console_image", defaultValue: "", description: "console_image镜像"),
           string(name: "ansible_image", defaultValue: "", description: "ansible_image镜像"),
           string(name: "monitor_server_image", defaultValue: "", description: "monitor_server_image镜像"),
           string(name: "monitor_agent_image", defaultValue: "", description: "monitor_agent_image镜像"),
           string(name: "release_version", defaultValue: "$params.release_version", description: "版本号 e.g.  3.8.0"),
           booleanParam(name: "UPLOAD_TO_BAIDU_WANGPAN", defaultValue: true, description: "是否上传百度网盘"),
           string(name: "release_description", defaultValue: "", description: "本次构建的描述信息，必须填写"),
        ])
        ])

        for (param in params) {
            if (param.key == "UPLOAD_TO_BAIDU_WANGPAN") {
                continue
            }
            if (!param.value.trim()) {
                error("the param ${param.key} is null")
            }
        }

        // 这里的namespace只用来发送钉钉消息
        def namespace = "vue-project123"
        
        def BUILD_SERVER="10.68.8.30"

        def now = new Date().format("yyyyMMddHHmm")
        def tgz_name = "rightcloud-cmp-ha-upgrade-any-to-${release_version}-${now}.tar.gz"
        def host_tgz_path = "/cloudStar/tmp/${tgz_name}"
        def baidu_path = "/product/rightcloud-cmp/ha-patch"

        try {
            stage ("save all images") {
                sshagent(['ssh-key-10.69.0.234-aio-dev']) {

                    def version = "release_version=${release_version}-${now}"
                    def PKG_NAME = "PKG_NAME=${tgz_name}"
                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" rm -rf /cloudStar/upgrade; mkdir -p /cloudStar/upgrade; echo ${version} > /cloudStar/upgrade/.version_info \" "

                    def tag
                    def count = 0
                    if (server_image) {
                        tag = "SERVER_TAG=${server_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${server_image} && docker save ${server_image} | gzip > server.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }

                    if (db_migration_image) {
                        tag = "DB_MIGRATION_TAG=${db_migration_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${db_migration_image} && docker save ${db_migration_image} | gzip > db_migration.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }

                    if (gateway_image) {
                        tag = "GATEWAY_TAG=${gateway_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${gateway_image} && docker save ${gateway_image} | gzip > gateway.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }

                    if (adapter_image) {
                        tag = "ADAPTER_TAG=${adapter_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${adapter_image} && docker save ${adapter_image} | gzip > adapter.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }
                    if (schedule_image) {
                        tag = "SCHEDULE_TAG=${schedule_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${schedule_image} && docker save ${schedule_image} | gzip > schedule.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }
                    if (console_image) {
                        tag = "CONSOLE_TAG=${console_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${console_image} && docker save ${console_image} | gzip > console.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++

                    }
                    if (ansible_image) {
                        tag = "ANSIBLE_TAG=${ansible_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${ansible_image} && docker save ${ansible_image} | gzip > ansible.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }
                    if (monitor_server_image) {
                        tag = "MONITOR_SERVER_TAG=${monitor_server_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${monitor_server_image} && docker save ${monitor_server_image} | gzip > monitor_server.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }
                    if (monitor_agent_image) {
                        tag = "MONITOR_AGENT_TAG=${monitor_agent_image}"
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd /cloudStar/upgrade && docker pull ${monitor_agent_image} && docker save ${monitor_agent_image} | gzip > monitor_agent.tgz && echo ${tag} >> /cloudStar/upgrade/.version_info \" "
                        count ++
                    }

                    if (count == 0) {
                        error("输入镜像为空，请至少输入一个镜像！")
                    }


                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"cd /cloudStar && tar zcf ${host_tgz_path} upgrade \" "
                    
                    echo "ha更新包内网获取：${BUILD_SERVER}-------${host_tgz_path}"
                    echo "ha更新包内网http：http://10.68.8.30/${tgz_name}"
                    md5_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"md5sum ${host_tgz_path}\" ").trim()
                    echo "ha更新包md5：${md5_info}"
                }
            }

            stage('upload to baidu wangpan') {
                def share_info
                BAIDU_BDUSS="G9yRHRPSFd3M1JhVFFsNUozRmlRfjFROUR2MEdFSDFuV1ZiZlQtdGpsN2poNnRlSVFBQUFBJCQAAAAAAAAAAAEAAABg0cmsenp0dDIwMTcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOP6g17j-oNeS"
                if (UPLOAD_TO_BAIDU_WANGPAN == "true") {
                    sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                        sh "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"curl --header 'PRIVATE-TOKEN: j-2w5318uPTixK6TLYQ5' http://10.68.6.20:8082/cloudstar/ infrastructure-ops/raw/master/upload-baidu/upload.sh| bash -s -- -b ${BAIDU_BDUSS} -s ${host_tgz_path} -d ${baidu_path}\" "
                        share_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"cat /tmp/share_link.txt\" ").trim()
                    }
                    //notifySuccessful("${share_info}")
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