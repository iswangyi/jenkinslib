def notifyFailed(mail) {
    sendMail(mail,
        subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
        attachLog: true
    );
}


def sendMail(String to,
             String subject,
             String body,
             List<String> cc=["chenzaichun@cloud-star.com.cn",
                              "shiwenqiang@cloud-star.com.cn",
                              "wuxiaobing@cloud-star.com.cn",
                              "maochaohong@cloud-star.com.cn",
                              "jipeigui@cloud-star.com.cn"],
             String replyTo='$DEFAULT_REPLYTO',
             boolean attachLog=false) {
    ccs = cc.join(", cc:")
    emailext (
        subject: subject,
        body: body,
        replyTo: replyTo,
        to: "${to}, cc: ${ccs}",
        attachLog: attachLog
    );
}

def sendMail(Map config) {
    def to = config.to ? config.to : ""
    def subject = config.subject ? config.subject : "No Subject"
    def body = config.body ? config.body : ""
    def cc = config.cc ? config.cc : ["chenzaichun@cloud-star.com.cn",
                                      "shiwenqiang@cloud-star.com.cn",
                                      "wuxiaobing@cloud-star.com.cn",
                                      "maochaohong@cloud-star.com.cn",
                                      "jipeigui@cloud-star.com.cn"]
    def replyTo = config.replyTo ? config.replyTo : '$DEFAULT_REPLYTO'
    def attachLog = config.attachLog ? config.attachLog : false

    sendMail(to, subject, body, cc, replyTo, attachLog)
}
