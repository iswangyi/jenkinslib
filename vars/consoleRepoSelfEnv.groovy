import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            string(name: "EMAIL_ADDRESS", defaultValue: '.*@cloud-star.com.cn',
                   description: "email addrees to indentify user for build"),
            string(name: "WAIT_TIME", defaultValue: '300',
                   description: "default wait time if test env is running"),
            string(name: "MONGODB_USERNAME", defaultValue: Constants.SELF_ENV_MONGO_USER, description: "MONGODB user"),
            string(name: "MONGODB_PASSWORD", defaultValue: Constants.SELF_ENV_MONGO_PASS, description: "MONGODB password"),
            string(name: "SCHEDULE_EMAIL_ADDRESS", defaultValue: '.*@cloud-star.com.cn',
                   description: "email addrees to run schedule"),
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
            def info = stageGetSelfEnvInfo()
            (mail, username, server, port, branch, MONGODB_HOST, MONGODB_PORT,
             REDIS_PORT, MQ_PORT, MYSQL_PORT, HTTP_PORT) = info

            def dockerserver = "${server}:2375"

            stageIsSelfEnvRunning(dockerserver,
                                  username,
                                  "console")

            stageServerAdapterSelfEnvNew(dockerserver,
                                      server, port,
                                      server, MYSQL_PORT,
                                      server, "${server}:3307",
                                      server, MQ_PORT,
                                      server, REDIS_PORT,
                                      server, server,
                                      MONGODB_HOST, MONGODB_PORT, "${params.MONGODB_USER}", "${params.MONGODB_PASSWORD}",
                                      branch, username, true)


            stageConsoleSelfEnv(dockerserver, HTTP_PORT,
                                server, port, username, branchname, false)
            // stageScheduleSelfEnv(dockerserver,
            //                      server, MYSQL_PORT,
            //                      server, REDIS_PORT,
            //                      server, MQ_PORT,
            //                      username,
            //                      branchname, true)


            notifySelfEnvSuccess(mail: mail, serverinfo: "http://${server}:${HTTP_PORT}")

        }catch (e) {
            println(e);
            println(e.getMessage());
            println(e.getStackTrace());
            notifyFailed(mail)
            error(e.getMessage())
        }

    }


}
