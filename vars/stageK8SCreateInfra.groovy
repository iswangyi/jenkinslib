import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            string(name: "MONGO_VERSION", defaultValue: '3.7', description: ""),
            string(name: "MYSQL_VERSION", defaultValue: '5.7.20', description: ""),
            string(name: "RABBITMQ_VERSION", defaultValue: '3.7.16-management-alpine-with-cer', description: ""),
            string(name: "REDIS_VERSION", defaultValue: 'latest', description: ""),
//            string(name: "SHELLINABOX_VERSION", defaultValue: 'v3.2', description: ""),
            string(name: "RDP_VERSION", defaultValue: '0.9.14', description: ""),
            string(name: "MONITOR_VERSION", defaultValue: 'v1.0', description: ""),
            string(name: "NAMESPACE", defaultValue: '', description: ""),
            string(name: "DBINIT_VERSION", defaultValue: 'v1.2', description: ""),
            booleanParam(name: "INIT_DB", defaultValue: false, description: "is init db"),
            booleanParam(name: "FORCE_INIT_DB", defaultValue: false, description: "force init db if db already exist"),
            choice(choices: 'product\nself_env', description: 'Which environment to create the infra?', name: 'region'),

        ])])

        if (!params.NAMESPACE?.trim()) {
            error("namespace is null or empty, nothing to do")
        }

        def nfs_server
        def k8s_server
        def domain

        if (params.region == 'product')  {
            nfs_server = Constants.PRODUCT_K8S_NFS
            k8s_server = Constants.PRODUCT_K8S_SERVER
            domain = "rc.com"
        } else if (params.region == 'self_env') {
            nfs_server = Constants.SELFENV_K8S_NFS
            k8s_server = Constants.SELFENV_K8S_SERVER
            domain = "rctest.com"
        } else {
            error("unknown region to create infra")
        }

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
//        stageK8SCreateShellinabox(params.NAMESPACE, params.SHELLINABOX_VERSION, k8s_server, true)
        stageK8SCreateRDP(params.NAMESPACE, params.RDP_VERSION, k8s_server, true)
        stageK8SCreateFilePVC(params.NAMESPACE, k8s_server, nfs_server, true)
        if (params.INIT_DB) {
            stageK8SDBInit(params.NAMESPACE, params.DBINIT_VERSION, k8s_server, params.FORCE_INIT_DB, true)
        }


        // stageK8SCreateMonitor(params.NAMESPACE, params.MONITOR_VERSION, true)
    }

}
