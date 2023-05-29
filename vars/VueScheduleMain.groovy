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
        def scheduletagname

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
                    "build & deploy": {
                        /*compileSchedule()*/
                        scheduletagname = stageBuildScheduleImage(dockerserver,
                                projectfinal,
                                branchname.split('/')[-1],
                                params.NAMESPACE,
                                istag, null, false)
                        if (isDeploy) {
                            container('kubectl') {
                                vueDeploySchedule(Constants.PRODUCT_K8S_SERVER,
                                        params.NAMESPACE,
                                        scheduletagname, true)
                            }
                        }
                        notifyDingDing(true, Constants.TEST_ENV, Constants.BUILD_SUCCESS_MESSAGE, Constants.BUILD_NOTIFY_PEOPEL)
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
