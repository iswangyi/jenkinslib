import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name
}

def call() {
node {
    def analyzerTagname

    def branchname = getGitBranchName()
    //def PARAM_GIT_TAGS = "release-test3.0";

    properties([parameters([
        [$class: 'GitParameterDefinition',
         name: 'PARAM_GIT_TAGS',
         description: ' tags',
         type:'PT_TAG',
         branch: '',
         branchFilter: '',
         tagFilter:'',
         sortMode:'DESCENDING_SMART',
         defaultValue: 'master',
         selectedValue:'NONE',
         quickFilterEnabled: true],

        string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://test.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
        string(name: "NAMESPACE", defaultValue: "test", description: "namespace for build"),
    ])
    ])

    def mail;
    try {
        if (!params.NAMESPACE?.trim()) {
            error("namespace is null or empty, nothing to do")
        }

        stage('Prepare source code') {

            echo "use branch for build: ${PARAM_GIT_TAGS}"
            if (branchname.contains("test")) {
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false
            } else {
                checkout scm;
            }

            mail = sh (
                script: 'git --no-pager show -s --format=\'%ae\'',
                returnStdout: true
            ).trim();
        }

        analyzerTagname = stageBuildAnalyzerImage(params.DOCKER_BUILD_SERVER,
                                                "cloudstar/saas/rightcloud-analyzer",
                                                "$PARAM_GIT_TAGS",
                                                params.NAMESPACE,
                                                true, null, false)

        container('kubectl') {
                stageK8SRunAnalyzer(Constants.PRODUCT_K8S_SERVER,
                                   params.NAMESPACE, analyzerTagname, true)
        }
    } catch (e) {
        println(e.getMessage());
        println(e.getStackTrace());
        //notifyFailed(mail)
        error("Failed build as " + e.getMessage())
    }
}

}
