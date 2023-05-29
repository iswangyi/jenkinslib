import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call() {
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

    node {
        def dbmigrationtagname
        def servertagname
        def adaptertagname

        //def PARAM_GIT_TAGS = "release-test3.0";

        if (branchname.contains("autoOps")) {
            properties([parameters([
                [$class: 'GitParameterDefinition',
                 name: 'PARAM_GIT_TAGS',
                 description: ' branch tags',
                 type:'PT_BRANCH_TAG',
                 branch: '',
                 branchFilter: '.*',
                 tagFilter:'*',
                 sortMode:'DESCENDING_SMART',
                 defaultValue: 'feature-autoOps',
                 selectedValue:'NONE',
                 quickFilterEnabled: true],

                string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://dev.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
                string(name: "NAMESPACE", defaultValue: "autoops", description: "namespace for build"),

            ])
            ])
        } else {
            properties([parameters([
                string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://dev.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
                string(name: "NAMESPACE", defaultValue: "autoops", description: "namespace for build"),
            ])
            ])
        }

        def mail;
        def dockerserver = getDockerBuildServer()
        try {
            if (!params.NAMESPACE?.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            stage('Prepare source code') {
                if (branchname.contains("autoOps")) {
                    branchname = "${PARAM_GIT_TAGS}"
                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false
                } else {
                    checkout scm;
                    istag = false
                }

                echo "use branch for build: ${branchname}"
                mail = sh (
                    script: 'git --no-pager show -s --format=\'%ae\'',
                    returnStdout: true
                ).trim();
            }

            dbmigrationtagname = stageCreateDBMigration(dockerserver,
                                                        projectfinal,
                                                        branchname.split('/')[-1],
                                                        params.NAMESPACE,
                                                        istag, null, false)

            servertagname = stageBuildServerImage(dockerserver,
                                                  projectfinal,
                                                  branchname.split('/')[-1],
                                                  params.NAMESPACE,
                                                  istag, null, false)

         /*   adaptertagname = stageBuildAdapterImage(params.DOCKER_BUILD_SERVER,
                                                    projectfinal,
                                                    branchname.split('/')[-1],
                                                    params.NAMESPACE,
                                                    istag, null, false)*/

            container('kubectl') {
                stageK8SRunServer(Constants.PRODUCT_K8S_SERVER,
                                  params.NAMESPACE,
                                  dbmigrationtagname,
                                  servertagname, true)
                /*stageK8SRunAdapter(Constants.PRODUCT_K8S_SERVER,
                                   params.NAMESPACE,
                                   adaptertagname, true)*/
            }

        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
