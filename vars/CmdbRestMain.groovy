import cn.com.rightcloud.jenkins.Constants

// env  project、dev、demo、self_test
def call(isDeploy=true) {
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
//                choice(name: "NAMESPACE",choices:'cmdb', defaultValue: "$params.NAMESPACE", description: "集群namespace"),
//             choice(name: "K8S_ENV", choices:'dev\nproject\nself_env\nprod\noem', defaultValue: "$params.K8S_ENV", description: "k8s集群环境"),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        try {
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/cmdb-rest:v.${branchname}.${env.BUILD_NUMBER}"
            def db_image_name = "image.rightcloud.com.cn/${namespace}/cmdb-dbmigration:v.${branchname}.${env.BUILD_NUMBER}"

            stage('clone rest'){
                dir('/home/jenkins/workspace/cmdb-rest') {
                    // 获取Dockerfile
                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                    sh 'rm -rf .git && rm -rf .idea'
                    stage('Version') {
                        sh 'time=$(date "+%Y%m%d%H%M") && sed -i "s/build/$time/g" src/main/resources/version.properties'
                    }
                }
            }


            stage("clone db service "){
                dir('/home/jenkins/workspace/cmdb-db-service') {
                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/cloudstar/cloud-cmdb/cmdb-db-service.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                    sh 'rm -rf .git && rm -rf .idea'
                }
            }
            def build_args = " --build-arg REST_PROJECT=./cmdb-rest  " +
                    " --build-arg DBSERVICE_PROJECT=./cmdb-db-service "

            stage("build dbmigration image") {
                container('docker') {
                    dir('/home/jenkins/workspace') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            sh "docker build -t ${db_image_name} --no-cache -f cmdb-db-service/Dockerfile --no-cache . ${build_args}"
                            sh "docker push ${db_image_name}"
                        }
                    }
                }
            }
//            stage("check dbmigration") {
//                if (isDeploy) {
//                    validateDBMigration(db_image_name, namespace, "${namespace}.rctest.com", getK8sEnv(namespace))
//                }
//            }

            stage("build rest") {
                container('docker') {
                    // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                        dir('/home/jenkins/workspace') {
                            sh "docker build -t ${image_name} --no-cache -f cmdb-rest/Dockerfile --no-cache . ${build_args}"
                            sh "docker push ${image_name}"
                        }
                    }
                }
            }

            if (isDeploy) {
//                stageK8ServiceDeploy("cmdb-rest", namespace, kubectl, image_name, false, 'cmdb')
                def group = 'cmdb';
                def service_name = 'cmdb-rest';
                def delete  = false;
                stage('deploy service') {
                    container('kubectl') {
                        dir("/home/jenkins/workspace/kubernetes-yml") {
                            stage('clone yaml template') {
                                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
                            }
                            stage('run in k8s') {
                                sh " cd ./${group}/app/${service_name}/overlays/${namespace} && kustomize edit set image ${service_name}=${image_name} && kustomize edit set image dbmigration=${db_image_name} && kustomize edit set namespace ${namespace}"
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
