def call(String fulltag,
         String dockerfile,
         String buildargs="") {
    def app
    // def dockerserver = getDockerBuildServer2()

    // httpGet("http://10.68.7.92:8111/build?server=${dockerserver}")

    // def buildserver = dockerserver
    // if (!buildserver.startsWith("tcp://")) {
    //     buildserver = "tcp://${buildserver}"
    // }

    // buildserver = buildserver.trim()

    //try {
    container('docker') {
        try {
        //docker.withTool("docker") {
        //echo "docker build server is: \"${buildserver}\""
        echo "image \"${fulltag}\" will be build"
        //withDockerServer([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", uri: "${buildserver}"]) {
        //sh "sleep 500"
        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
            //app = docker.build(fulltag, "--no-cache -f ${dockerfile} . ${buildargs}")
            //sh "sleep 300"
            sh "docker build -t ${fulltag} --no-cache -f ${dockerfile} . ${buildargs}"
            //app.push()
            sh "docker push ${fulltag}"
//            sh "docker rmi -f ${fulltag} || true"
        }
        } finally {
            //sh "docker system prune -a -f; docker images | grep \"<none>\" | awk '{print \$3}' | xargs docker rmi -f || true"
//            sh "docker system prune -a -f;"
        }
        //}
        //}
    }
    //} // finally {

    //     httpGet("http://10.68.7.92:8111/release?server=${dockerserver}")
    // }

    return app
}
