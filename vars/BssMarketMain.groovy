import cn.com.rightcloud.jenkins.Constants

// env  project、dev、demo、self_test
def call(isDeploy=true) {
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
             choice(name: "IS_DEPLOY", choices: 'true\nfalse', description: '是否部署到环境,测试人员使用时需要选择False'),
         ])
         ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

         try {
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/bss-management:v.${branchname}.${env.BUILD_NUMBER}"

            stage("build") {
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                def  build_args = " --network=host --build-arg   MANAGEMENT_PROJECT=./ "
                container('docker') {
                    // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                        sh "docker build -t ${image_name} --no-cache -f docker/Dockerfile --no-cache . ${build_args}"
                        sh "docker push ${image_name}"
                    }
                }
            }
            if (isDeploy && params.IS_DEPLOY == "true") {
                stageK8ServiceDeploy("market", namespace, kubectl, image_name, false, "bss")
            }
         } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }
}
