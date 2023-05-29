import cn.com.rightcloud.jenkins.Constants

def call(kuberc_branch="release-rightcloud-standard", charts_branch="master") {
    node {
        properties([parameters([
            string(name: "FLUENTD_TAG", defaultValue: "image.rightcloud.com.cn/library/fluentd:1.6.2.12-alpine", description: "fluentd镜像"),
            string(name: "NEXUS3_TAG", defaultValue: "image.rightcloud.com.cn/rightcloud/nexus3:v202007091022", description: "nexus镜像,docker仓库和其包含的初始镜像"),
            string(name: "RDP_TAG", defaultValue: "image.rightcloud.com.cn/library/rightcloud-rdp:2.0.0", description: "rdp镜像"),
            string(name: "DB_INIT_TAG", defaultValue: "image.rightcloud.com.cn/rightcloud/dbinit:v3.0", description: "dbinit镜像"),

            string(name: "SERVER_TAG", defaultValue: "", description: "server镜像"),
            string(name: "DB_MIGRATION_TAG", defaultValue: "", description: "db_migration_image镜像"),
            string(name: "GATEWAY_TAG", defaultValue: "", description: "gateway_image镜像"),
            string(name: "ADAPTER_TAG", defaultValue: "", description: "adapter_image镜像"),
            string(name: "SCHEDULE_TAG", defaultValue: "", description: "schedule_image镜像"),
            string(name: "CONSOLE_TAG", defaultValue: "", description: "console_image镜像"),
            string(name: "ANSIBLE_TAG", defaultValue: "", description: "ansible_image镜像"),
            string(name: "MONITOR_SERVER_TAG", defaultValue: "", description: "monitor_server_image镜像"),
            string(name: "MONITOR_AGENT_TAG", defaultValue: "", description: "monitor_agent_image镜像"),    

            choice(choices: 'offline\nonline', description: 'offline or online', name: 'on_or_off'),
            choice(choices: 'amd64\narm64', description: 'cpu架构', defaultValue: "amd64", name: 'amd64_or_arm64'),
            string(name: "release_version", defaultValue: "$params.release_version", description: "版本号"),
            booleanParam(name: "UPLOAD_TO_BAIDU_WANGPAN", defaultValue: true, description: "是否上传百度网盘"),
            string(name: "release_description", defaultValue: "", description: "本次构建的描述信息，必须填写"),
        ])
        ])

        for (param in params) {
            if (param.key == "UPLOAD_TO_BAIDU_WANGPAN") {
                continue
            }
            if (!param.value) {
                error("the param ${param.key} is null，所有参数都必须填写")
            }
        }

        // 这里的namespace只用来发送钉钉消息
        def namespace = "vue-project123"
        
        def now = new Date().format("yyyyMMddHHmm")

        def all_tag_map = [
            "FLUENTD_TAG": "fluentd.tgz",
            "RDP_TAG": "rdp.tgz",
            "DB_INIT_TAG": "db_init.tgz",
            "DB_MIGRATION_TAG": "db_migration.tgz",
            "ANSIBLE_TAG": "ansible.tgz",
            "SERVER_TAG": "server.tgz",
            "ADAPTER_TAG": "adapter.tgz",
            "CONSOLE_TAG": "console.tgz",
            "SCHEDULE_TAG": "schedule.tgz",
            "MONITOR_SERVER_TAG": "monitor_server.tgz",
            "MONITOR_AGENT_TAG": "monitor_agent.tgz",
            "GATEWAY_TAG": "gateway.tgz",
            "NEXUS3_TAG": "nexus.tgz",
        ]

        try {
            
            stage('git clone kuberc and charts') {
                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kuberc.git', branch: "${kuberc_branch}"
                sh "mkdir -p /home/package/kuberc"
                sh "cp -r * /home/package/kuberc/"
                sh "rm -rf /home/package/kuberc/.git"

                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/charts.git', branch: "${charts_branch}"
                sh "sed -i \"s|appVersion: .*|appVersion: v${release_version}|g\" cmp/Chart.yaml"
                sh "tar zcvf cmp.tgz cmp && mv cmp.tgz /home/package/kuberc/roles/cmp-app/files/"
            }

            stage("Package every image") {
                container('docker') {
                    sh "echo RELEASE_VERSION=${release_version} > /home/package/kuberc/.version_info"
                    all_tag_map.each {key, value -> 
                        if (params.on_or_off == "offline") {
                            sh "docker pull ${params[key]}"
                            if (key == "NEXUS3_TAG") {
                                sh "docker save ${params[key]} |gzip > /home/package/kuberc/roles/nexus3/files/${value}"
                            }else {
                                sh "docker save ${params[key]} |gzip > /home/package/kuberc/roles/cmp-app/files/${value}"
                            }
                        }
                        sh "echo ${key}=${params[key]} >>  /home/package/kuberc/.version_info"
                        // 替换所有hosts中的镜像为 jenkins参数获取的值
                        if (key == "NEXUS3_TAG") {
                            sh "sed -i \"s|nexus_image_tag:.*|nexus_image_tag: ${params[key]}|g\" /home/package/kuberc/roles/nexus3/defaults/main.yml"
                        }else {
                            sh "sed -i 's|${key}=.*|'${key}=${params[key]}'|g' /home/package/kuberc/hosts"
                        }
                    }
                }
            }

            stage("package and upload to baiduwangpan") {

                def pkg_tgz_name = "cmp-hak8s-installer-${release_version}-${now}.tar.gz"
                def host_tgz_path = "/home/package/${pkg_tgz_name}"
                def baidu_path = "/Release-transfer-standby/rightcloud-cmp/ha-k8s"

                sh "cd /home/package/ && mv kuberc ansible && tar zcvf ${pkg_tgz_name} ansible"

                def innerStorageServer = '10.69.5.30'
                def innerStorageServerPath = "/data/public/ha-k8s-install"
                def innerStorageServerTgzPath = "${innerStorageServerPath}/${pkg_tgz_name}"
                BAIDU_BDUSS="G9yRHRPSFd3M1JhVFFsNUozRmlRfjFROUR2MEdFSDFuV1ZiZlQtdGpsN2poNnRlSVFBQUFBJCQAAAAAAAAAAAEAAABg0cmsenp0dDIwMTcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOP6g17j-oNeS"
                sshagent(credentials: ['global-ssh-key-czc']) {
                    sh "ssh -o StrictHostKeyChecking=no -l root ${innerStorageServer} uname -a"
                    sh "scp -r ${host_tgz_path} root@${innerStorageServer}:${innerStorageServerPath}"

                    echo "ha-k8s安装包内网下载：http://${innerStorageServer}/ha-k8s-install/${pkg_tgz_name}"
                    def md5_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${innerStorageServer} \"md5sum ${innerStorageServerTgzPath}\" ").trim()
                    echo "--------------md5：${md5_info}"

                    if (UPLOAD_TO_BAIDU_WANGPAN == "true") {
                        sh "ssh -o StrictHostKeyChecking=no -l root ${innerStorageServer} \" cd /upload/ && curl --header 'PRIVATE-TOKEN: j-2w5318uPTixK6TLYQ5' http://10.68.6.20:8082/cloudstar/infrastructure-ops/raw/master/upload-baidu/upload.sh| bash -s -- -b ${BAIDU_BDUSS} -s ${innerStorageServerTgzPath} -d ${baidu_path} \""
                        share_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${innerStorageServer} \"cat /tmp/share_link.txt\"").trim()
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
