import cn.com.rightcloud.jenkins.Constants

def call() {
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

    stage('Prepare source code') {
        checkout scm;

        branch = "${env.BRANCH_NAME}";

        committerEmail = sh (
            script: 'git --no-pager show -s --format=\'%ae\'',
            returnStdout: true
        ).trim();

        mail = committerEmail;
        echo "commit mail: ${mail}";
        // def matcher = mail =~ /@.*/
        username = mail.replaceAll("@.*", "");
        username = username.replaceAll(/\./, "");
        username = username.replaceAll(/-/, "_");
        echo "username is ${username}";

        def match = true
        if (! match) {
            echo "match result: ${match}"
            echo "${mail} is not allow for build right now."
            currentBuild.result = 'ABORTED'
            build.getExecutor().interrupt()
            build.getExecutor().interrupt(Result.ABORTED)

            return
        }

        def serverinfo = Constants.SELF_ENV_MAPPING.get(mail);
        echo "${serverinfo}";
        if (serverinfo == null) {
            echo "failed to get the configuration of email : ${mail}";
            error("failed to get the configuration of email : ${mail}");
        }

        server = serverinfo.get("server");
        port = serverinfo.get("port");

        if (server == null) {
            echo "Server not found...";
            error("failed to get the server configuration of email : ${mail}");
        } else {
            echo "Server is configured as ${server} for mail: ${mail}";
        }

        if (port == "") {
            echo "Port not found...";
            error("failed to get the port configuration of email : ${mail}");
        } else {
            echo "Port is configured as ${port} for mail: ${mail}";
        }

        MONGODB_HOST = server
        MONGODB_PORT = serverinfo.get("mongoport")
        REDIS_PORT = serverinfo.get("redisport")
        MQ_PORT = serverinfo.get("mqport")
        MYSQL_PORT = serverinfo.get("mysqlport")
        HTTP_PORT = serverinfo.get("httpport")
    }

    return [mail, username, server, port, branch, MONGODB_HOST, MONGODB_PORT, REDIS_PORT, MQ_PORT, MYSQL_PORT, HTTP_PORT]
}
