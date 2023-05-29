import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name.split('/')[-1]
}

def call(group, project, k8sserver, namespace) {
    def branchname = getGitBranchName()
    def istag = true
    if (!namespace?.trim()) {
        namespace = "vue-project"
    }

    node {
        def openapitagname

        //def PARAM_GIT_TAGS = "release-test3.0";

        properties([parameters([
            [$class: 'GitParameterDefinition',
             name: 'PARAM_GIT_TAGS',
             description: ' tags',
             type:'PT_BRANCH_TAG',
             branch: '',
             branchFilter: '*',
             tagFilter:'*',
             sortMode:'DESCENDING_SMART',
             defaultValue: 'master',
             selectedValue:'NONE',
             quickFilterEnabled: true],

            string(name: "NAMESPACE", defaultValue: "$namespace", description: "namespace for build"),

        ])
        ])


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

            openapitagname = stageBuildOpenAPIImage(group,
                                                    project,
                                                    branchname.split('/')[-1],
                                                    params.NAMESPACE,
                                                    istag, null, false)

            container('kubectl') {
                stageK8SRunOpenAPI(k8sserver,
                                   params.NAMESPACE,
                                   openapitagname, true)
            }
        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
