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

def call(group, project, k8sserver, namespace, isDeploy=true) {
    def branchname = getGitBranchName()
    def istag = true
    if (!namespace?.trim()) {
        namespace = "gabigdata-project"
    }
    node {
        def consoletagname

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

            stage('Prepare source code') {

                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false

                echo "use branch for build: ${branchname}"

                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            consoletagname = stageBuildConsoleImageVue(group,
                                                       project,
                                                       branchname.split('/')[-1],
                                                       params.NAMESPACE,
                                                       istag, null, false)

            if (isDeploy) {
                container('kubectl') {
                    vueDeployConsole(k8sserver,
                                   params.NAMESPACE, consoletagname, true)
                }
            }
            notifyDingDing(true, Constants.TEST_ENV, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
            
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
