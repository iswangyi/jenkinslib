import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(defaultnamespace="") {
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
        def viclienttagname

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

            string(name: "NAMESPACE", defaultValue: "${namespace}", description: "namespace for build"),
        ])
        ])
        
        def dockerserver = getDockerBuildServer()

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

            /*compileSchedule()*/
            viclienttagname = stageBuildViClientImage(dockerserver,
                                                      projectfinal,
                                                      branchname,
                                                      params.NAMESPACE,
                                                      istag, null, false)
            container('kubectl') {
                stageK8SRunViClient(Constants.PRODUCT_K8S_SERVER,
                                    params.NAMESPACE,
                                    viclienttagname, true)
            }
        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
