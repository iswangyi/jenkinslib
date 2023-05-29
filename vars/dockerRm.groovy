def call(runserver, container_name) {
     docker.withTool("docker") {
        withDockerServer([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", uri: "${runserver}"]) {
            def EXISTS = sh(script: "docker ps -a |grep \"${container_name}\" |wc -l", returnStdout: true).trim()
            if (EXISTS != "0") {
                sh "docker rm -f ${container_name}"
            }
        }
    }
}
