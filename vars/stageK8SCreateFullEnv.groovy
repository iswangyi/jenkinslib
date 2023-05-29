import cn.com.rightcloud.jenkins.Constants

def call() {
    node (){
        def ansibletagname
        def consoletagname
        def dbmigrationtagname
        def servertagname
        def adaptertagname
        def scheduletagname

        properties([parameters([
            string(name: "NAMESPACE", defaultValue: "", description: "namespace for build"),
            string(name: "SERVER_TAG",
                   description: "server tag used to build the image"),
            string(name: "SCHEDULE_TAG",
                   description: "schedule tag used to build the image"),
            string(name: "CONSOLE_TAG",
                   description: "console tag used to build the image"),
            string(name: "ANSIBLE_TAG",
                   description: "ansible tag used to build the image"),


            string(name: "MONGO_VERSION", defaultValue: '3.7', description: ""),
            string(name: "MYSQL_VERSION", defaultValue: '5.7.20', description: ""),
            string(name: "RABBITMQ_VERSION", defaultValue: '3.6', description: ""),
            string(name: "REDIS_VERSION", defaultValue: 'latest', description: ""),
            string(name: "SHELLINABOX_VERSION", defaultValue: 'v3.2', description: ""),
            string(name: "MONITOR_VERSION", defaultValue: 'v1.0', description: ""),
            string(name: "DBINIT_VERSION", defaultValue: 'v1.0', description: ""),
            booleanParam(name: "INIT_DB", defaultValue: false, description: "is init db"),
            booleanParam(name: "FORCE_INIT_DB", defaultValue: false, description: "force init db if db already exist"),

            string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://test.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
        ])
        ])

        try {
            if (!params.NAMESPACE?.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            def k8s_server = Constants.PRODUCT_K8S_SERVER
            def domain = "rc.com"
            def nfs_server=Constants.PRODUCT_K8S_NFS

            container('kubectl') {
                stage("create nfs dir") {
                    sshagent(['global-ssh-key-czc']) {
                        sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${params.NAMESPACE}/mysql "
                        sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${params.NAMESPACE}/files "
                    }
                }

                stageK8SCreateNameSpace(params.NAMESPACE, k8s_server)
                stageK8SCreateConfigMap(params.NAMESPACE, k8s_server, domain, true)
                stageK8SCreateTraefik(params.NAMESPACE, k8s_server, domain, true)
                stageK8SCreateRedis(params.NAMESPACE, params.REDIS_VERSION, k8s_server, true)
                stageK8SCreateMQ(params.NAMESPACE, params.RABBITMQ_VERSION, k8s_server, true)
                stageK8SCreateMongo(params.NAMESPACE, params.MONGO_VERSION, k8s_server, true)
                stageK8SCreateMysql(params.NAMESPACE, params.MYSQL_VERSION, k8s_server, nfs_server, true)
                stageK8SCreateShellinabox(params.NAMESPACE, params.SHELLINABOX_VERSION, k8s_server, true)
                stageK8SCreateFilePVC(params.NAMESPACE, k8s_server, nfs_server, true)
                if (params.INIT_DB) {
                    stageK8SDBInit(params.NAMESPACE, params.DBINIT_VERSION, k8s_server, params.FORCE_INIT_DB, true)
                }

                // stageK8SCreateConfigMap(params.NAMESPACE, true)
                // stageK8SCreateTraefik(params.NAMESPACE, true)
                // stageK8SCreateRedis(params.NAMESPACE, params.REDIS_VERSION, true)
                // stageK8SCreateMQ(params.NAMESPACE, params.RABBITMQ_VERSION, true)
                // stageK8SCreateMongo(params.NAMESPACE, params.MONGO_VERSION, true)
                // stageK8SCreateMysql(params.NAMESPACE, params.MYSQL_VERSION, true)
                // stageK8SCreateShellinabox(params.NAMESPACE, params.SHELLINABOX_VERSION, true)
                // stageK8SCreateFilePVC(params.NAMESPACE, true)
                // if (params.INIT_DB) {
                //     stageK8SDBInit(params.NAMESPACE, params.DBINIT_VERSION, params.FORCE_INIT_DB, true)
                // }
            }

            dbmigrationtagname = stageCreateDBMigration(params.DOCKER_BUILD_SERVER,
                                                        "cloudstar/rightcloud",
                                                        params.SERVER_TAG,
                                                        params.NAMESPACE,
                                                        true, null, true)

            servertagname = stageBuildServerImage(params.DOCKER_BUILD_SERVER,
                                                  "cloudstar/rightcloud",
                                                  params.SERVER_TAG,
                                                  params.NAMESPACE,
                                                  true, null, true)

            adaptertagname = stageBuildAdapterImage(params.DOCKER_BUILD_SERVER,
                                                    "cloudstar/rightcloud",
                                                    params.SERVER_TAG,
                                                    params.NAMESPACE,
                                                    true, null, true)

            scheduletagname = stageBuildScheduleImage(params.DOCKER_BUILD_SERVER,
                                                      "cloudstar/rightcloud-schedule",
                                                      params.SCHEDULE_TAG,
                                                      params.NAMESPACE,
                                                      true, null, true)

            ansibletagname = stageBuildAnsibleImage(params.DOCKER_BUILD_SERVER,
                                                    "rightcloud/ansible-adapter",
                                                    params.ANSIBLE_TAG,
                                                    params.NAMESPACE,
                                                    true, null, true)
            consoletagname = stageBuildConsoleImage(params.DOCKER_BUILD_SERVER,
                                                    "cloudstar/rightcloud-portal",
                                                    params.CONSOLE_TAG,
                                                    params.NAMESPACE,
                                                    true, null, true)

            container('kubectl') {
                stageK8SRunServer(k8s_server,
                    params.NAMESPACE, dbmigrationtagname, servertagname, true)
                stageK8SRunAdapter(k8s_server,
                    params.NAMESPACE, adaptertagname, true)

                stageK8SRunSchedule(k8s_server,
                    params.NAMESPACE, scheduletagname, true)
                stageK8SRunConsole(k8s_server,
                    params.NAMESPACE, consoletagname, true)
                stageK8SRunAnsible(k8s_server,
                    params.NAMESPACE, ansibletagname, true)
            }
        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
