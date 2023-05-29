def call(String fulltag,
         String dockerfile,
         String buildargs="",
         String context="./") {
    def app

    container('builder') {
        try {
            echo "image \"${fulltag}\" will be build"
            sh "/kaniko/executor --context dir://${context} --dockerfile ${dockerfile} --destination ${fulltag} ${buildargs}  --cache=true --skip-tls-verify=true --cache-dir=/workspace/cache"
        } finally {
            echo "build push success"
        }

    }
    return app
}
