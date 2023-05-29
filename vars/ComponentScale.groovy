
def call(isDeploy="true") {
    node {
        properties([parameters([
                choice(name: "NAMESPACE",choices:'cmp-dev-a\ncmp-dev-b\ncmp-dev-c', defaultValue: "$params.NAMESPACE", description: "集群namespace"),
//                choice(name: "K8S_ENV", choices:'dev\nproject\nself_env\nprod\noem', defaultValue: "$params.K8S_ENV", description: "k8s集群环境"),
                choice(name: "COMPONENT", choices:'rightcloud-server\nrightcloud-iam\nrightcloud-schedule\nrightcloud-resource\nrightcloud-monitor-server\nrightcloud-analysis\nrightcloud-backup', defaultValue: "$params.COMPONENT", description: "组件名称"),
                choice(name: "NUM", choices:'0\n1', defaultValue: "0", description: "0将关闭组件，1将启动组件"),
        ])
        ])
        stage('scale component'){
            def namespace = params.NAMESPACE
            def component = params.COMPONENT
            def num = params.NUM
            def kubectl = getKubectl(getK8sEnv(namespace))
            container('kubectl') {
                sh "${kubectl} -n ${namespace} scale --replicas=${num} deployment ${component}"
            }
        }
    }
}
