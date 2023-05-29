def call(mail) {
    sendMail2(
        to: mail,
        subject: "任务失败: '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """<p>任务失败: '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>查询任务日志: "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
        attachLog: true
    );
}
