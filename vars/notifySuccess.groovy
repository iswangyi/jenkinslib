def call() {
    emailext (
        subject: "任务成功: '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """<p>任务成功: '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>查询任务日志: "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
        to: '$DEFAULT_RECIPIENTS',
        attachLog: true
    )
}
