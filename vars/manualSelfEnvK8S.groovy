import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            string(name: "USER_NAME", description: "user name for building self env"),
            string(name: "CONSOLE_BRANCH", description: "branch of console"),
            booleanParam(defaultValue: true, description: 'build console?', name: 'IS_BUILD_CONSOLE'),
            string(name: "SERVER_BRANCH", description: "branch of server"),
            booleanParam(defaultValue: true, description: 'build server?', name: 'IS_BUILD_SERVER'),
            string(name: "ADAPTER_BRANCH", description: "branch of adapter"),
            booleanParam(defaultValue: true, description: 'build adapter?', name: 'IS_BUILD_ADAPTER'),
            string(name: "SCHEDULE_BRANCH", description: "branch of schedule"),
            booleanParam(defaultValue: true, description: 'build schedule?', name: 'IS_BUILD_SCHEDULE'),
            string(name: "ANSIBLE_BRANCH", description: "branch of ansible"),
            booleanParam(defaultValue: true, description: 'build ansible?', name: 'IS_BUILD_ANSIBLE'),
            string(name: "GROUP_NAME", defaultValue: "cloudstar",description: "构建的group名称,如果是项目构建航天科工则填写如下：ProjectGroup/htkg"),

        ])])


        def consolebranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.CONSOLE_BRANCH) {
            consolebranch  = params.CONSOLE_BRANCH.trim()
        }

        def serverbranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.SERVER_BRANCH) {
            serverbranch = params.SERVER_BRANCH.trim()
        }

        def adapterbranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.ADAPTER_BRANCH) {
            adapterbranch = params.ADAPTER_BRANCH.trim()
        }


        def schedulebranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.SCHEDULE_BRANCH) {
            schedulebranch = params.SCHEDULE_BRANCH.trim()
        }

        def ansiblebranch = "test"
        if (params.ANSIBLE_BRANCH) {
            ansiblebranch = params.ANSIBLE_BRANCH.trim()
        }

        def groupName = "cloudstar"
        if (params.GROUP_NAME) {
            groupName = params.GROUP_NAME.trim()
        }


        def mail = "${params.USER_NAME}@cloud-star.com.cn";
        def username = params.USER_NAME;

        try {
            if (params.IS_BUILD_ADAPTER) {
                stageAdapterSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                       adapterbranch, username, true, groupName)
            }

            if (params.IS_BUILD_SERVER) {
                stageServerSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                      serverbranch, username, true, groupName)
            }

            if (params.IS_BUILD_CONSOLE) {
                stageConsoleSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                       username, consolebranch, true,groupName)
            }

            if (params.IS_BUILD_SCHEDULE) {
                stageScheduleSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                        username, schedulebranch, true,groupName)
            }

            if (params.IS_BUILD_ANSIBLE) {
                stageAnsibleSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                        username, ansiblebranch, true)
            }

            notifySelfEnvSuccessK8S(mail: mail, serverinfo: "http://${username}.rctest.com", username: username)

        }catch (e) {
            println(e);
            println(e.getMessage());
            println(e.getStackTrace());
            notifyFailed(mail)
            error(e.getMessage())
        }

    }


}
