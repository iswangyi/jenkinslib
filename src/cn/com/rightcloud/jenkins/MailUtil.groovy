package cn.com.rightcloud.jenkins

@Singleton
class MailUtil {
    public static void notifyFailed(mail) {
        sendMail(mail,
                 subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                 body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
                 attachLog: true
        );
    }


    public static void sendMail(String to,
                         String subject,
                         String body,
                         List<String> cc=["maxiao@cloud-star.com.cn",
                                          "shiwenqiang@cloud-star.com.cn",
                                          "wuxiaobing@cloud-star.com.cn",
                                          "maochaohong@cloud-star.com.cn",
                                          "jipeigui@cloud-star.com.cn",
                                          "rdc_test@cloud-star.com.cn"],
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

}
