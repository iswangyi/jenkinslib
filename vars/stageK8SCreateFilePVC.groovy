def call(namespace, server, nfs_server, checkout=false) {
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

        stage("deploy redis") {
            sh " sed -i 's/namespaceVersion/'${namespace}'/g'   rightcloud/replace-yaml.sh "

            sh "  chmod +x  ./rightcloud/replace-yaml.sh"
            sh "  ./rightcloud/replace-yaml.sh"

            sh "sed -i 's/\$nfs_server/'${nfs_server}'/g'  ./rightcloud/pv-pvc/rightcloud-files-pv.yaml"
            container("kubectl") {
            sh "kubectl -s ${server} apply -f  ./rightcloud/pv-pvc/rightcloud-files-pv.yaml"
            sh "kubectl -s ${server} apply -f  ./rightcloud/pv-pvc/rightcloud-files-pvc.yaml"
            }
        }
    }

}
