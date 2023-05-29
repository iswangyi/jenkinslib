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
    def projectpath = splited[4..-1].join('/')
    echo "projectpath is: ${projectpath}"
    def projectfinalServer = projectpath
    def projectfinal = projectfinalServer.replace('.git', '')
    echo "projectfinal is: ${projectfinal}"

    def namespace
    if (!defaultnamespace.trim()) {
        namespace = splited[-2] + "-project"
    } else {
        namespace = defaultnamespace
    }

    echo "namespace is: ${namespace}"

    node {
//        def dbmigrationtagname
        def servertagname
//        def adaptertagname

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

            string(name: "NAMESPACE", defaultValue: "$namespace", description: "namespace for build"),

            string(name: "VERSION_PREFIX", defaultValue: "3.2.0", description: "版本号前缀"),
        ])
        ])

        def mail;
        def dockerserver = getDockerBuildServer()
        try {
            if (!namespace.trim()) {
                error("namespace is null or empty, nothing to do")
            }

            stage('Prepare source code') {
                container('docker') {
                    branchname = "${PARAM_GIT_TAGS}"
                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: scm.userRemoteConfigs, branches: [[name: "${PARAM_GIT_TAGS}"]]], poll: false

                    echo "use branch for build: ${branchname}"
                    mail = sh (
                        script: 'git --no-pager show -s --format=\'%ae\'',
                        returnStdout: true
                    ).trim();
                }
            }

            /*stage('Build server') {
                compileServer()
            }*/


            servertagname = stageBuildServerImage(dockerserver,
                                                  projectfinal,
                                                  branchname.split('/')[-1],
                                                  params.NAMESPACE,
                                                  istag, null, false)

            container('kubectl') {
                // def mysqlport=getMysqlPort(Constants.PRODUCT_K8S_SERVER,
                //                            params.NAMESPACE)
                // if (!mysqlport.trim()) {
                //     error("failed to get mysql port from ${params.NAMESPACE} @ ${Constants.PRODUCT_K8S_SERVER}")
                // }

                if (!params.VERSION_PREFIX.trim()) {
                    error("version prefix is empty")
                }

                echo "updating version information"
                def versioninfo=params.VERSION_PREFIX.trim()
                def k8sserver="${Constants.PRODUCT_K8S_SERVER}"
                def updateversionstdout = sh (
                    script: "datetime=`TZ=Asia/Shanghai date +%Y%m%d%H%M%S`;mysqlpwd=\$(kubectl -s ${k8sserver} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' '); mysqluser=\$(kubectl -s ${k8sserver} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' '); podname=\$(kubectl -s ${k8sserver} -n ${namespace} get pod |grep rightcloud-mysql | grep Running | awk '{print \$1}'); kubectl -s ${k8sserver} -n ${namespace} exec -it \${podname} -- sh -c \"exec mysql -h127.0.0.1 -u\${mysqluser} -p\${mysqlpwd} rightcloud -e \\\"update sys_m_config set config_value='${versioninfo}-\${datetime}' where config_key='system.version';\\\"\"",
                    returnStdout: true
                ).trim();
                echo "update version stdout: ${updateversionstdout}"

                // inject version info

                stageK8SRunServer(Constants.PRODUCT_K8S_SERVER,
                                  params.NAMESPACE,
                                  servertagname, true)
            }

        } catch (e) {
            println(e.getMessage());
            println(e.getStackTrace());
            //notifyFailed(mail)
            error("Failed build as " + e.getMessage())
        }
    }

}
