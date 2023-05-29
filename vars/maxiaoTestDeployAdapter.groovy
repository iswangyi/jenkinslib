def call(k8sserver, namespace, version, checkout=false)  {
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
            // sh " sed -i 's|\$adapterTag|'${version}'|g'   rightcloud/app/rightcloud-adapter-template.yaml "
            // sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-adapter-template.yaml"
            sh " sed -i -e  's|\$adapterTag|'${version}'|g' -e 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-adapter-template.yaml"

            container('kubectl') {
                //取出pod共享存储(/home/kubectl-admin/)下的config，用于dev-k8s集群的apiServer认证。
                def kubectl="kubectl --kubeconfig=/home/kubectl-admin/config"
                sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-adapter-template.yaml"
            }
        }
    }

}
