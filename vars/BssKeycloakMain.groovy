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
             string(name: "VERSION_PREFIX", defaultValue: "$params.VERSION_PREFIX", description: "版本号前缀"),
             choice(name: "IS_DEPLOY", choices: 'true\nfalse', description: '是否部署到环境,测试人员使用时需要选择False'),
         ])
         ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

         try {
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/bss-keycloak:v.${branchname}.${env.BUILD_NUMBER}"
            stage("build") {

                def build_args = " --build-arg CONSOLE_THEME_PROJECT=./console-login-theme " +
                        " --build-arg MANAGEMENT_THEME_PROJECT=./management-login-theme "

                parallel(
                        failFast: true,
                        "clone login theme": {
                            dir('/home/jenkins/workspace/console-login-theme') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                                sh "chmod -R 775 /home/jenkins/workspace/console-login-theme"
                                sh "sed -i 's|#appVersion#|'$params.VERSION_PREFIX'|g' /home/jenkins/workspace/console-login-theme/login/template.ftl"
                            }
                        },
                        "clone management theme":{
                            dir('/home/jenkins/workspace/management-login-theme') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/cloudstar/cloud-boss/management-login-theme.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                                sh "chmod -R 775 /home/jenkins/workspace/management-login-theme"
                                sh "sed -i 's|#appVersion#|'$params.VERSION_PREFIX'|g' /home/jenkins/workspace/management-login-theme/login/template.ftl"
                            }
                })

                container('docker') {
                    // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                        dir('/home/jenkins/workspace') {
                            sh "docker build -t ${image_name} --no-cache -f console-login-theme/docker/Dockerfile --no-cache . ${build_args}"
                            sh "docker push ${image_name}"
                        }
                    }
                }
            }
             stage("deploy"){
                 if (isDeploy || params.IS_DEPLOY == "true") {
                     stageK8ServiceDeploy("bss-keycloak", namespace, kubectl, image_name, false, "bss")
                 }
             }
         } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }
}
