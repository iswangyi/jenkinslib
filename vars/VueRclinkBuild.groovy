import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(defaultnamespace="", isDeploy=true) {
    def branchname = getGitBranchName()
    def istag = true

    def scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
    def splited = scmUrl.split('/')
    echo "scmUrl is: ${scmUrl}"
    echo "splited is: ${splited}"
    def projectpath = splited[3..-1].join('/')
    echo "projectpath is: ${projectpath}"
    def projectfinal = projectpath.replace('.git', '')
    echo "projectfinal is: ${projectfinal}"

    def namespace
    if (!defaultnamespace.trim()) {
        namespace = splited[-2] + "-project"
    } else {
        namespace = defaultnamespace
    }

    node {
        def rclinktagname

        //def PARAM_GIT_TAGS = "release-test3.0";

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

            string(name: "NAMESPACE", defaultValue: "${namespace}", description: "namespace for build"),
        ])
        ])

        def dockerserver;

        def mail;
        try {
            if (!namespace.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            stage('Prepare source code') {

                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false

                echo "use branch for build: ${branchname}"

                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            parallel (
                    "sonarQube task": {
                        stageCodeQualityAnalysis()
                    },
                    "build image": {
                        rclinktagname = stageBuildRclinkImage(dockerserver,
                                projectfinal,
                                branchname.split('/')[-1],
                                params.NAMESPACE,
                                istag, null, false)

                        stage('tar zcf packing') {
                            container('docker') {
                                sh "docker pull ${rclinktagname}"
                                sh "docker save  ${rclinktagname} > rclink.tar"
                                sh "tar zcf rclink.tgz rclink.tar"
                                // 放入pod共享存储empty dir(/home/kubectl-admin)
                                sh 'mkdir -p /home/kubectl-admin/rclink && mv ./rclink.tgz /home/kubectl-admin/rclink/'
                            }
                        }

                        def vue_nfs_server = '10.67.7.18'
                        stage('send tar package to vue server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${vue_nfs_server} uname -a"
                                sh "ls /home/kubectl-admin/rclink/"
                                sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${vue_nfs_server}:/mnt/sdb/nfs/vue-project/files/"
                            }
                        }

                        // def demo_nfs_server = '10.68.12.4'
                        def demo_nfs_server = '10.68.12.16'
                        stage('send tar package to demo server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${demo_nfs_server} uname -a"
                                sh "ls /home/kubectl-admin/rclink/"
                                // sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${demo_nfs_server}:/mnt/nfs/rightcloud-standard/files/"
                                sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${demo_nfs_server}:/usr/local/rightcloud/nfs/files/"
                            }
                        }

                        stage('send tar package to master server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${vue_nfs_server} cp /mnt/sdb/nfs/vue-project/files/rclink.tgz /mnt/sdb/nfs/rightcloud-master/files/"
                            }
                        }

                        def saas_server = '10.68.12.12'
                        stage('send tar package to saas server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${saas_server} uname -a"
                                sh "ls /home/kubectl-admin/rclink/"
                                sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${saas_server}:/usr/local/rightcloud/data/files/"
                            }
                        }

                        def ha_server = '10.69.0.138'
                        stage('send tar package to saas server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${ha_server} uname -a"
                                sh "ls /home/kubectl-admin/rclink/"
                                sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${ha_server}:/var/nfsshare/files/"
                            }
                        }

                        def ha_pre_server = '10.69.1.134'
                        stage('send tar package to pre_demo ha server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${ha_pre_server} uname -a"
                                sh "ls /home/kubectl-admin/rclink/"
                                sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${ha_pre_server}:/var/nfsshare/files/"
                            }
                        }

                        def demo_bak_server = '139.9.5.132'
                        stage('send tar package to huawei cloud demo_bak server') {
                            sshagent(credentials: ['global-ssh-key-czc']) {
                                sh "ssh -o StrictHostKeyChecking=no -l root ${demo_bak_server} uname -a || true"
                                sh "ls /home/kubectl-admin/rclink/ || true"
                                sh "scp -r /home/kubectl-admin/rclink/rclink.tgz root@${demo_bak_server}:/usr/local/rightcloud/data/files/ || true"
                            }
                        }
                    }

            )
        }   catch (e) {
            def errorMessage = e.getMessage()
            println(errorMessage)
            println("current result:" + currentBuild.result)
//            notifyFailed(mail)
            if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
                println("FAILURE send message info")
                notifyDingDing(false, Constants.TEST_ENV, Constants.BUILD_FAILURE_MESSAGE + "\n" + errorMessage, Constants.BUILD_NOTIFY_PEOPEL)
            } else {
                println("ABORTED not send info")
            }
            error("Failed build as " + e.getMessage())
        }finally{
            def currentResult = currentBuild.result ?: 'SUCCESS'
            // 不稳定发送消息
            if (currentResult == 'UNSTABLE') {
                notifyDingDing(true, Constants.TEST_ENV, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            }
        }
    }

}
