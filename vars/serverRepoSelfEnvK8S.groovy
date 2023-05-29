import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            string(name: "WAIT_TIME", defaultValue: '300',
                   description: "default wait time if test env is running"),
         ])])

        echo "starting....."
        def branchname = "${BRANCH_NAME}";
        def mail;
        def username;

        def server;
        def port;
        def branch = BRANCH_NAME;
        def MONGODB_HOST
        def MANGODB_PORT
        def REDIS_PORT
        def MQ_PORT
        def MYSQL_PORT
        def HTTP_PORT

        try {
            def info = getGitCommitterInfo()
            (mail, username) = info

            stageIsSelfEnvRunningK8S(Constants.SELFENV_K8S_SERVER, username,
                                     "rightcloud-server")


            stageServerAdapterSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                         branch, username, false, 'cloudstar')

            // if (!isPodRunningOnK8S(Constants.SELFENV_K8S_SERVER,
            //                       username, "rightcloud-console")) {
                stageConsoleSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                       username, branch, true,'cloudstar')
           // }

            if (!isPodRunningOnK8S(Constants.SELFENV_K8S_SERVER,
                                  username, "rightcloud-schedule")) {
                stageScheduleSelfEnvK8S(Constants.SELFENV_K8S_SERVER,
                                        username, branch, true,'cloudstar')
            }

//            stageScheduleSelfEnv(dockerserver,
//                                 server, MYSQL_PORT,
//                                 server, REDIS_PORT,
//                                 server, MQ_PORT,
//                                 username,
//                                 branchname, true)


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
