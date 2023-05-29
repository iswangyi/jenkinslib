import cn.com.rightcloud.jenkins.Constants

def call(isDeploy = "true") {
//    def kubectl = getKubectl(k8sEnv)

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
                choice(name: "BUILD_THIRD_PARTY_MODULES", choices: 'true\nfalse', defaultValue: "$params.BUILD_THIRD_PARTY_MODULES", description: '是否打包third-party模块'),
        ])
        ])
        def namespace = env.NAMESPACE_INJECT
        def kubectl = getKubectl(getK8sEnv(namespace))

        try {
            // tag_name
            def branchname = "${PARAM_GIT_TAGS}"
            def image_name = "image.rightcloud.com.cn/${namespace}/rightcloud-console:v.${branchname}.${env.BUILD_NUMBER}"

            stage("build") {
                // 获取Dockerfile
                parallel(
                        failFast: true,
                        "clone console": {
                            dir('/home/jenkins/workspace/rightcloud-console') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }
                        },
                        "clone doc": {
                            dir('/home/jenkins/workspace/rightcloud-doc') {
                                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-v4/rightcloud-doc.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                sh 'rm -rf .git && rm -rf .idea'
                            }

                        },
                        "clone modules": {
                            if (params.BUILD_THIRD_PARTY_MODULES == 'true') {
                                println('clone backup module')
                                dir('/home/jenkins/workspace/rightcloud-console-cloudbackup') {
                                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-backup/rightcloud-console-cloudbackup.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                    sh 'rm -rf .git && rm -rf .idea'
                                }
                                println('clone container module')
                                dir('/home/jenkins/workspace/rightcloud-console-container') {
                                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/rightcloud-modules/cloud-container/rightcloud-console-cloudcontainer.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branchname]]], poll: false
                                    sh 'rm -rf .git && rm -rf .idea'
                                }
                            }
                        }
                )
//                def third_party_build_args = "";
//                stage('prepare dockerfile') {
//                    def docker_file_template = '/home/jenkins/workspace/rightcloud-console/docker/Dockerfile.template'
//                    def docker_file_content = readFile(file: docker_file_template)
//                    def replace_content_1 = ''
//                    def replace_content_2 = ''
//                    def replace_content_3 = ' && ls '
//                    def replace_content_4 = ''
//
//                    if (params.BUILD_THIRD_PARTY_MODULES == 'true') {
//                        // 云备份部分
//                        replace_content_1 += "ARG BACKUP_PROJECT\n"
//                        replace_content_2 += 'COPY ${BACKUP_PROJECT} /user/share/backup/ \n'
//                        replace_content_3 += '\\ \n && cd /user/share/backup/ && cnpm install && npm run build & '
//                        replace_content_4 += 'COPY --from=builder /user/share/backup/dist/ /usr/share/nginx/html/plugin/backup\n'
//                        third_party_build_args += " --build-arg BACKUP_PROJECT=./rightcloud-console-cloudbackup "
//                        // 云容器部分
//                        replace_content_1 += "ARG CONTAINER_PROJECT\n"
//                        replace_content_2 += 'COPY ${CONTAINER_PROJECT} /user/share/container/ \n'
//                        replace_content_3 += '\\ \n  cd /user/share/container/ && cnpm install && npm run build & '
//                        replace_content_4 += 'COPY --from=builder /user/share/container/dist/ /usr/share/nginx/html/plugin/container\n'
//                        third_party_build_args += " --build-arg CONTAINER_PROJECT=./rightcloud-console-container "
//
//                        replace_content_3 += ' wait && echo "done" '
//                    }
//                    docker_file_content = docker_file_content.replace("#hook1", replace_content_1)
//                    docker_file_content = docker_file_content.replace("#hook2", replace_content_2)
//                    docker_file_content = docker_file_content.replace("#hook3", replace_content_3)
//                    docker_file_content = docker_file_content.replace("#hook4", replace_content_4)
//                    println(docker_file_content)
//                    writeFile(file: docker_file_template, text: docker_file_content)
//                }
                stage('build console image') {
                    // 准备Dockerfile所需的--build-arg参数
                    def third_party_build_args = " --build-arg CONTAINER_PROJECT=./rightcloud-console-container " +
                            " --build-arg BACKUP_PROJECT=./rightcloud-console-cloudbackup "
                    def build_args = " --build-arg CONSOLE_PROJECT=./rightcloud-console " +
                            " --build-arg DOC_PROJECT=./rightcloud-doc " + third_party_build_args
                    container('docker') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            dir('/home/jenkins/workspace') {
                                sh "docker build -t ${image_name} --no-cache -f rightcloud-console/docker/Dockerfile --no-cache . ${build_args}"
                                sh "docker push ${image_name}"
                            }
                        }
                    }
                }
            }
            if (isDeploy) {
                stageK8ServiceDeploy("console", namespace, kubectl, image_name, false)
            }
            notifyDingDing(true, namespace, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            //     }
            // )
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
