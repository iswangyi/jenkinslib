def notifySuccessful(share_info) {
    emailext (
        subject: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """<p>SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Please download repomirror ova install package ${params.PACKAGE_VERSION}.tar.gz via "<a href='${share_info}'>'${share_info}'</a>" </p>""",
        replyTo: "${env.INSTALL_PACKAGE_REPLYTO}",
        to: "${env.INSTALL_PACKAGE_RECIPIENTS}",
        attachLog: true
        //recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
    )
}
def call() {
    node {
        properties([parameters([
            string(name: "IMAGE_TAG", defaultValue: 'image.rightcloud.com.cn/rightcloud/repomirror:v1.20180307',
                   description: "image tag use to build the ova"),
            string(name: "BUILD_SERVER", defaultValue: '10.69.0.234',
                   description: "server used to build the ova"),
            string(name: "VERSION_PREFIX", defaultValue: '1.20180307',
                   description: "version prefix"),
            booleanParam(defaultValue: false, description: 'import ova for testing?', name: 'IS_IMPORT_OVA'),
            string(name: "IP_FOR_VM", defaultValue: '10.62.0.121', description: "vm ip addres for testing"),
        ])])

        try {
            def version="${params.VERSION_PREFIX}"
            def cmdparam = ""
            def name="rightcloud_repomirror_${version}"

            if (params.IS_IMPORT_OVA) {
                cmdparam += " -i -p ${params.IP_FOR_VM} "
            }

            stage ("call script") {
                sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} \"curl -fSsL --request GET --header 'PRIVATE-TOKEN: 3srGP9AmSuEWe3YzJxVs' http://10.68.6.20:8082/chenzaichun/allinone-docker/raw/master/ova/build_ova_repo.sh | bash -s -- -v ${version} ${cmdparam} -t ${params.IMAGE_TAG}\" "
                }
            }

            stage('upload to baidu wangpan') {
                def share_info
                sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                    sh "ssh -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} \"curl --header 'PRIVATE-TOKEN: 3srGP9AmSuEWe3YzJxVs' http://10.68.6.20:8082/cloudstar/infrastructure-ops/raw/master/upload-baidu/upload.sh| bash -s -- -b ${env.BAIDU_BDUSS} -s /root/govc/${name}.ova -d /POC\" "
                    share_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} \"cat /tmp/share_link.txt\" ").trim()
                }
                notifySuccessful("${share_info}")
            }
        } catch (e) {
            println(e);
            println(e.getMessage());
            println(e.getStackTrace());
            // notifyFailed(mail)
            error(e.getMessage())
        }
    }
}
