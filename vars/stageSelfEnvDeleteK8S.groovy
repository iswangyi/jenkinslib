import cn.com.rightcloud.jenkins.Constants

def notifySuccessful(mail, serverinfo) {
    emailext (
        subject: "SUCCESSFUL: Your TEST ENV has been destroyed",
        body: """<font size=72 color="green">Thanks ${mail}, your test env has been destroyed</font></p>""",
        replyTo: '$DEFAULT_REPLYTO',
        to: mail,
        attachLog: false
    );
}

def notifyFailed(mail) {
    emailext (
        subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
        replyTo: '$DEFAULT_REPLYTO',
        to: mail,
        attachLog: true
    );
}

def call() {
    properties([parameters([
        string(name: "NAMESPACE", defaultValue: '',
               description: "default namespace to delete"),
        choice(name: "COMPONENTS", choices:'ALL\nServer/Adapter\nConsole\nSchedule\n', description: "component to delete from k8s server")
    ])])

    def mail="${params.NAMESPACE}@cloud-star.com.cn"
    try {

        def namespace = params.NAMESPACE
        if (!isNamespaceValid(namespace)) {
            error("destroy components in namespace \"$namespace\" is not allowed")
        }


        def k8sserver = Constants.SELFENV_K8S_SERVER
        container('kubectl') {
            stage('clone yaml template') {
                git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
            }

            stage('delete resources') {
                sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-server-template.yaml"
                sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-adapter-template.yaml"

                sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-console-template.yaml"
                sh " sed -i 's|\$namespace|'${namespace}'|g' rightcloud/app/rightcloud-schedule-template.yaml"

                if (params.COMPONENTS == 'ALL' || params.COMPONENTS == 'Server/Adapter'){
                    sh "kubectl -s ${k8sserver} delete -f  ./rightcloud/app/rightcloud-server-template.yaml || true"
                    sh "kubectl -s ${k8sserver} delete -f  ./rightcloud/app/rightcloud-adapter-template.yaml || true"
                }
                if (params.COMPONENTS == 'ALL' || params.COMPONENTS == 'Console'){
                    sh "kubectl -s ${k8sserver} delete -f  ./rightcloud/app/rightcloud-console-template.yaml || true"
                }
                if (params.COMPONENTS == 'ALL' || params.COMPONENTS == 'Schedule'){
                    sh "kubectl -s ${k8sserver} delete -f  ./rightcloud/app/rightcloud-schedule-template.yaml || true"
                }

            }
        }
        notifySuccessful(mail, "http://${params.NAMESPACE}.rctest.com")

    } catch(e) {
        println(e);
        println(e.getMessage());
        println(e.getStackTrace()); 
        notifyFailed(mail)
    }
}
