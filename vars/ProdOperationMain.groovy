import cn.com.rightcloud.jenkins.Constants

def call() {
  node {
    properties([parameters([
        string(name: "server_image", defaultValue: '', description: "server镜像，例如：image.rightcloud.com.cn/vue-project/rightcloud-server:v.t.1.187，可以为空，包括下面所有镜像相关参数都可以为空"),
        string(name: "dbmigration_image", defaultValue: '', description: "dbmigration tag，和server_image一起更新，要么都不填，要么都填"),
        string(name: "bss_image", defaultValue: '', description: "server tag"),
        string(name: "console_image", defaultValue: '', description: "Console tag"),
        string(name: "portal_image", defaultValue: '', description: "Console tag"),
        string(name: "adapter_image", defaultValue: '', description: "adapter tag"),
        string(name: "schedule_image", defaultValue: '', description: "schedule tag"),
        string(name: "ansible_image", defaultValue: '', description: "ansible tag"),
        string(name: "openAPI_image", defaultValue: '', description: "openapi tag"),
        string(name: "monitor_server_image", defaultValue: '', description: "monitor server tag"),
        string(name: "monitor_agent_image", defaultValue: '', description: "monitor agent tag"),

        string(name: "release_version", defaultValue: "$params.release_version", description: "release version,e.g. 3.7.1"),
        string(name: "namespace", defaultValue: 'rightcloud-operation', description: ""),

    ])])

    try {

        // TODO: 自动更新release_version

        stage('rename image tag') {

        }

        stage('prepare yaml template') {

            git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: 'http://10.68.6.20:8082/cloudstar/kubernetes-yml.git', branch: "master"
            // server
            sh "sed -i 's|^serverTag=.*|serverTag='${params.server_image}'|g' rightcloud/replace-yaml.sh"
            // console
            sh "sed -i 's|^consoleTag=.*|consoleTag='${params.console_image}'|g' rightcloud/replace-yaml.sh"
            // adapter
            sh "sed -i 's|^adapterTag=.*|adapterTag='${params.adapter_image}'|g' rightcloud/replace-yaml.sh"
            // schedule
            sh "sed -i 's|^scheduleTag=.*|scheduleTag='${params.schedule_image}'|g' rightcloud/replace-yaml.sh"
            // ansible
            sh "sed -i 's|^ansibleTag=.*|ansibleTag='${params.ansible_image}'|g' rightcloud/replace-yaml.sh"
            // openAPI
            sh "sed -i 's|^openapiTag=.*|openapiTag='${params.openAPI_image}'|g' rightcloud/replace-yaml.sh"
            // namespace
            sh "sed -i 's|namespaceVersion|'${params.namespace}'|g'   rightcloud/replace-yaml.sh"
            // dbmigration
            sh "sed -i 's|\$dbmigrationTag|'${params.dbmigration_image}'|g'   rightcloud/app/rightcloud-server-template.yaml"

            // monitor_server
            sh "sed -i 's|\$monitorserverTag|'${params.monitor_server_image}'|g'   rightcloud/app/rightcloud-monitor-server-template.yaml"
            // monitor_agent
            sh "sed -i 's|\$monitoragentTag|'${params.monitor_agent_image}'|g'   rightcloud/app/rightcloud-monitor-agent-template.yaml"
            // portal
            sh "sed -i 's|\$marketTag|'${params.portal_image}'|g'   rightcloud/app/rightcloud-market-template.yaml"
            // bss
            sh "sed -i 's|\$bssTag|'${params.bss_image}'|g'   rightcloud/app/rightcloud-bss-template.yaml"

            sh "  chmod +x   ./rightcloud/replace-yaml.sh"
            sh "  ./rightcloud/replace-yaml.sh"

            echo "${params.server_image}"
        }

        container('kubectl') {
            def kubectl = 'kubectl -s https://10.68.12.7:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJkYXNoYm9hcmQtYWRtaW4tdG9rZW4tam1tNzkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGFzaGJvYXJkLWFkbWluIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiZTE2ZmU2YjMtNzIyNy0xMWU5LTk3MDItMDA1MDU2OGRlNjE4Iiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50Omt1YmUtc3lzdGVtOmRhc2hib2FyZC1hZG1pbiJ9.QSxrMDb6_nEn5JwSlXYbSpxndWxxUvJ_SWCJ8NtZEyVQfEu1wJdUuWj4OTXIyDHkDBS-7eKnDfKXFeLPwo2kSJW9FmyakHnmzfMb-fFc0_KQBpW7EHBigBd6SrcH1E_ZDm6N4UqXOAqFrO2KBRIzerBAQwxaiYEc-k5N1Pxzgz1FNO2-f0qkD2nirqO2G4xS9XaP0Lr8cm_eOolpBSPNI3mQZgKbbsLtxlOqQq5s47M7SJazRod7WZVFNcev9tYlkLeRnwttH5cxQ0-IShFyOoBj_jzk81xfuZySFfF-NjZon3PkC--7g3Uw-hzcUfihx0LfUSgCcLVzu-AHRyCWrIGFZ5XtYckfpSB_rqoU_4ltKhbI6CzvV8siADSOtdY5fBxsdPN3mS97ITLvCLwq7cQYSCYyEIeE39h7jf2ovfdOB3i33qw55P1S4hxKjAsGXSQqy5Z1bbjVORAUtESJQSM_31a-z52knEpgq-E5IOkTJ_R0ClcF-SLHlg_UqY7ur5gFv6fmOiXNi_pRF9_X_bB4R2F2-H2e6aThJAmRV_UKLI-xIvfUEnOYjjoc6AnCja0-EbRyV2gB5Arsud6yFJIjl15Aca-Qsz1pZrI7s8vJfLKTKzh0lT4832eO2L1YPzMWWQyF2Euh7cc4P1mYty9K3WheR13mKgMBP3Dd_Zs --insecure-skip-tls-verify=true'

            stage('Deploy all to k8s') {
                // server
                if (params.server_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-server-template.yaml" }
                // console
                if (params.console_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-console-template.yaml" }
                // adapter
                if (params.adapter_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-adapter-template.yaml" }
                // schedule
                if (params.schedule_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-schedule-template.yaml" }
                // ansible
                if (params.ansible_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-ansible-template.yaml" }
                // bss
                if (params.bss_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-bss-template.yaml" }
                // portal
                if (params.portal_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-market-template.yaml" }
                // monitor_server
                if (params.monitor_server_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-monitor-server-template.yaml" }
                // monitor_agent
                if (params.monitor_agent_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-monitor-agent-template.yaml" }
                // openAPI
                if (params.openAPI_image.trim()) { sh "${kubectl} apply -f  ./rightcloud/app/rightcloud-openapi-template.yaml" }
                
            }
        }
      
    }catch(err) {
        println(err);
        println(err.getMessage());
        println(err.getStackTrace());
        error(err.getMessage())
    }

  }
}
