import cn.com.rightcloud.jenkins.Constants

// groovy的数组截取是左开右开
def filterGroup(repo) {
    return repo ? repo.split('/')[0..-2].join('/') : "error"
}

def filterProject(repo) {
    return repo ? repo.split('/')[-1] : "error"
}


def call(isDeploy="true") {
//    def kubectl = getKubectl(k8sEnv)

    node {
        properties([parameters([
            string(name: "ANSIBLE_TAG", defaultValue: "", description: "ansible_image镜像"),
        ])
         ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))
        try {
            container('kubectl') {
                dir("kubernetes-yml") {
                    stage('clone yaml template') {
                        git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                    stage("run ansible") {
                       sh " sed -i 's|\$ansibleTag|'${ANSIBLE_TAG}'|g'   rightcloud/app/rightcloud-ansible-template.yaml "
                       sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-ansible-template.yaml"
           
                       sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-ansible-template.yaml"
                    }
               }
         }
         } catch (e) {
            def errorMessage = e.getMessage()
            println(errorMessage)
            println("current result:" + currentBuild.result)
            if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
                println("FAILURE send message info")
                notifyDingDing(false, namespace, Constants.BUILD_FAILURE_MESSAGE + "\n" + errorMessage, Constants.BUILD_NOTIFY_PEOPEL)
            } else {
                println("ABORTED not send info")
            }
            error("Failed build as " + e.getMessage())
        }finally{
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
