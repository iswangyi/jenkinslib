def call(runserver, app, params) {
    docker.withTool("docker") {
        withDockerServer([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", uri: "${runserver}"]) {
            app.run(params)
        }
    }
}
