def call(server, containername) {
    docker.withTool("docker") {
        withDockerServer([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", uri: "${server}"]) {
            def EXISTS = sh(script: "docker ps -a |grep \"${containername}\" |wc -l", returnStdout: true).trim()

            return EXISTS != "0"
        }
    }
}
