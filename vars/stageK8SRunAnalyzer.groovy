def call(k8server, namespace, version, checkout=false)  {
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

        stage("run analyzer") {
            sh " sed -i 's|\$analyzerTag|'${version}'|g'   rightcloud/app/rightcloud-analyzer-template.yaml "
            sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-analyzer-template.yaml"

            sh "kubectl -s ${k8server} apply -f  ./rightcloud/app/rightcloud-analyzer-template.yaml"
        }
    }

}
