import cn.com.rightcloud.jenkins.Constants

// k8s_env  project、dev、demo(prod)、self_test
def call(k8sEnv) {
    def kubectl
    switch(k8sEnv) {
        case 'project':
            kubectl = "kubectl -s ${Constants.PRODUCT_K8S_SERVER} "
            break
        case 'self_env':
            kubectl = "kubectl -s ${Constants.SELFENV_K8S_SERVER} "
            break
        case 'dev':
            kubectl = "kubectl --kubeconfig=/home/kubectl-cer/config "
            break
        case 'prod':
            kubectl="kubectl -s https://10.68.12.7:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJkYXNoYm9hcmQtYWRtaW4tdG9rZW4tam1tNzkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGFzaGJvYXJkLWFkbWluIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiZTE2ZmU2YjMtNzIyNy0xMWU5LTk3MDItMDA1MDU2OGRlNjE4Iiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50Omt1YmUtc3lzdGVtOmRhc2hib2FyZC1hZG1pbiJ9.QSxrMDb6_nEn5JwSlXYbSpxndWxxUvJ_SWCJ8NtZEyVQfEu1wJdUuWj4OTXIyDHkDBS-7eKnDfKXFeLPwo2kSJW9FmyakHnmzfMb-fFc0_KQBpW7EHBigBd6SrcH1E_ZDm6N4UqXOAqFrO2KBRIzerBAQwxaiYEc-k5N1Pxzgz1FNO2-f0qkD2nirqO2G4xS9XaP0Lr8cm_eOolpBSPNI3mQZgKbbsLtxlOqQq5s47M7SJazRod7WZVFNcev9tYlkLeRnwttH5cxQ0-IShFyOoBj_jzk81xfuZySFfF-NjZon3PkC--7g3Uw-hzcUfihx0LfUSgCcLVzu-AHRyCWrIGFZ5XtYckfpSB_rqoU_4ltKhbI6CzvV8siADSOtdY5fBxsdPN3mS97ITLvCLwq7cQYSCYyEIeE39h7jf2ovfdOB3i33qw55P1S4hxKjAsGXSQqy5Z1bbjVORAUtESJQSM_31a-z52knEpgq-E5IOkTJ_R0ClcF-SLHlg_UqY7ur5gFv6fmOiXNi_pRF9_X_bB4R2F2-H2e6aThJAmRV_UKLI-xIvfUEnOYjjoc6AnCja0-EbRyV2gB5Arsud6yFJIjl15Aca-Qsz1pZrI7s8vJfLKTKzh0lT4832eO2L1YPzMWWQyF2Euh7cc4P1mYty9K3WheR13mKgMBP3Dd_Zs --insecure-skip-tls-verify=true"
            break
        case 'oem':
            kubectl="kubectl -s https://10.69.5.61:6443 --token=eyJhbGciOiJSUzI1NiIsImtpZCI6IlhtUXRJNDV6a0Q2cjktLW51STktMlZ1WW56YUhvckthN3dQc09oNkw1aTgifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLW0yc2Z4Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJjZmRmNGEyYi01ZmJiLTQ0NWMtOTgzZS01ZjkyYmIzM2UwZmEiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.rNbruVkzXDGIWvWDWFsdPvG0lYIVyxzLTHaArSezDFj1uF-bPm6bG1x5bMOoUO3evt-9kwFfNMROX1URKFLDqA-AyRiurLw-P_ahS298gvVJ9LHT8bBcWbQl9S4ZfN8syNN71HsqHWbqjdXUdtw0pGiC-05lkJwedf5MtKPLT6iZORT89165m6Ps5jI_BtjiX7upc-7ygHsRtiKjQ60c9w2uhIWpItdgwxHP4d2zZ2HerexrkgZ3yqlXZJuZasOWS5WkW7-raoaH5IuzWQFbx3DhhmiG2SKpdlgzskF1JGIF1dv-JSBg5l15wP4EZAozZf3ENuraP7qU0hp5DWP5CQ --insecure-skip-tls-verify=true"
            break
        default: 
            error("unknown region to create infra")
            break
    }
    return kubectl
}