def call(mail, username) {
    sendMail2 (
        subject: "任务等待中: '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """亲爱的${username},<br><p>任务处于等待中: '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        你的自测环境还在<font size=72 color="red">运行</font>中，如果要新的构建，请先手动移除掉自己的自测环境。""",
        to: "${mail}",
        attachLog: false
    );
}
