def call(service_name, namespace, kubectl, image_name, delete=false, group="rightcloud-v4") {
    stage('deploy service') {
        container('kubectl') {
            dir("kubernetes-yml") {
                stage('clone yaml template') {
                    git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
                }
                stage('run in k8s') {
                    sh " cd ./${group}/app/${service_name}/overlays/${namespace} && kustomize edit set image ${service_name}=${image_name} && kustomize edit set namespace ${namespace}"

                    //先删除
                    if (delete) {
                        try {
                            sh " kustomize build ./${group}/app/${service_name}/overlays/${namespace} | ${kubectl} delete -f -"
                        }catch(e){
                            println(e.getMessage())
                        }
                    }
                    sh " kustomize build ./${group}/app/${service_name}/overlays/${namespace} | ${kubectl} apply -f -"
                }
            }
        }
    }
}
