import cn.com.rightcloud.jenkins.Constants


def call(isDeploy = true) {
    node {
        properties([parameters([
                [$class            : 'GitParameterDefinition',
                 name              : 'PARAM_GIT_TAGS',
                 description       : ' tags',
                 type              : 'PT_BRANCH_TAG',
                 branch            : '',
                 branchFilter      : '.*',
                 tagFilter         : '*',
                 sortMode          : 'DESCENDING_SMART',
                 defaultValue      : 'release-test3.0',
                 selectedValue     : 'NONE',
                 quickFilterEnabled: true],
//                choice(name: "QUALITY_ANALYSIS", choices: 'false\ntrue', description: '是否进行质量分析'),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        // 获取Dockerfile 和 db-resource
        def branchname = "${PARAM_GIT_TAGS}"
        parallel(
                failFast: true,
                "clone third party": {
                    dir('/home/jenkins/workspace/third-party') {
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                },
                "clone backup": {
                    dir('/home/jenkins/workspace/backup') {
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-backup/rightcloud-module-cloudbackup.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                },
                "clone container": {
                    dir('/home/jenkins/workspace/container') {
                        checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-container/rightcloud-module-cloudcontainer.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                        sh 'rm -rf .git && rm -rf .idea'
                    }
                }
        )

        def image_name = "image.rightcloud.com.cn/${namespace}/third-party:v.${branchname}.${env.BUILD_NUMBER}"
        def db_image_name = "image.rightcloud.com.cn/${namespace}/third-party-dbmigration:v.${branchname}.${env.BUILD_NUMBER}"
        //准备Dockerfile所需的--build-arg参数
        def  build_args = " --network=host --build-arg   BACKUP_PROJECT=./backup " +
                "--build-arg CONTAINER_PROJECT=./container " +
                "--build-arg SUMMARY_PROJECT=./third-party "
        try {

            stage("build dbmigration image") {
                container('docker') {
                    dir('/home/jenkins/workspace') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            sh "docker build -t ${db_image_name} --no-cache -f third-party/docker/Dockerfile.db --no-cache . ${build_args}"
                            sh "docker push ${db_image_name}"
                        }
                    }
                }
            }
            if (params.QUALITY_ANALYSIS == "true") {
                stageCodeQualityAnalysis()
            }

            stage("build server image") {
                container('docker') {
                    dir('/home/jenkins/workspace') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            sh "docker build -t ${image_name} --no-cache -f third-party/docker/Dockerfile --no-cache . ${build_args}"
                            sh "docker push ${image_name}"
                        }
                    }
                }
                if (isDeploy) {
                    def group = 'rightcloud-v4';
                    def service_name = 'third-party';
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
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
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
        } finally {
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
