import cn.com.rightcloud.jenkins.Constants


// 这个版本需要一台打包的服务器，打包流程主要在打包服务器上完成，可以缓存离线镜像包
def call(pkg_host_dir="/data/pkg/aio-k8s-install", baidu_path="/Release-transfer-standby/rightcloud-cmp/aio-k8s") {
    node {
        properties([parameters([
            //string(name: "release_version", defaultValue: "$params.release_version", description: "发布版本号"),
            string(name: "INFLUXDB_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/influxdb:1.7.6-alpine", description: "influxdb镜像"),
            string(name: "REDIS_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/redis:latest", description: "redis镜像"),
            string(name: "MONGODB_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/mongodb:3.7", description: "mongodb镜像"),
            string(name: "MYSQL_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/mysql:5.7.20", description: "mysql镜像"),
            string(name: "RABBITMQ_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/rabbitmq:3.7.16-management-alpine-with-cer", description: "rabbitmq镜像"),
            // string(name: "DBINIT_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/dbinit:v3.0", description: "dbinit镜像"),
            string(name: "DBINIT_TAG", defaultValue: "$params.DBINIT_TAG", description: "dbinit镜像"),
            string(name: "DBCONFIG_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/dbconfig:v3.0", description: "dbconfig镜像"),
            string(name: "NODE_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/kindest-node:v1.18.2", description: "kind node 镜像"),
 
            string(name: "FLUENTD_TAG", defaultValue: "image.rightcloud.com.cn/library/fluentd:1.6.2.12-alpine", description: "fluentd镜像"),
            string(name: "RDP_TAG", defaultValue: "image.rightcloud.com.cn/library/rightcloud-rdp:2.0.0", description: "rdp镜像"),
            string(name: "CMPROXY_TAG", defaultValue: "registry.cn-hangzhou.aliyuncs.com/rc-hub/cmp-proxy:1.0", description: "https镜像"),
 
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

        // 这里的namespace只用来发送钉钉消息
        def namespace = "vue-project123"

        def now = new Date().format("yyyyMMddHHmm")

        def pkg_server = '10.69.5.30'
        def pkg_tgz_name = "cmp-aiok8s-installer-${release_version}-${now}.tar.gz"
        
        def pkg_host_tgz_file = "/data/public/aio-k8s-install/${pkg_tgz_name}"

        // all_env_tag中的tag和kuberc-aio仓库中的bin/.env中的tag保持一致
        def all_env_tag = ["INFLUXDB_TAG", "REDIS_TAG", "RDP_TAG", "MONGODB_TAG", "MYSQL_TAG", "RABBITMQ_TAG", "DBINIT_TAG", "DBCONFIG_TAG", "FLUENTD_TAG", "CMPROXY_TAG", "ANSIBLE_TAG", "SERVER_TAG", "DBUPGRADE_TAG", "ADAPTER_TAG", "CONSOLE_TAG", "SCHEDULE_TAG", "MONITORSERVER_TAG", "MONITORAGENT_TAG", "GATEWAY_TAG", "NODE_TAG"]
        // all_values_tag_map的value主要取自values.yaml文件中每一个镜像的完整镜像名中提取标志性部分，用于sed时找到那一行
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

            stage('git clone kuberc-aio') {
                sh "mkdir -p /home/package/cmp/images"
                // 这里只是为了拿 .env文件，真正的分支在打包服务器上已经手动切换好
                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kuberc-aio.git', branch: "master"
                // sh "cp -r * /home/package/cmp/"
            }
            stage("update the latest mirror tag") {
                // 替换.env文件中的镜像为 jenkins参数获取的值
                for (tag in all_env_tag) {
                    sh "sed -i 's|${tag}=.*|'${tag}=${params[tag]}'|g' bin/.env"
                }
                sh "sed -i 's|RELEASE_VERSION=.*|'RELEASE_VERSION=${release_version}'|g' bin/.env"
                sshagent(credentials: ['global-ssh-key-czc']) {
                  sh "ssh -o StrictHostKeyChecking=no -l root ${pkg_server} uname -a"
                  sh "scp -r bin/.env root@${pkg_server}:/tmp/${now}"
                }

            }

            stage("package and upload to baiduwangpan") {

                BAIDU_BDUSS="G9yRHRPSFd3M1JhVFFsNUozRmlRfjFROUR2MEdFSDFuV1ZiZlQtdGpsN2poNnRlSVFBQUFBJCQAAAAAAAAAAAEAAABg0cmsenp0dDIwMTcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOP6g17j-oNeS"
                sshagent(credentials: ['global-ssh-key-czc']) {
                    sh "ssh -o StrictHostKeyChecking=no -l root ${pkg_server} \" cd ${pkg_host_dir} && bash cmp/download_image.sh '${pkg_host_dir}' '/tmp/${now}' '${release_version}' && tar zcf ${pkg_host_tgz_file} cmp --exclude=cmp/.git --exclude=cmp/download_image.sh \""

                    echo "aio-k8s安装包内网下载：http://${pkg_server}/aio-k8s-install/${pkg_tgz_name}"
                    def md5_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${pkg_server} \"md5sum ${pkg_host_tgz_file}\" ").trim()
                    echo "--------------md5：${md5_info}"
                    if (UPLOAD_TO_BAIDU_WANGPAN == "true") {
                        sh "ssh -o StrictHostKeyChecking=no -l root ${pkg_server} \" cd /upload/ && curl --header 'PRIVATE-TOKEN: j-2w5318uPTixK6TLYQ5' http://10.68.6.20:8082/cloudstar/infrastructure-ops/raw/master/upload-baidu/upload.sh| bash -s -- -b ${BAIDU_BDUSS} -s ${pkg_host_tgz_file} -d ${baidu_path} \""
                        share_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${pkg_server} \"cat /tmp/share_link.txt\"").trim()
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
