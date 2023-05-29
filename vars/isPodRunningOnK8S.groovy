def call(server, namespace, app) {
    container('kubectl') {
        def EXISTS = sh(script: "kubectl -s ${server} get pods -n ${namespace} -l app=${app} |grep \"${app}\" |grep \"Running\" |wc -l", returnStdout: true).trim()
        return EXISTS != "0"
    }
}
