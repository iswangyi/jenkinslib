def call(namespace, server, domain, checkout=false) {
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

        stage("deploy traefik") {
            sh " sed -i 's/\$namespace/'${namespace}'/g'   traefik/traefik-rightcloud.yaml "
            sh " sed -i 's/\$domain/'${domain}'/g'   traefik/traefik-rightcloud.yaml "

            container("kubectl") {
            sh "kubectl -s ${server} apply -f  ./traefik/traefik-rightcloud.yaml"
            }
        }
    }

}
