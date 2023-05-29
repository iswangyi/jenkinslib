import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

/**
 * build console test env on k8s, vue version
 *
 * @param group console group, cloudstart/, ProjectGroup/GABigData/...
 * @param repo console repo rightcloud-console, gabigdata-console...
 * @param k8sserver k8s server for running the console pod
 */

def call(group, project, k8sserver, namespace) {
    def branchname = getGitBranchName()
    def istag = true
    node {
        def portaltagname

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

        def mail;
        try {
            if (!params.NAMESPACE?.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            // 下载kubectl认证相关配置和证书，放入pod共享存储empty dir(/home/kubectl-admin)
            // pod共享存储是在jenkins系统配置下的cloud下的pod template中配置好的
            stage('git kubectl config and cert') {
                git credentialsId: 'maxiao_gitlab', url: 'http://10.68.6.20:8082/mx/kubectl-admin.git', branch: "master"
                sh 'mv ./* /home/kubectl-admin/'
            }

            stage('Prepare source code') {
                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false

                echo "use branch for build: ${branchname}"

                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            portaltagname = stageBuildPortalImageVue(group,
                                                       project,
                                                       branchname.split('/')[-1],
                                                       params.NAMESPACE,
                                                       istag, null, false)

            container('kubectl') {
                stageK8SRunPortal(k8sserver,
                                  params.NAMESPACE, portaltagname, true)
            }
        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
