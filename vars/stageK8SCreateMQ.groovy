def call(namespace, version, server, checkout=false, createpvc=false) {
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

        stage("deploy rabbitmq") {
            sh " sed -i 's/rabbitmqVersion/'${version}'/g'   rightcloud/replace-yaml.sh "
            sh " sed -i 's/namespaceVersion/'${namespace}'/g'   rightcloud/replace-yaml.sh "

            sh "  chmod +x   ./rightcloud/replace-yaml.sh"
            sh "  ./rightcloud/replace-yaml.sh"

            container("kubectl") {
            if (createpvc) {
                sh "kubectl -s ${server} apply -f  ./rightcloud/pv-pvc/rightcloud-rabbitmq-pv.yaml"
                sh "kubectl -s ${server} apply -f  ./rightcloud/pv-pvc/rightcloud-rabbitmq-pvc.yaml"
                sh "kubectl -s ${server} apply -f  ./rightcloud/infra/rightcloud-rabbitmq.yaml"
            } else {
                sh "kubectl -s ${server} apply -f  ./rightcloud/infra/rightcloud-rabbitmq-no-pv.yaml"
            }
            }
        }
    }

}
