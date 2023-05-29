import cn.com.rightcloud.jenkins.Constants

def call(charts_branch="master") {
    node {
        properties([parameters([
                string(name: "FLUENTD_TAG", defaultValue: "", description: "fluentd镜像"),
                string(name: "RDP_TAG", defaultValue: "", description: "rdp镜像"),

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
                string(name: "pre_version", defaultValue: "any", description: "更新前版本号，默认any"),
                string(name: "release_version", defaultValue: "$params.release_version", description: "版本号"),
                booleanParam(name: "UPLOAD_TO_BAIDU_WANGPAN", defaultValue: true, description: "是否上传百度网盘"),
                string(name: "release_description", defaultValue: "", description: "本次构建的描述信息，必须填写"),
        ])
        ])

        // for (param in params) {
        //     if (!param.value.trim()) {
        //         error("the param ${param.key} is null，所有参数都必须填写")
        //     }
        // }

        // 这里的namespace只用来发送钉钉消息
        def namespace = "vue-project123"

        def now = new Date().format("yyyyMMddHHmm")

        def all_env_tag = ["RDP_TAG", "FLUENTD_TAG", "ANSIBLE_TAG", "SERVER_TAG", "DBUPGRADE_TAG", "ADAPTER_TAG", "CONSOLE_TAG", "SCHEDULE_TAG", "MONITORSERVER_TAG", "MONITORAGENT_TAG", "GATEWAY_TAG"]

        try {

            stage('git clone charts') {
                sh "mkdir -p /home/package/cmp/bin /home/package/cmp/images"

                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/charts.git', branch: "${charts_branch}"

                sh "mv cmp /home/package/cmp/"

                sh "mv /home/package/cmp/cmp/values-aio.yaml /home/package/cmp/cmp/values.yaml"

                sh "sed -i \"s|appVersion: .*|appVersion: v${release_version}|g\" /home/package/cmp/cmp/Chart.yaml"

                sh "cd /home/package/cmp && tar zcf cmp.tgz cmp && mv cmp.tgz /home/package/cmp/ && rm -rf ./cmp"
            }

            stage("Package image") {
                container('docker') {
                    sh "echo 'WORKDIR=/usr/local/cmp' > /home/package/cmp/bin/.update"
                    sh "echo RELEASE_VERSION=${release_version} > /home/package/cmp/bin/.update"
                    def builds = [:]
                    all_env_tag.each {
                        def tag = it;
                        tag_name = params[tag].trim()
                        if (tag_name != "" && tag_name != null) {
                            builds[tag] = {
                                tag_name = params[tag].trim()
                                if (params.on_or_off == "offline") {
                                    sh "docker pull ${params[tag]}"
                                    tar_name = params[tag].trim().split('/')[-1] + ".tar"
                                    sh "docker save ${params[tag]} > /home/package/cmp/images/${tar_name}"
                                }
                                sh "echo ${tag}=${params[tag]} >>  /home/package/cmp/bin/.update"
                            }
                        }
                    }
                    parallel builds
                }
            }

            stage("package and upload to baiduwangpan") {

                def pkg_tgz_name = "cmp-aiok8s-upgrade-${pre_version}-to-${release_version}-${now}.tar.gz"
                def host_tgz_path = "/home/package/${pkg_tgz_name}"
                def baidu_path = "/Release-transfer-standby/rightcloud-cmp/aio-k8s-patch"

                sh "cd /home/package/ && tar zcvf ${pkg_tgz_name} cmp"

                def innerStorageServer = '10.69.5.30'
                def innerStorageServerPath = "/data/public/aio-k8s-patch"
                def innerStorageServerTgzPath = "${innerStorageServerPath}/${pkg_tgz_name}"
                BAIDU_BDUSS="G9yRHRPSFd3M1JhVFFsNUozRmlRfjFROUR2MEdFSDFuV1ZiZlQtdGpsN2poNnRlSVFBQUFBJCQAAAAAAAAAAAEAAABg0cmsenp0dDIwMTcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOP6g17j-oNeS"
                sshagent(credentials: ['global-ssh-key-czc']) {
                    sh "ssh -o StrictHostKeyChecking=no -l root ${innerStorageServer} uname -a"
                    sh "scp -r ${host_tgz_path} root@${innerStorageServer}:${innerStorageServerPath}"

                    echo "aio-k8s更新包内网下载：http://${innerStorageServer}/aio-k8s-patch/${pkg_tgz_name}"
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
