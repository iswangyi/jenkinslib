def call(k8sserver, namespace, dbmigration, version, checkout=false)  {
    def dirname= "./"
    if (checkout) {
        dirname = "kubernetes-yml"
    }

    dir(dirname) {
        if (checkout) {
            stage('clone yaml template') {
                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
            }
        }

        stage("run server") {
            // sh " sed -i 's|\$serverTag|'${version}'|g'   rightcloud/app/rightcloud-server-template.yaml "
            // sh " sed -i 's|\$dbmigrationTag|'${dbmigration}'|g'   rightcloud/app/rightcloud-server-template.yaml "
            // sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-server-template.yaml"

            sh " sed -i -e 's|\$serverTag|'${version}'|g' -e 's|\$dbmigrationTag|'${dbmigration}'|g'  -e 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-server-template.yaml"


            container('kubectl') {
            //取出pod共享存储(/home/kubectl-admin/)下的config，用于dev-k8s集群的apiServer认证。
            def kubectl="kubectl --kubeconfig=/home/kubectl-admin/config"

            sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-server-template.yaml"

                stage("checking dbmiration status") {
                    timeout(time: 180, unit: 'SECONDS') {
                def dbmigrationstatus= sh(script: "pod=\$(kubectl --kubeconfig=/home/kubectl-admin/config -n ${namespace} get pod --sort-by=.metadata.creationTimestamp | grep -v Evicted | grep rightcloud-server| awk '{print \$1}'|tail -1);kubectl --kubeconfig=/home/kubectl-admin/config -n ${namespace} get pod \${pod} --template \'{{range .status.initContainerStatuses}} {{.state.terminated.exitCode}}{{end}}\'", returnStdout: true).trim()

                while (dbmigrationstatus.trim() == '<no value>') {
                    dbmigrationstatus = sh(script: "pod=\$(kubectl --kubeconfig=/home/kubectl-admin/config -n ${namespace} get pod --sort-by=.metadata.creationTimestamp | grep -v Evicted | grep rightcloud-server|awk '{print \$1}'|tail -1);kubectl --kubeconfig=/home/kubectl-admin/config -n ${namespace} get pod \${pod} --template \'{{range .status.initContainerStatuses}} {{.state.terminated.exitCode}}{{end}}\'", returnStdout: true).trim()
                }

                if (dbmigrationstatus.trim() != "0") {
                            def dbmigrationmsg = sh(script: "pod=\$(kubectl --kubeconfig=/home/kubectl-admin/config -n ${namespace} get pod --sort-by=.metadata.creationTimestamp | grep -v Evicted | grep rightcloud-server|awk '{print \$1}'|tail -1);kubectl --kubeconfig=/home/kubectl-admin/config -n ${namespace} logs \${pod} -c dbmigration", returnStdout: true).trim()

                            error("dbmigration failed with: ${dbmigrationmsg}")

                }
            }
        }

            }
        }
    }

}
