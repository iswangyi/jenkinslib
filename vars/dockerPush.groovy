def call(buildserver, app) {
    docker.withTool("docker") {
        withDockerServer([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", uri: "${buildserver}"]) {
            app.push()
        }
    }
}
