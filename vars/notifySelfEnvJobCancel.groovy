def call(Map config) {
    sendMail2 (
        subject: "任务取消: '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """亲爱的 ${config.username},<br><p>您的编译任务已经取消: '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p><br>
        请先手动移除掉自己的自测环境，然后再重新编译。
""",
        to: "${config.mail}"
        attachLog: false
    );
}
