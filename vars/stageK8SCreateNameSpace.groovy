def call(namespace, server) {
    stage("creating namespace") {
        echo "checking namespace ${params.NAMESPACE}"
        
        def isexist
        container("kubectl") {
            isexist = sh(script:" kubectl -s ${server} get namespace ${namespace}", returnStatus:true)
        }

        if (isexist != 0) {
            echo "namespace ${namespace} is not exist, creating it"
            if (!isNamespaceValid(namespace)) {
                error("creating namespace $namespace is not allowed")
            } else {
                container("kubectl") {
                sh "kubectl -s ${server} create namespace $namespace"
                }
            }
        } else {
            echo "namespace ${namespace} already exist...."
        }
    }
}
