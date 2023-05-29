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
             [$class: 'GitParameterDefinition',
             name: 'PARAM_GIT_TAGS',
             description: ' tags',
             type:'PT_BRANCH_TAG',
             branch: '',
             branchFilter: '.*',
             tagFilter:'*',
             sortMode:'DESCENDING_SMART',
             defaultValue: 'release-test3.0',
             selectedValue:'NONE',
             quickFilterEnabled: true],

            string(name: "VERSION_PREFIX", defaultValue: "$params.VERSION_PREFIX", description: "版本号前缀"),
//             choice(name: "NAMESPACE",choices:'cmdb', defaultValue: "$params.NAMESPACE", description: "集群namespace"),
//             choice(name: "K8S_ENV", choices:'dev\nproject\nself_env\nprod\noem', defaultValue: "$params.K8S_ENV", description: "k8s集群环境"),
//            choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'),
         ])
         ])

//        def kubectl = getKubectl(params.K8S_ENV)
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        // 获取Dockerfile 和 db-resource
         def branchname = "${PARAM_GIT_TAGS}"
         checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
         def image_name = "image.rightcloud.com.cn/${namespace}/cmdb-dbmigration:v.${branchname}.${env.BUILD_NUMBER}"

         try {
            parallel(
                failFast: true,
                "build & deploy": {
                    
                    stage("build db migration image") {
                        //准备Dockerfile所需的--build-arg参数
                        def build_args = " --build-arg DBSERVICE_PROJECT=./  "

                        container('docker') {
                            // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                                sh 'ls -al'
                                sh 'pwd'
                                sh "docker build -t ${image_name} --no-cache -f Dockerfile --no-cache . ${build_args}"

                                sh "docker push ${image_name}"
                            }
                        }
                    }

                    if (isDeploy) {

                        stageK8ServiceDeploy("cmdb-dbservice", namespace, kubectl, image_name, true, 'cmdb')
                    }
                    notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
                }
            )
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
