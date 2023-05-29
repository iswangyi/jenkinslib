import cn.com.rightcloud.jenkins.Constants


// 这个版本所有打包流程放到jenkins中完成，不缓存离线包
def call(kuberc_aio_branch="master", charts_branch="master") {
    node {
        properties([parameters([
            //string(name: "release_version", defaultValue: "$params.release_version", description: "发布版本号"),
            string(name: "INFLUXDB_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/influxdb:1.7.6-alpine", description: "influxdb镜像"),
            string(name: "REDIS_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/redis:latest", description: "redis镜像"),
            string(name: "MONGODB_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/mongodb:3.7", description: "mongodb镜像"),
            string(name: "MYSQL_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/mysql:5.7.20", description: "mysql镜像"),
            string(name: "RABBITMQ_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/rabbitmq:3.7.16-management-alpine-with-cer", description: "rabbitmq镜像"),
            string(name: "DBINIT_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/dbinit:v3.0", description: "dbinit镜像"),
            string(name: "DBCONFIG_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/dbconfig:v3.0", description: "dbconfig镜像"),
            string(name: "NODE_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/kindest/node:v1.18.2", description: "kind node 镜像"),
 
            string(name: "FLUENTD_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/fluentd:1.1-alpine", description: "fluentd镜像"),
            string(name: "RDP_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/cmp-rdp:0.9.14", description: "rdp镜像"),
 
            string(name: "SERVER_TAG", defaultValue: "", description: "server镜像"),
            string(name: "DBUPGRADE_TAG", defaultValue: "", description: "db_migration_image镜像"),
            string(name: "GATEWAY_TAG", defaultValue: "", description: "gateway_image镜像"),
            string(name: "ADAPTER_TAG", defaultValue: "", description: "adapter_image镜像"),
            string(name: "SCHEDULE_TAG", defaultValue: "", description: "schedule_image镜像"),
            string(name: "CONSOLE_TAG", defaultValue: "", description: "console_image镜像"),
            string(name: "ANSIBLE_TAG", defaultValue: "", description: "ansible_image镜像"),
            string(name: "MONITORSERVER_TAG", defaultValue: "", description: "monitor_server_image镜像"),
            string(name: "MONITORAGENT_TAG", defaultValue: "", description: "monitor_agent_image镜像"),
 
            choice(choices: 'offline\nonline', description: 'offline or online', name: 'on_or_off'),
            choice(choices: 'amd64\narm64', description: 'cpu架构', defaultValue: "amd64", name: 'amd64_or_arm64'),
            string(name: "release_version", defaultValue: "$params.release_version", description: "版本号"),
            booleanParam(name: "UPLOAD_TO_BAIDU_WANGPAN", defaultValue: true, description: "是否上传百度网盘"),
            string(name: "release_description", defaultValue: "", description: "本次构建的描述信息，必须填写"),
        ])
        ])

        for (param in params) {
            //if (!param.value.trim()) {
            if (param.key == "UPLOAD_TO_BAIDU_WANGPAN") {
                continue
            }
            if (!param.value) {
                error("the param ${param.key} is null，所有参数都必须填写")
            }
        }

        // 整体设计，更新和安装包离线镜像不一样，更新时，可以只打包有填写的镜像

        // 这里的namespace只用来发送钉钉消息
        def namespace = "vue-project123"

        def now = new Date().format("yyyyMMddHHmm")

        // all_env_tag中的tag和kuberc-aio仓库中的bin/.env中的tag保持一致
        def all_env_tag = ["INFLUXDB_TAG", "REDIS_TAG", "RDP_TAG", "MONGODB_TAG", "MYSQL_TAG", "RABBITMQ_TAG", "DBINIT_TAG", "DBCONFIG_TAG", "FLUENTD_TAG", "ANSIBLE_TAG", "SERVER_TAG", "DBUPGRADE_TAG", "ADAPTER_TAG", "CONSOLE_TAG", "SCHEDULE_TAG", "MONITORSERVER_TAG", "MONITORAGENT_TAG", "GATEWAY_TAG", "NODE_TAG"]
        // all_values_tag_map的key主要取自values.yaml文件中每一个镜像的完整镜像名中提取标志性部分，用于sed时找到那一行
        def all_values_tag_map = [
            "DBUPGRADE_TAG": "migration",
            "SERVER_TAG": "server",
            "SCHEDULE_TAG": "schedule",
            "ADAPTER_TAG": "adapter",
            "CONSOLE_TAG": "console",
            "ANSIBLE_TAG": "ansible",
            "MONITORSERVER_TAG": "monitor/server",
            "MONITORAGENT_TAG": "monitor/agent",
            "GATEWAY_TAG": "gateway",
            "FLUENTD_TAG": "fluentd",
            "RDP_TAG": "rdp",
        ]
        
        try {
            
            stage('git clone kuberc-aio and charts') {
                sh "mkdir -p /home/package/cmp/images"
                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kuberc-aio.git', branch: "${kuberc_aio_branch}"
                sh "cp -r * /home/package/cmp/"
                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/charts.git', branch: "${charts_branch}"
                sh "cp -r cmp /home/package/cmp/"
            }
            stage("update the latest mirror tag") {
                sh "rm -rf /home/package/cmp/.git"
                sh "mv /home/package/cmp/cmp/values-aio.yaml /home/package/cmp/cmp/values.yaml"

                // 替换chart version
                sh "sed -i \"s|appVersion: .*|appVersion: v${release_version}|g\" /home/package/cmp/cmp/Chart.yaml"
                // 替换.env文件中的镜像为 jenkins参数获取的值
                for (tag in all_env_tag) {
                    sh "sed -i 's|${tag}=.*|'${tag}=${params[tag]}'|g' /home/package/cmp/bin/.env"
                }
                // 替换values文件中的镜像
                def tmp_sed
                all_values_tag_map.each {key, value ->
                    if (key == "SERVER_TAG") {
                        tmp_sed = sh(returnStdout: true, script: "cat /home/package/cmp/cmp/values.yaml |grep -A 1 ${value}|grep repository|grep -v monitor").trim()
                        sh "sed -i 's|${tmp_sed}|repository: ${params[key]}|g' /home/package/cmp/cmp/values.yaml"
                    }else {
                        tmp_sed = sh(returnStdout: true, script: "cat /home/package/cmp/cmp/values.yaml |grep -A 1 ${value}|grep repository").trim()
                        sh "sed -i 's|${tmp_sed}|repository: ${params[key]}|g' /home/package/cmp/cmp/values.yaml"
                    }
                }

                sh "cd /home/package/cmp/ && tar zcvf cmp.tgz cmp"

            }

            def tar_name
            stage("Package every image") {
                if (params.on_or_off == "offline") {
                    container('docker') {
                        for (tag in all_env_tag) {
                            sh "docker pull ${params[tag]}"
                            tar_name = params[tag].trim().split('/')[-1] + ".tar"
                            sh "docker save ${params[tag]} > /home/package/cmp/images/${tar_name}"
                        }
                    }
                }
            }

            stage("package and upload to baiduwangpan") {

                def pkg_tgz_name = "cmp-aio-installer-${release_version}-${now}.tar.gz"
                def host_tgz_path = "/home/package/${pkg_tgz_name}"
                def baidu_path = "/Release-transfer-standby/rightcloud-cmp/aio-k8s"

                sh "cd /home/package/ && tar zcvf ${pkg_tgz_name} cmp"

                def innerStorageServer = '10.69.5.30'
                def innerStorageServerPath = "/data/public/aio-k8s-install"
                def innerStorageServerTgzPath = "${innerStorageServerPath}/${pkg_tgz_name}"
                BAIDU_BDUSS="G9yRHRPSFd3M1JhVFFsNUozRmlRfjFROUR2MEdFSDFuV1ZiZlQtdGpsN2poNnRlSVFBQUFBJCQAAAAAAAAAAAEAAABg0cmsenp0dDIwMTcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOP6g17j-oNeS"
                sshagent(credentials: ['global-ssh-key-czc']) {
                    sh "ssh -o StrictHostKeyChecking=no -l root ${innerStorageServer} uname -a"
                    sh "scp -r ${host_tgz_path} root@${innerStorageServer}:${innerStorageServerPath}"

                    echo "aio-k8s安装包内网下载：http://${innerStorageServer}/aio-k8s-install/${pkg_tgz_name}"
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
