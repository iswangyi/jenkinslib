import cn.com.rightcloud.jenkins.Constants

def call(username) {
    def mail = "${username}@cloud-star.com.cn".trim()

    def server
    def port
    def MONGODB_HOST
    def MANGODB_PORT
    def REDIS_PORT
    def MQ_PORT
    def MYSQL_PORT
    def HTTP_PORT

    def serverinfo = Constants.SELF_ENV_MAPPING.get(mail)
    echo "${serverinfo}";
    if (serverinfo == null) {
        echo "failed to get the configuration of email : ${mail}."
        error("failed to get the configuration of email : ${mail}.")
    }

    server = serverinfo.get("server");
    port = serverinfo.get("port");

    if (server == null) {
        echo "Server not found...";
        error("failed to get the server configuration of email : ${mail}")
    } else {
        echo "Server is configured as ${server} for mail: ${mail}"
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

    return [mail, username, server, port, MONGODB_HOST, MONGODB_PORT, REDIS_PORT, MQ_PORT, MYSQL_PORT, HTTP_PORT]
}
