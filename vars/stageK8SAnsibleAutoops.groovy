import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name
}

def call() {
node (){
    def ansibletagname

    def branchname = getGitBranchName()
    //def PARAM_GIT_TAGS = "release-test3.0";

    properties([parameters([
        [$class: 'GitParameterDefinition',
         name: 'PARAM_GIT_TAGS',
         description: ' tags',
         type:'PT_BRANCH_TAG',
         branch: '',
         branchFilter: '.*',
         tagFilter:'.*',
         sortMode:'DESCENDING_SMART',
         defaultValue: 'feature-autoOps',
         selectedValue:'NONE',
         quickFilterEnabled: true],

        string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://test.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
        string(name: "NAMESPACE", defaultValue: "autoops", description: "namespace for build"),
    ])
    ])

    def mail;
    try {
        if (!params.NAMESPACE?.trim()) {
            error("namespace is null or empty, nothing to do")
        }

        stage('Prepare source code') {

            echo "use branch for build: ${PARAM_GIT_TAGS}"
            if (branchname.contains("autoOps")) {
                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false
            } else {
                checkout scm;
            }

            mail = sh (
                script: 'git --no-pager show -s --format=\'%ae\'',
                returnStdout: true
            ).trim();
        }

        ansibletagname = stageBuildAnsibleImage(params.DOCKER_BUILD_SERVER,
                                                "autoops/ansible-adapter",
                                                branchname.split('/')[-1],
                                                params.NAMESPACE,
                                                true, null, false)

        container('kubectl') {
                stageK8SRunAnsible(Constants.PRODUCT_K8S_SERVER,
                                   params.NAMESPACE, ansibletagname, true)
        }
    } catch (e) {
        println(e.getMessage());
        println(e.getStackTrace());
        //notifyFailed(mail)
        error("Failed build as " + e.getMessage())
    }
}

}
