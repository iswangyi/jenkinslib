import cn.com.rightcloud.jenkins.Constants

def call(namespace, region) {
    if (!isNamespaceValid(namespace)) {
        error("destroy namespace \"$namespace\" is not allowed")
    }
    def nfs_server
    def k8s_server
    
    if (region == 'product')  {
        nfs_server = Constants.PRODUCT_K8S_NFS
        k8s_server = Constants.PRODUCT_K8S_SERVER
    } else if (region == 'self_env') {
        nfs_server = Constants.SELFENV_K8S_NFS
        k8s_server = Constants.SELFENV_K8S_SERVER
    } else {
        error("unknown region to create infra")
    }


    stage("delete all in namespace $namespace") {
        sh "kubectl -s ${k8s_server} delete --all deployments --namespace=$namespace"
        sh "kubectl -s ${k8s_server} delete --all services --namespace=$namespace"
        sh "kubectl -s ${k8s_server} delete --all jobs --namespace=$namespace"
        sh "kubectl -s ${k8s_server} delete --all ingress --namespace=$namespace"
        sh "kubectl -s ${k8s_server} delete pvc rightcloud-pvc-files rightcloud-pvc-mysql --namespace=$namespace"
        sh "kubectl -s ${k8s_server} delete pv $namespace-pv-files $namespace-pv-mysql"
        sh "kubectl -s ${k8s_server} delete configmap rightcloud-config --namespace=$namespace"
        sh "kubectl -s ${k8s_server} delete namespace $namespace"
    }

}
