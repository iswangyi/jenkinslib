import cn.com.rightcloud.jenkins.Constants

def getCompTag(comp, tag) {
    if (!tag?.trim()) {
        error("${comp} Tag cannot be empty")
    }
    echo "retag: $tag"
    tag = tag.trim()
    def number = tag.tokenize('.')[-1]
    return  "image.rightcloud.com.cn/rightcloud/rightcloud-${comp}:v.p.1.${number}"
}

def reTag(orgtag, newtag) {
    sh "docker pull $orgtag"
    sh "docker tag $orgtag $newtag"
    sh "docker push $newtag"
}

def deployComponent(tag, deployFuncStr, servers) {
    stage("${deployFuncStr}") {
        if (tag.trim()) {
            sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                for (s in servers[0..-2]) {
                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${s} \". /usr/local/rightcloud/rightcloud_env_ha && ${deployFuncStr} ${tag}\" "
                }
            }
        }
    }
}


def call(servers=["10.69.0.128", "10.69.0.129", "10.69.0.130", "10.69.0.131"], isReTAG=false) {
    try {
        node {
            def servertag
            def migrationtag
            def consoletag
            def adaptertag
            def scheduletag
            def ansibletag
            def monitorServerTag
            def monitorAgentTag
            def openAPITag


            // 注意，这里的机器都配置了免密登录的，所以改ip的时候注意对应key
            // 这里用的是10.69.0.234这台机器的key
            // echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC/nLU87dto8IpOJdoNBXiKIZATixrWNv2rb44ybZ+0LYzRiHEHcyZlitvI+EpPVaqU/gP6IHqgtZvvMDWPYATxSXb6rrkt3QgltCI2WbBVqVDjVakfqHKP9RIW+DNvytzZs9ifRO7S05nRV1QT+Agnszi+sikjOsZXqK+PUZ6fwECUnhUfRH+1+I8pvngVqOas/88645HOeUsbozMBcSiQgSbOJK86N3kq07YwBZl58gLd80M3YCxQIKw4t+JvqVA7rbKFj+0ih/oqjlv3H0TVHHJ/niquXwElGL6kbBUJ4b6C4iaVuzfD5q0c/uc5cHsrny06SqwioQ48FzmKiFnN root@rightcloud-am9eezvb" >> ~/.ssh/authorized_keys
            // def servers=["10.69.0.128", "10.69.0.129", "10.69.0.130"]

            //def PARAM_GIT_TAGS = "release-test3.0";
            properties([parameters([
                string(name: "DB_MIGRATION_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "SERVER_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "GATEWAY_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "ADAPTER_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "SCHEDULE_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "ANSIBLE_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "CONSOLE_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "REPOMIRROR_TAG", defaultValue: '', description: "demo环境也可以不填"),
                string(name: "MONITOR_SERVER_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "MONITOR_AGENT_TAG", defaultValue: '', description: "demo环境必填，其他环境可以不填"),
                string(name: "BUILD_DESRCIPTION", defaultValue: '', description: "描述一下，留作记录，demo环境必填"),
            ])
            ])

            if (isReTAG) {
                stage('retag for demo env') {
                    if (!params.BUILD_DESRCIPTION?.trim()) {
                        error("demo环境发版必须填写描述信息")
                    }
                    servertag = getCompTag('server', params.SERVER_TAG)
                    gatewaytag = getCompTag('gateway', params.GATEWAY_TAG)
                    migrationtag = getCompTag('db-upgrade', params.DB_MIGRATION_TAG)
                    consoletag = getCompTag('console', params.CONSOLE_TAG)
                    adaptertag = getCompTag('adapter', params.ADAPTER_TAG)
                    scheduletag = getCompTag('schedule', params.SCHEDULE_TAG)
                    ansibletag = getCompTag('ansible', params.ANSIBLE_TAG)
                    monitorServerTag = getCompTag('monitor-server', params.MONITOR_SERVER_TAG)
                    monitorAgentTag = getCompTag('monitor-agent', params.MONITOR_AGENT_TAG)
                    container('docker') {
                        // withDockerRegistry([credentialsId: "02ffbd93-1dfc-4f77-b3be-1b403c9632e0", url: "https://image.rightcloud.com.cn"]) {
                            reTag(params.SERVER_TAG, servertag)
                            reTag(params.GATEWAY_TAG, gatewaytag)
                            reTag(params.DB_MIGRATION_TAG, migrationtag)
                            reTag(params.CONSOLE_TAG, consoletag)
                            reTag(params.ADAPTER_TAG, adaptertag)
                            reTag(params.SCHEDULE_TAG, scheduletag)
                            reTag(params.ANSIBLE_TAG, ansibletag)
                            reTag(params.MONITOR_SERVER_TAG, monitorServerTag)
                            reTag(params.MONITOR_AGENT_TAG, monitorAgentTag)
                        }
                    }
                }
            }

            if (isReTAG) {
                stage("db migration") {
                    if (migrationtag.trim()) {
                        // db migration provided
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[0]} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-dbmigration ${migrationtag}\" "
                        }
                    }
                }

                deployComponent(servertag, "deploy-server", servers)
                deployComponent(gatewaytag, "deploy-gateway", servers)
                deployComponent(adaptertag, "deploy-adapter", servers)
                deployComponent(scheduletag, "deploy-schedule", servers)
                deployComponent(consoletag, "deploy-console", servers)

                stage("deploy ansible") {
                    if (ansibletag.trim()) {
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            for (s in servers[0..-2]) {
                                sh "ssh -T -o StrictHostKeyChecking=no -l root ${s} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-ansible ${ansibletag}\" "
                            }
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[0]} \". /usr/local/rightcloud/rightcloud_env_ha && run_haproxy\" "
                            // sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[1]} \". /usr/local/rightcloud/rightcloud_env_ha && run_haproxy\" "
                        }
                    }
                }

                stage("deploy monitor_server") {
                    if (params.MONITOR_SERVER_TAG.trim()) {
                        // db migration provided
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[2]} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-monitor-server ${params.MONITOR_SERVER_TAG}\" "
                        }
                    }
                }
    
                stage("deploy monitor_agent") {
                    if (params.MONITOR_AGENT_TAG.trim()) {
                        // db migration provided
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[2]} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-monitor-agent ${params.MONITOR_AGENT_TAG}\" "
                        }
                    }
                }

                deployComponent(REPOMIRROR_TAG, "deploy-repomirror", servers)
            }else {
                stage("db migration") {
                    if (DB_MIGRATION_TAG.trim()) {
                        // db migration provided
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[0]} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-dbmigration ${DB_MIGRATION_TAG}\" "
                        }
                    }
                }

                deployComponent(SERVER_TAG, "deploy-server", servers)
                deployComponent(GATEWAY_TAG, "deploy-gateway", servers)
                deployComponent(ADAPTER_TAG, "deploy-adapter", servers)
                deployComponent(SCHEDULE_TAG, "deploy-schedule", servers)
                deployComponent(CONSOLE_TAG, "deploy-console", servers)

                stage("deploy ansible") {
                    if (ANSIBLE_TAG.trim()) {
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            for (s in servers[0..-2]) {
                                sh "ssh -T -o StrictHostKeyChecking=no -l root ${s} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-ansible ${ANSIBLE_TAG}\" "
                            }
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[0]} \". /usr/local/rightcloud/rightcloud_env_ha && run_haproxy\" "
                            // sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[1]} \". /usr/local/rightcloud/rightcloud_env_ha && run_haproxy\" "
                        }
                    }
                }

                stage("deploy monitor_server") {
                    if (params.MONITOR_SERVER_TAG.trim()) {
                        // db migration provided
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[2]} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-monitor-server ${params.MONITOR_SERVER_TAG}\" "
                        }
                    }
                }
    
                stage("deploy monitor_agent") {
                    if (params.MONITOR_AGENT_TAG.trim()) {
                        // db migration provided
                        sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                            sh "ssh -T -o StrictHostKeyChecking=no -l root ${servers[2]} \". /usr/local/rightcloud/rightcloud_env_ha && deploy-monitor-agent ${params.MONITOR_AGENT_TAG}\" "
                        }
                    }
                }

                deployComponent(REPOMIRROR_TAG, "deploy-repomirror", servers)
            }
        }
    } catch (e) {
        println(e.getMessage());
        println(e.getStackTrace());
        //notifyFailed(mail)
        error("Failed build as " + e.getMessage())
    }
}
