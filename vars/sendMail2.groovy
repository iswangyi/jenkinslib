import cn.com.rightcloud.jenkins.Constants

def call(String to,
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

def call(Map config) {
    def cc = (config.cc == null) ? Constants.DEFAULT_CC : config.cc
    def subject = (config.subject == null) ? "No Subject" : config.subject
    def mailbody = (config.body == null) ? "" : config.body
    def replyTo = (config.replyTo == null) ? '$DEFAULT_REPLYTO' : config.replyTo
    def attachLog = (config.attachLog == null) ? false : config.attachLog
    def to = (config.to == null) ? cc : (config.to + "," + cc)

    emailext (
        subject: subject,
        body: mailbody,
        replyTo: replyTo,
        to: to,
        attachLog: attachLog
    );
}
