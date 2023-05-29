import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call() {
    def branchname = getGitBranchName()
    def istag = true
    node {
        def scheduletagname

        //def PARAM_GIT_TAGS = "release-test3.0";

        if (branchname.contains("test")) {
            properties([parameters([
                [$class: 'GitParameterDefinition',
                 name: 'PARAM_GIT_TAGS',
                 description: ' tags',
                 type:'PT_TAG',
                 branch: '',
                 branchFilter: '.*/release-test.*',
                 tagFilter:'release-test*',
                 sortMode:'DESCENDING_SMART',
                 defaultValue: 'release-test',
                 selectedValue:'NONE',
                 quickFilterEnabled: true],

                string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://dev.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
                string(name: "NAMESPACE", defaultValue: "ceis-project", description: "namespace for build"),
            ])
            ])
        } else {
            properties([parameters([
                string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://dev.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
                string(name: "NAMESPACE", defaultValue: "", description: "namespace for build"),
            ])
            ])
        }

        def mail;
        try {
            if (!params.NAMESPACE?.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            stage('Prepare source code') {

                if (branchname.contains("test")) {
                    branchname = "${PARAM_GIT_TAGS}"
                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: branchname]]], poll: false
                } else {
                    istag = false
                    checkout scm;
                }

                echo "use branch for build: ${branchname}"

                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            /*compileSchedule()*/
            scheduletagname = stageBuildScheduleImage(params.DOCKER_BUILD_SERVER,
                                                      "ProjectGroup/CEIS/rightcloud-schedule",
                                                      branchname,
                                                      params.NAMESPACE,
                                                      istag, null, false)
            container('kubectl') {
                stageK8SRunSchedule(Constants.PRODUCT_K8S_SERVER,
                                    params.NAMESPACE, scheduletagname, true)
            }
        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
