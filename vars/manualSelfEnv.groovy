import cn.com.rightcloud.jenkins.Constants

def call() {
    node {
        properties([parameters([
            string(name: "USER_NAME", description: "user name for building self env"),
            string(name: "CONSOLE_BRANCH", description: "branch of console"),
            booleanParam(defaultValue: true, description: 'build console?', name: 'IS_BUILD_CONSOLE'),
            string(name: "SERVER_BRANCH", description: "branch of server"),
            booleanParam(defaultValue: true, description: 'build server/adapter?', name: 'IS_BUILD_SERVER'),
            string(name: "SCHEDULE_BRANCH", description: "branch of schedule"),
            booleanParam(defaultValue: true, description: 'build schedule?', name: 'IS_BUILD_SCHEDULE'),
        ])])


        def consolebranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.CONSOLE_BRANCH) {
            consolebranch  = params.CONSOLE_BRANCH.trim()
        }

        def serverbranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.SERVER_BRANCH) {
            serverbranch = params.SERVER_BRANCH.trim()
        }

        def schedulebranch = Constants.DEFAULT_SELF_ENV_BRANCH
        if (params.SCHEDULE_BRANCH) {
            schedulebranch = params.SCHEDULE_BRANCH.trim()
        }

        def mail;
        def username;

        def server;
        def port;
        def MONGODB_HOST
        def MANGODB_PORT
        def REDIS_PORT
        def MQ_PORT
        def MYSQL_PORT
        def HTTP_PORT

        try {
            def info = getUserSelfEnvInfo(params.USER_NAME.trim())
            (mail, username, server, port, MONGODB_HOST, MONGODB_PORT,
             REDIS_PORT, MQ_PORT, MYSQL_PORT, HTTP_PORT) = info

            def dockerserver = "${server}:2375"

            if (params.IS_BUILD_SERVER) {
                stageServerAdapterSelfEnvNew(dockerserver,
                                             server, port,
                                             server, MYSQL_PORT,
                                             server, "${server}:3307",
                                             server, MQ_PORT,
                                             server, REDIS_PORT,
                                             server, server,
                                             MONGODB_HOST, MONGODB_PORT,
                                             Constants.SELF_ENV_MONGO_USER,
                                             Constants.SELF_ENV_MONGO_PASS,
                                             serverbranch, username, true)
            }

            if (params.IS_BUILD_CONSOLE) {
                stageConsoleSelfEnv(dockerserver, HTTP_PORT,
                                    server, port, username,
                                    consolebranch, true)
            }

            if (params.IS_BUILD_SCHEDULE) {
                stageScheduleSelfEnvNew(dockerserver,
                                        server, MYSQL_PORT,
                                        server, REDIS_PORT,
                                        server, MQ_PORT,
                                        username,
                                        schedulebranch, true)
            }

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
