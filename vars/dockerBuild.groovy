def call(String buildserver, String registry,
         String tagprefix, String dockerfile, String buildargs="") {
    def app
    docker.withTool("docker") {
        echo "docker build server is: ${buildserver}"
        withDockerServer([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", uri: "${buildserver}"]) {
            app = docker.build("${registry}:${tagprefix}${env.BUILD_NUMBER}", "--no-cache -f ${dockerfile} . ${buildargs}")
        }
    }

    return app
}
