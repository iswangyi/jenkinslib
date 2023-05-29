import cn.com.rightcloud.jenkins.Constants

//def getCompTag(comp, tag) {
//    if (!tag?.trim()) {
//        error("${comp} Tag cannot be empty")
//    }
//    echo "retag: $tag"
//    tag = tag.trim()
//    def number = tag.tokenize('.')[-1]
//    return  "image.rightcloud.com.cn/rightcloud/rightcloud-${comp}:v3.p.${number}"
//}
//
//def reTag(orgtag, newtag) {
//    if (orgtag.trim()){
//        sh "docker pull $orgtag"
//        sh "docker tag $orgtag $newtag"
//        sh "docker push $newtag"
//    }
//}


def call() {
    try {

        node {
                def servertag=""
                def gatewaytag=""
                def migrationtag=""
                def consoletag=""
                def adaptertag=""
                def scheduletag=""
                def ansibletag=""
                def monitorServerTag=""
                def monitorAgentTag=""
                def resourcetag=""
                def analysistag=""
                def iamtag=""
                def thirdpartytag=""
                def thirdpartydbmigrationtag=""
                // 注意，这里的机器都配置了免密登录的，所以改ip的时候注意对应key
                // 这里用的是10.69.0.234这台机器的key
                // echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC/nLU87dto8IpOJdoNBXiKIZATixrWNv2rb44ybZ+0LYzRiHEHcyZlitvI+EpPVaqU/gP6IHqgtZvvMDWPYATxSXb6rrkt3QgltCI2WbBVqVDjVakfqHKP9RIW+DNvytzZs9ifRO7S05nRV1QT+Agnszi+sikjOsZXqK+PUZ6fwECUnhUfRH+1+I8pvngVqOas/88645HOeUsbozMBcSiQgSbOJK86N3kq07YwBZl58gLd80M3YCxQIKw4t+JvqVA7rbKFj+0ih/oqjlv3H0TVHHJ/niquXwElGL6kbBUJ4b6C4iaVuzfD5q0c/uc5cHsrny06SqwioQ48FzmKiFnN root@rightcloud-am9eezvb" >> ~/.ssh/authorized_keys
                // def servers=["10.69.0.128", "10.69.0.129", "10.69.0.130"]

                //def PARAM_GIT_TAGS = "release-test3.0";
                properties([parameters([
                    string(name: "DB_MIGRATION_TAG", defaultValue: '', description: "DB_MIGRATION镜像"),
                    string(name: "SERVER_TAG", defaultValue: '', description: "SERVER镜像"),
                    string(name: "GATEWAY_TAG", defaultValue: '', description: "GATEWAY镜像"),
                    string(name: "ADAPTER_TAG", defaultValue: '', description: "ADAPTER镜像"),
                    string(name: "SCHEDULE_TAG", defaultValue: '', description: "SCHEDULE镜像"),
                    string(name: "ANSIBLE_TAG", defaultValue: '', description: "ANSIBLE镜像"),
                    string(name: "CONSOLE_TAG", defaultValue: '', description: "CONSOLE镜像"),
                    string(name: "REPOMIRROR_TAG", defaultValue: '', description: "REPOMIRROR镜像"),
                    string(name: "MONITOR_SERVER_TAG", defaultValue: '', description: "MONITOR_SERVER镜像"),
                    string(name: "MONITOR_AGENT_TAG", defaultValue: '', description: "MONITOR_AGENT镜像"),
                    string(name: "RESOURCE_TAG", defaultValue: '', description: "RESOURCE镜像"),
                    string(name: "ANALYSIS_TAG", defaultValue: '', description: "ANALYSIS镜像"),
                    string(name: "IAM_TAG", defaultValue: '', description: "iam镜像"),
                    string(name: "ThirdParty_TAG", defaultValue: '', description: "ThirdParty和ThirdPartyDbMigration镜像tag一致"),
                    string(name: "ThirdPartyDbMigration_TAG", defaultValue: '', description: "ThirdParty和ThirdPartyDbMigration镜像tag一致"),
                    string(name: "BUILD_Server", defaultValue: '', description: "更新Server服务器，必填(*)"),
                    string(name: "BUILD_DESRCIPTION", defaultValue: '', description: "描述一下，留作记录,必填(*)"),
                ])
                ])



                stage('get k8s v3 images') {
                    if (!params.BUILD_DESRCIPTION?.trim()) {
                        error("K8s V3 测试环境发版必须填写描述信息")
                    }
                    if (!params.BUILD_Server?.trim()) {
                        error("K8s V3 更新执行服务器")
                    }

                    if (params.SERVER_TAG.trim()) {
                        servertag=params.SERVER_TAG
                    }

                    if (params.GATEWAY_TAG.trim()) {
                        gatewaytag=params.GATEWAY_TAG
                    }

                    if (params.DB_MIGRATION_TAG.trim()) {
                        migrationtag =params.DB_MIGRATION_TAG
                    }

                    if (params.CONSOLE_TAG.trim()) {
                        consoletag = params.CONSOLE_TAG
                    }

                    if (params.ADAPTER_TAG.trim()) {
                        adaptertag = params.ADAPTER_TAG
                    }

                    if (params.SCHEDULE_TAG.trim()) {
                        scheduletag =params.SCHEDULE_TAG
                    }
                    if (params.ANSIBLE_TAG.trim()) {
                        ansibletag = params.ANSIBLE_TAG
                    }
                    if (params.MONITOR_SERVER_TAG.trim()) {
                        monitorServerTag = params.MONITOR_SERVER_TAG
                    }

                    if (params.MONITOR_AGENT_TAG.trim()) {
                        monitorAgentTag = params.MONITOR_AGENT_TAG
                    }

                    if (params.RESOURCE_TAG.trim()) {
                        resourcetag = params.RESOURCE_TAG
                    }
                    if (params.ANALYSIS_TAG.trim()) {
                        analysistag = params.ANALYSIS_TAG
                    }
                    if (params.IAM_TAG.trim()) {
                        iamtag = params.IAM_TAG
                    }
                    if (params.ThirdParty_TAG.trim()) {
                        thirdpartytag = params.ThirdParty_TAG
                    }
                    if (params.ThirdPartyDbMigration_TAG.trim()) {
                        thirdpartydbmigrationtag = params.ThirdPartyDbMigration_TAG
                    }

//                    container('docker') {
//                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
//                            reTag(params.SERVER_TAG, servertag)
//                            reTag(params.GATEWAY_TAG, gatewaytag)
//                            reTag(params.DB_MIGRATION_TAG, migrationtag)
//                            reTag(params.CONSOLE_TAG, consoletag)
//                            reTag(params.ADAPTER_TAG, adaptertag)
//                            reTag(params.SCHEDULE_TAG, scheduletag)
//                            reTag(params.ANSIBLE_TAG, ansibletag)
//                            reTag(params.MONITOR_SERVER_TAG, monitorServerTag)
//                            reTag(params.MONITOR_AGENT_TAG, monitorAgentTag)
//                        }
//                    }
                }
                stage("K8s in deploy images") {
                    def API_Server=params.BUILD_Server
                    def NameSpace="default"
                    def tag_groups="${servertag},${gatewaytag},${consoletag},${adaptertag},${scheduletag},${ansibletag},${resourcetag},${analysistag},${iamtag}"
                    def thirdparty_groups="${thirdpartydbmigrationtag},${thirdpartytag}"
                    def monitor_groups="${monitorServerTag},${monitorAgentTag}"
                    def kubectl=""
                    def sshflag=false
                    switch(API_Server) {
                        case '10.69.1.32':
                            kubectl="kubectl -s https://10.69.1.32:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6InR0S2JfVHViVTdDTUdCeHU5X3RxN3VzaHRvTXpJVWNibmZLX3Npd2YyZDQifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLTY3bGYyIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJmYTljNDhiNC04NDY2LTQ1ZmYtOWI1Mi1jZmEwODM5Yjk2YjAiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.j6PsDswA4u2QN5ih5eCpowTDLyedF1YuAD9-03ysIJ_EdHo1CV0Ibwtev_w4O1Gj80Kvoz8KaA-lfHR6PndOl9249SL2QSirKPP5qCO-mrBjicLiz8fERM6a3N_cptUd-hPC8whDOa_oHnoDCazVnaKsCtbtygTQaRQjvEzYkD4roRu45VWx3W-Dpw5AAoIEKopckiPMpkj13kpM0jqUYa-3GMnj796SFQENN-qKOW4baVoatDkO4WV7glILBcKhtjLlGGfdDSwjHWer1dl9-6SvbVgSIUIczW5d5yi0xrdN0n-m9ddBYlHPbRLHUBwttYV7MHHnc0TTJnSvizabaQ --insecure-skip-tls-verify=true"
                            break
                        case '10.69.1.190':
                            kubectl="kubectl -s https://10.69.1.190:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6ImhyZ0QyMjFOSlJJanRGaEVWTF93d3c5Zl9XNzYwb1NWbWlLNjVvWkZULTAifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLW1jajRtIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJhZTcwY2NiNS0xMWM3LTRmNjYtYTg2Zi00ZTI1MmUzNGU4NGYiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.GSc-Il599TLvSUMtTQCrVe5pN_SFGLfuly16GQHrapr1-z4-bVqGStEL0h1siNzzTKzKbGrkJJTbOAtNVWSFnUfRbTgCNAKIzodSOTZgmxvUv3ovPzBALy9pXEYoSOdJJVksh0bIdxHsp6XOFBiSKWNKX5ouplcbiYoIjvOxUv5nOxoPfkjmBs_UfACeYS7-uFYx1rwiGVzVWnSRbkJBCejfQ_YNOnjA5C7klh4Ekb9UlT4xH-ap705mugmUp37MACiX5PKpC7hV75bm2mqzyGQvNaePg4F6jtKfXiZpqg_J5rvJezEIFoGslj8GcZkqqOBB6oar_DKn4g1bbin4kg --insecure-skip-tls-verify=true"
                            break
                        case '10.69.1.189':
                            kubectl="kubectl -s https://10.69.1.189:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IlpLZVJBNUU5NjJzNmNzSlNFMG5oMVBxREZ2OUw0R1NZNGhINHF5X2RWdjgifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLWxnY2trIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiI1MzZhYmU3Mi1lYjkzLTRjNjEtYTcyZS04NTMxOGJjODg5ZGYiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.AUZ8z_7ZaS_NwgdJK4jObTIKNtSPFJxKTddzdxmO7auaPBwIDtU4aUpdWZWpkC4nTuCiDvyqY4E7t4Prp6offSjOnzC4Im5uh5nBDV-ZrJU4mt6kweeMUDBqg8bnte7vGILeEjIqq3SaYoAv9ULAoIoVubQjJmsSo4G1_ywc_FNu2elhsBZu6Qdeh3f0JLvi14Ee4j8woBEbhzXvB989g0652w59OQm6Ggjy2E-ykOstEB0aWv_YlZvVMffnQpSL1ed_xM4DVPKfcvtekP6q9_lPHvne1B-DtBU8TDAvIvQ7c68cdXCB1ed5zgctfunMscmYh5zqj6YhBRPqMVAUBg --insecure-skip-tls-verify=true"
                            break
                        case '10.69.3.3':
                            kubectl="kubectl -s https://10.69.3.3:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IlNVemNqLVBIYV9lbWZCUHFFQXZaWFJ6cEliSllzeDFOT2Vud1pKRC14V1UifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLWs3bjJsIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiI3ZmM0M2RmYi00MjkxLTQ1OGUtYWIxNi04ZDA2MmNjZGI0OWUiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.STG7rjKdDMBuAuqdUFtVfvjZQb-a8TjkJU7PEDK0rv9eCTEW6iW0iYt-CXHEQR7AAqyvUOGS-71GwLb1bcQLpanzNukdL_Z31p-1A_cbtnaHPaFKl9kLX3KwglNLBxj08fas9l_eoBZNOFWsKDAC31SRPp993UCb1NISnAgrkeFnRusJBauedEmxlu86R44kMmIgMloqWe9u_7BRKM8IrFSplkhKLEP0jOs-GWFC_LK6nPhReB8oCoAri3cT3WO5hKZ18LPxH5742M1dgYyTBY31rmppbAIez0tjxOQiMVBPO5LBKl7G3RiDpM6OmM_HDQbJvQO4PngCzvni3rNshw --insecure-skip-tls-verify=true"
                            break
                        case '10.69.5.190':
                            kubectl="ssh -o StrictHostKeyChecking=no -l root 10.69.5.190 kubectl"
                            sshflag=true
                            break
                        default:
                            error("unknown ERROR update Server")
                            break
                    }
                    script{
                        echo "##########CMP APP deploy ################"
                        echo "tag_groups=["+tag_groups+"]"
                        for (tag in tag_groups.tokenize(',')){
                            if (tag.trim()) {
                                // tag provided
                                def container_name = tag.tokenize('/')[-1].tokenize(':')[0]
                                def deploy_name = "cmp-" + container_name.tokenize('-')[-1]

                                if (sshflag) {
                                    sshagent (credentials: ['ssh-10-69-5-190']) {
                                        sh "${kubectl} set image deployment/${deploy_name} ${container_name}=${tag} -n  ${NameSpace}"
                                    }
                                }else{
                                    container('kubectl') {
                                        sh "${kubectl} set image deployment/${deploy_name} ${container_name}=${tag} -n  ${NameSpace}"
                                    }
                                }

                            }
                        }
                        echo "##########Monitor deploy ################"
                        echo "monitor_groups=["+monitor_groups+"]"
                        container('kubectl') {
                            if (monitorServerTag.trim()){
                                sh "${kubectl} set image deployment/cmp-monitor-server rightcloud-monitor-server=${monitor_groups.tokenize(',')[0]} -n  ${NameSpace}"
                            }

                            if (monitorAgentTag.trim()){
                                sh "${kubectl} set image deployment/cmp-monitor-agent rightcloud-monitor-agent=${monitor_groups.tokenize(',')[1]} -n  ${NameSpace}"
                            }


                        }

                        echo "##########thirdparty deploy ################"
                        echo "thirdparty_groups=["+thirdparty_groups+"]"
                        container('kubectl') {
                           if (thirdpartydbmigrationtag.trim()){
                                sh "${kubectl} patch deployment/cmp-third-party -p '{\"spec\":{\"template\":{\"spec\":{\"initContainers\":[{\"name\":\"dbmigration\",\"image\":\"${thirdparty_groups.tokenize(',')[0]}\"}]}}}}' -n ${NameSpace} || true"
                           }

                            if (thirdpartytag.trim()){
                                sh "${kubectl} set image deployment/cmp-third-party rightcloud-third-party=${thirdparty_groups.tokenize(',')[1]} -n  ${NameSpace}"
                            }
                        }

                        echo "################dbmigration deploy############"
                        container('kubectl') {
                            if (migrationtag.trim()){
                                def check_job_str = sh(script: "${kubectl} get job -n ${NameSpace} |grep -w rightcloud-dbmigration|wc -l", returnStdout: true).trim()
                                echo ">>${check_job_str}"
                                if ("${check_job_str}"=="1"){
                                    echo ">>删除已存在rightcloud-dbmigration job"
                                    sh "${kubectl} delete job rightcloud-dbmigration -n ${NameSpace}"
                                }
                                echo ">>创建rightcloud-dbmigration job"
                                sh '''cat << EOF >kube-rightcloud-dbmigration-job.yaml                           
apiVersion: batch/v1
kind: Job
metadata:
  name: rightcloud-dbmigration
  namespace: '''+"${NameSpace}"+'''
spec:
  template:
    spec:
      containers:
        - name: rightcloud-dbmigration
          image: '''+"${migrationtag}"+'''
          imagePullPolicy: Always
          envFrom:
            - configMapRef:
                name: cmp-config
      restartPolicy: Never
EOF
                                '''
                                sh "${kubectl}  create -f kube-rightcloud-dbmigration-job.yaml"

                            }

                        }


                    }

                }
            }

    }catch (e) {
        println(e.getMessage());
        println(e.getStackTrace());
        //notifyFailed(mail)
        error("Failed build as " + e.getMessage())
    }
}
