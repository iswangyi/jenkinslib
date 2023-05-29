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

        return [mail, username]
    }
}
