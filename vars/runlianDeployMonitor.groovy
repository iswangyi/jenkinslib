def call(k8sserver, server_or_agent, namespace, version, checkout=false)  {
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

        stage("run monitor") {
            sh " sed -i 's|\$monitor'${server_or_agent}'Tag|'${version}'|g'   rightcloud/app/rightcloud-monitor-${server_or_agent}-template.yaml "
            sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-monitor-${server_or_agent}-template.yaml"

            sh "kubectl -s ${k8sserver} apply -f  ./rightcloud/app/rightcloud-monitor-${server_or_agent}-template.yaml"
        }
    }

}
