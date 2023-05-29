import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call() {
    def istag = true

    node {
        def tagname

        //def PARAM_GIT_TAGS = "release-test3.0";

        properties([parameters([
            [$class: 'GitParameterDefinition',
             name: 'PARAM_GIT_TAGS',
             description: ' tags',
             type:'PT_TAG',
             branch: '',
             branchFilter: '.*/release-test.*',
             tagFilter:'*',
             sortMode:'DESCENDING_SMART',
             defaultValue: 'release-test3.0',
             selectedValue:'NONE',
             quickFilterEnabled: true],

        ])
        ])
        
        def mail;
        try {
            stage('Prepare source code') {

                branchname = "${PARAM_GIT_TAGS}"
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false

                echo "use branch/tag for build: ${branchname}"

                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            stageBuildServerImage("", "cloudstar/rightcloud",
                                  branchname,
                                  "rightcloud",
                                  true,
                                  "v.ph.1.${BRANCH_OR_TAG_NAME}.")
          } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
