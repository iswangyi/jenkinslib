import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            string(name: "MONGO_VERSION", defaultValue: '3.7', description: ""),
            string(name: "MYSQL_VERSION", defaultValue: '5.7.20', description: ""),
            string(name: "RABBITMQ_VERSION", defaultValue: '3.7.16-management-alpine-with-cer', description: ""),
            string(name: "REDIS_VERSION", defaultValue: 'latest', description: ""),
            // string(name: "SHELLINABOX_VERSION", defaultValue: 'v3.2', description: ""),
            string(name: "RDP_VERSION", defaultValue: '2.0.0', description: ""),
            string(name: "NAMESPACE", defaultValue: '', description: ""),
            string(name: "DBINIT_VERSION", defaultValue: 'v3.1', description: ""),
            string(name: "FLUENTD_IMAGE", defaultValue: 'image.rightcloud.com.cn/library/fluentd:1.6.2.12-alpine', description: ""),
            string(name: "INFLUXDB_IMAGE", defaultValue: 'image.rightcloud.com.cn/library/influxdb:1.7.6-alpine', description: ""),
            string(name: "NACOS_IMAGE", defaultValue: '1.4.0', description: ""),
            booleanParam(name: "INIT_DB", defaultValue: false, description: "is init db"),
            booleanParam(name: "FORCE_INIT_DB", defaultValue: false, description: "force init db if db already exist"),
            choice(choices: 'project\nself_env\ndev\nprod\noem', description: 'Which environment to create the infra?', name: 'region'),

        ])])

        if (!params.NAMESPACE?.trim()) {
            error("namespace is null or empty, nothing to do")
        }

        switch(params.region) {
            case 'project':
                nfs_server = Constants.PRODUCT_K8S_NFS
                // 之前的self和projectk8s环境，可以直接8080直接访问apiServer，不需要认证
                kubectl = "kubectl -s ${Constants.PRODUCT_K8S_SERVER} "
                domain = "rc.com"
                break
            case 'self_env':
                nfs_server = Constants.SELFENV_K8S_NFS
                kubectl = "kubectl -s ${Constants.SELFENV_K8S_SERVER} "
                domain = "rctest.com"
                break
            case 'dev':
                nfs_server = Constants.DEV_K8S_NFS
                // 这里的cert在kubectl镜像中，如果后续新增k8s环境，需要将cert证书打包进kubectl镜像放在不同目录
                kubectl = "kubectl --kubeconfig=/home/kubectl-cer/config "
                domain = "rc.com"
                break
            case 'prod':
                nfs_server = Constants.DEMO_K8S_NFS
                kubectl="kubectl -s https://10.68.12.7:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJkYXNoYm9hcmQtYWRtaW4tdG9rZW4tam1tNzkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGFzaGJvYXJkLWFkbWluIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiZTE2ZmU2YjMtNzIyNy0xMWU5LTk3MDItMDA1MDU2OGRlNjE4Iiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50Omt1YmUtc3lzdGVtOmRhc2hib2FyZC1hZG1pbiJ9.QSxrMDb6_nEn5JwSlXYbSpxndWxxUvJ_SWCJ8NtZEyVQfEu1wJdUuWj4OTXIyDHkDBS-7eKnDfKXFeLPwo2kSJW9FmyakHnmzfMb-fFc0_KQBpW7EHBigBd6SrcH1E_ZDm6N4UqXOAqFrO2KBRIzerBAQwxaiYEc-k5N1Pxzgz1FNO2-f0qkD2nirqO2G4xS9XaP0Lr8cm_eOolpBSPNI3mQZgKbbsLtxlOqQq5s47M7SJazRod7WZVFNcev9tYlkLeRnwttH5cxQ0-IShFyOoBj_jzk81xfuZySFfF-NjZon3PkC--7g3Uw-hzcUfihx0LfUSgCcLVzu-AHRyCWrIGFZ5XtYckfpSB_rqoU_4ltKhbI6CzvV8siADSOtdY5fBxsdPN3mS97ITLvCLwq7cQYSCYyEIeE39h7jf2ovfdOB3i33qw55P1S4hxKjAsGXSQqy5Z1bbjVORAUtESJQSM_31a-z52knEpgq-E5IOkTJ_R0ClcF-SLHlg_UqY7ur5gFv6fmOiXNi_pRF9_X_bB4R2F2-H2e6aThJAmRV_UKLI-xIvfUEnOYjjoc6AnCja0-EbRyV2gB5Arsud6yFJIjl15Aca-Qsz1pZrI7s8vJfLKTKzh0lT4832eO2L1YPzMWWQyF2Euh7cc4P1mYty9K3WheR13mKgMBP3Dd_Zs --insecure-skip-tls-verify=true"
                domain = "rightcloud.com.cn"
                break
            case 'oem':
                nfs_server = Constants.OEM_K8S_NFS
                kubectl="kubectl -s https://10.69.5.61:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IlhtUXRJNDV6a0Q2cjktLW51STktMlZ1WW56YUhvckthN3dQc09oNkw1aTgifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLW0yc2Z4Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJjZmRmNGEyYi01ZmJiLTQ0NWMtOTgzZS01ZjkyYmIzM2UwZmEiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.rNbruVkzXDGIWvWDWFsdPvG0lYIVyxzLTHaArSezDFj1uF-bPm6bG1x5bMOoUO3evt-9kwFfNMROX1URKFLDqA-AyRiurLw-P_ahS298gvVJ9LHT8bBcWbQl9S4ZfN8syNN71HsqHWbqjdXUdtw0pGiC-05lkJwedf5MtKPLT6iZORT89165m6Ps5jI_BtjiX7upc-7ygHsRtiKjQ60c9w2uhIWpItdgwxHP4d2zZ2HerexrkgZ3yqlXZJuZasOWS5WkW7-raoaH5IuzWQFbx3DhhmiG2SKpdlgzskF1JGIF1dv-JSBg5l15wP4EZAozZf3ENuraP7qU0hp5DWP5CQ --insecure-skip-tls-verify=true"
                domain = "cmp.com"
                break
            default: 
                error("unknown region to create infra")
                break
        }

        def namespace = sh(returnStdout: true, script: "echo ${params.NAMESPACE}").trim()
        echo "the region is ${params.region}"


        stage("create nfs dir") {
            sshagent(['global-ssh-key-czc']) {
                sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${namespace}/mysql "
                sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${namespace}/files "
                sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${namespace}/feature "
                sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${namespace}/fluentd "
                sh "ssh -T -o StrictHostKeyChecking=no -l root $nfs_server mkdir -p /mnt/sdb/nfs/${namespace}/influxdb "
            }
        }

        // git 下载yaml template
        stage('clone yaml template') {
            git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
        }

        stage("replace all") {
            // prepare replace all
            sh "sed -i 's/\$namespace/'${namespace}'/g'                              ./traefik/traefik-rightcloud.yaml"
            sh "sed -i 's/\$domain/'${domain}'/g'                                    ./traefik/traefik-rightcloud.yaml"
            sh "sed -i 's/\$domain/'${domain}'/g'                                    ./rightcloud-v4/app/rightcloud-config-template.yaml"
            sh "sed -i 's/nfs_placeholder/'${nfs_server}'/g'                         ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's/redisVersion/'${params.REDIS_VERSION}'/g'                  ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's/rabbitmqVersion/'${params.RABBITMQ_VERSION}'/g'            ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's/mongoVersion/'${params.MONGO_VERSION}'/g'                  ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's/mysqlVersion/'${params.MYSQL_VERSION}'/g'                  ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's/windowsrdpVersion/'${params.RDP_VERSION}'/g'               ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's/namespaceVersion/'${namespace}'/g'                         ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's|fluentdTag_placeholder|'${params.FLUENTD_IMAGE}'|g'          ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's|influxdbTag_placeholder|'${params.INFLUXDB_IMAGE}'|g'        ./rightcloud-v4/replace-yaml.sh "
            sh "sed -i 's|nacosVersion|'${params.NACOS_IMAGE}'|g'        ./rightcloud-v4/replace-yaml.sh "

            // 初始化mysql
            if (params.INIT_DB) {
                sh "sed -i 's/dbinitVersion/'${params.DBINIT_VERSION}'/g'       ./rightcloud-v4/replace-yaml.sh "
                sh "sed -i 's/isforceinitdb/'${params.FORCE_INIT_DB}'/g'        ./rightcloud-v4/replace-yaml.sh "
            }
            // replace all placeholder at all yaml manifest
            sh "chmod +x ./rightcloud-v4/replace-yaml.sh"
            sh "./rightcloud-v4/replace-yaml.sh"
        }

        container("kubectl") {

            stage("creating namespace") {
                echo "checking namespace ${namespace}"
                def isexist = sh(script:" ${kubectl} get namespace ${namespace}", returnStatus:true)

                if (isexist != 0) {
                    echo "namespace  is not exist, creating it"
                    if (false) {
                        error("creating namespace ${namespace} is not allowed")
                    } else {
                        sh "${kubectl} create namespace ${namespace}"
                    }
                } else {
                    echo "namespace ${namespace} already exist...."
                }
            }

            // 部署各基础组件，包括mysql，mongo，rabbitMQ，rdp，redis，ingress，configMap，FilePVC（给console用）
            stage("init all") {
                // redis rabbitmq mongoDB是否需要做数据持久化
                def createPv=false

                // deploy configMap
                sh "${kubectl} apply -f ./rightcloud-v4/app/rightcloud-config-template.yaml"

                // deploy ingress
                sh "${kubectl} apply -f  ./traefik/traefik-rightcloud.yaml"

                // deploy  redis、rabbitmq、mongo
                // TODO 如果中途某个shell失败，下次如何保证幂等，验证一下pv和pvc的创建，如果已经存在。。。?
                // 这里先简单粗暴的 || true解决
                if (createPv) {
                    sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-redis-pv.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-redis-pvc.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-redis.yaml"

                    sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-rabbitmq-pv.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-rabbitmq-pvc.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-rabbitmq.yaml"

                    sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-mongo-pv.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-mongo-pvc.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-mongo.yaml"
                }else {
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-redis-no-pv.yaml || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-rabbitmq.yaml"
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-mongo-no-pv.yaml || true"
                }

                // deploy mysql
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-mysql-pv.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-mysql-pvc.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-mysql.yaml"

                // deploy rdp
                sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-rdp.yaml"

                // deploy FilePVC，给console和server用的
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-files-pv.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-files-pvc.yaml || true"

                // deploy feature pv-pvc，给server用的，保存许可证特征码
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-feature-pv.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-feature-pvc.yaml || true"

                 // deploy fluentd and pv-pvc，用作日志服务
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-fluentd-pv.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-fluentd-pvc.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-fluentd.yaml"

                // deploy influxdb and pv-pvc，用作监控服务
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-influxdb-pv.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/pv-pvc/rightcloud-influxdb-pvc.yaml || true"
                sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-influxdb.yaml"


                if (params.INIT_DB) {
                    sh "${kubectl} --namespace=${namespace} delete job rightcloud-dbinit || true"
                    sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-dbinit.yaml"
                }
                // deploy nacos 服务
                sh "${kubectl} apply -f  ./rightcloud-v4/infra/rightcloud-nacos.yaml || true"
            }
        }
        
    }
}
