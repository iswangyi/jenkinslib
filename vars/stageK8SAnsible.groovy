import cn.com.rightcloud.jenkins.Constants

def getGitBranchName() {
    return scm.branches[0].name
}

def call() {
node {
        def ansibletagname

    def branchname = getGitBranchName()
    //def PARAM_GIT_TAGS = "release-test3.0";

    if (branchname.contains("dev")) {
        properties([parameters([
            string(name: "ANYNODE_PREFIX", defaultValue: 'rightcloud', description: "docker container name prefix"),
            string(name: "SERVER_PORT", defaultValue: "8444", description: "server port to expose"),
            string(name: "SERVER_PORT_H", defaultValue: "8080", description: "server port to expose"),
            string(name: "HOST_IP", defaultValue: "dev.rd.rightcloud.com.cn", description: "Host IP for running"),
            string(name: "DB_PORT", defaultValue: "3306", description: "mysql port to expose"),
            string(name: "REDIS_PORT", defaultValue: "6379", description: "redis port"),
            string(name: "MQ_PORT", defaultValue: "5672", description: "rabbitmsq port"),
            string(name: "CONSOLE_PORT", defaultValue: "8445", description: "console port"),
            string(name: "FALCON_DB_URL", defaultValue: "test.rd.rightcloud.com.cn:3306", description: "openfalcon database url"),
            string(name: "FALCON_DB_USER", defaultValue: "rightcloud", description: "openfalcon database username"),
            string(name: "FALCON_DB_PASSWORD", defaultValue: "H89lBgAg", description: "openfalcon database password"),
            string(name: "FALCON_IP", defaultValue: "test.rd.rightcloud.com.cn", description: "openfalcon database url"),
            string(name: "FALCON_PORT", defaultValue: "6030", description: "openfalcon port"),
            string(name: "FALCON_HTTP_PORT", defaultValue: "6060", description: "openfalcon transfer http port"),
            string(name: "REGISTRY", defaultValue: "image.rightcloud.com.cn/rightcloud/rightcloud-server", description: "server docker image name"),
            string(name: "REGISTRY_ADAPTER", defaultValue: "image.rightcloud.com.cn/rightcloud/rightcloud-adapter", description: "adapter docker image name"),
            string(name: "TAGPREFIX", defaultValue: "v.d.1.", description: "default tag prefix"),
            string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://dev.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
            string(name: "DOCKER_SERVER", defaultValue: "tcp://dev.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
            string(name: "COOKIE_DOMAIN", defaultValue: "dev.rd.rightcloud.com.cn", description: "cookie domain for local storage"),
            string(name: "MONGODB_HOST", defaultValue: "dev.rd.rightcloud.com.cn", description: "MONGODB  host"),
            string(name: "MONGODB_USERNAME", defaultValue: "rightcloud", description: "MONGODB user"),
            string(name: "MONGODB_PASSWORD", defaultValue: "H89lBgAg", description: "MONGODB password"),
            booleanParam(defaultValue: false, description: 'do auto test?', name: 'AUTO_TEST'),
        ])
        ])
    } else if (branchname.contains("test")) {
        properties([parameters([
            [$class: 'GitParameterDefinition',
             name: 'PARAM_GIT_TAGS',
             description: ' tags',
             type:'PT_TAG',
             branch: '',
             branchFilter: '.*/release-.*',
             tagFilter:'release-*',
             sortMode:'DESCENDING_SMART',
             defaultValue: 'release-test3.0',
             selectedValue:'NONE',
             quickFilterEnabled: true],

            string(name: "DOCKER_BUILD_SERVER", defaultValue: "tcp://test.rd.rightcloud.com.cn:2375", description: "Docker server used to build the image"),
            string(name: "NAMESPACE", defaultValue: "test", description: "namespace for build"),
        ])
        ])
    } 

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

        ansibletagname = stageBuildAnsibleImage(params.DOCKER_BUILD_SERVER,
                                                "rightcloud/ansible-adapter",
                                                "$PARAM_GIT_TAGS",
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
