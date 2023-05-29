import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            //string(name: "release_version", defaultValue: "$params.release_version", description: "发布版本号"),
            string(name: "server_image", defaultValue: "", description: "server镜像"),
            string(name: "db_migration_image", defaultValue: "", description: "db_migration_image镜像"),
            string(name: "gateway_image", defaultValue: "", description: "gateway_image镜像"),
            string(name: "adapter_image", defaultValue: "", description: "adapter_image镜像"),
            string(name: "schedule_image", defaultValue: "", description: "schedule_image镜像"),
            string(name: "console_image", defaultValue: "", description: "console_image镜像"),
            string(name: "ansible_image", defaultValue: "", description: "ansible_image镜像"),
            string(name: "monitor_server_image", defaultValue: "", description: "monitor_server_image镜像"),
            string(name: "monitor_agent_image", defaultValue: "", description: "monitor_agent_image镜像"),
            string(name: "release_version", defaultValue: "$params.release_version", description: "版本号"),
            choice(choices: 'offline\nonline', description: 'offline or online', name: 'ON_OR_OFF'),
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
        def host_dir_path = "/cloudStar/cmp-oem-396"
        def deploy_dir="${host_dir_path}/ha-deployment"
        def host_file="${deploy_dir}/host"

        def now = new Date().format("yyyyMMddHHmm")
        def tgz_name = "cmp-ha-installer-${release_version}-${now}.tar.gz"
        def host_tgz_path = "/cloudStar/tmp/${tgz_name}"
        def baidu_path = "/Release-transfer-standby/rightcloud-cmp-oem/ha"
        
        try {
            stage ("handle images and package") {
                sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                    def version = "release_version=${release_version}-${now}"

                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd ${deploy_dir} && git reset --hard && git pull \" "

                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" echo '' > ${deploy_dir}/.version_info && echo ${version} > ${deploy_dir}/.version_info \" "

                    if (server_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|^SERVER_TAG=.*|'SERVER_TAG=${params.server_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (ansible_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|ANSIBLE_TAG=.*|'ANSIBLE_TAG=${params.ansible_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (adapter_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|ADAPTER_TAG=.*|'ADAPTER_TAG=${params.adapter_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (gateway_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|GATEWAY_TAG=.*|'GATEWAY_TAG=${params.gateway_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (console_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|CONSOLE_TAG=.*|'CONSOLE_TAG=${params.console_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (schedule_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|SCHEDULE_TAG=.*|'SCHEDULE_TAG=${params.schedule_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (monitor_server_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|MONITOR_SERVER_TAG=.*|'MONITOR_SERVER_TAG=${params.monitor_server_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (monitor_agent_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|MONITOR_AGENT_TAG=.*|'MONITOR_AGENT_TAG=${params.monitor_agent_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }
                    if (db_migration_image.trim()) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|DB_MIGRATION_TAG=.*|'DB_MIGRATION_TAG=${params.db_migration_image.replace('rightcloud','cmp')}'|g' ${host_file} \" "
                    }


                    if (ON_OR_OFF.trim() == "online") {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|PULL_CMP_IMAGE=.*|'PULL_CMP_IMAGE=true'|g' ${host_file} \" "
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" tar zcPf ${host_tgz_path} ${deploy_dir} --exclude=${deploy_dir}/.git \" "
                    }else if (ON_OR_OFF.trim() == "offline") {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" sed -i 's|PULL_CMP_IMAGE=.*|'PULL_CMP_IMAGE=false'|g' ${host_file} \" "
                        sh "ssh -T -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \" cd ${deploy_dir} && ./download_images.sh && cd ${host_dir_path} && tar zcf ${host_tgz_path} ha-deployment --exclude=ha-deployment/.git \" "
                    }else {
                        error("invalid param ON_OR_OFF")
                    }

                    echo "ha安装包内网获取：${BUILD_SERVER}-------${host_tgz_path}"
                    echo "ha安装包内网http：http://10.68.8.30/${tgz_name}"
                    md5_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"md5sum ${host_tgz_path}\" ").trim()
                    echo "ha安装包md5：${md5_info}"
                }
            }

            stage('upload to baidu wangpan') {
                def share_info
                BAIDU_BDUSS="G9yRHRPSFd3M1JhVFFsNUozRmlRfjFROUR2MEdFSDFuV1ZiZlQtdGpsN2poNnRlSVFBQUFBJCQAAAAAAAAAAAEAAABg0cmsenp0dDIwMTcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOP6g17j-oNeS"
                if (UPLOAD_TO_BAIDU_WANGPAN == "true") {
                    sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                        sh "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"curl --header 'PRIVATE-TOKEN: j-2w5318uPTixK6TLYQ5' http://10.68.6.20:8082/cloudstar/infrastructure-ops/raw/master/upload-baidu/upload.sh| bash -s -- -b ${BAIDU_BDUSS} -s ${host_tgz_path} -d ${baidu_path}\" "
                        share_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} \"cat /tmp/share_link.txt\" ").trim()
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