
def call(k8sserver, namespace, branch, checkoutsource=false) {
    def ansibletagname

    stage("check $namespace ansible") {
        def workingdir = './'
        if (checkoutsource) {
            workingdir = 'ansible'
        }

        def branchname = branch
        def dockerserver="tcp://dev.rd.rightcloud.com.cn:2375"

        dir(workingdir) {
            if (checkoutsource) {
                branchname = tryGitCheckout("rightcloud/ansible-adapter", branch)
            }

            ansibletagname = stageBuildAnsibleImage(dockerserver,
                                                    "rightcloud/ansible-adapter",
                                                    branchname,
                                                    namespace,
                                                    true, null, false)

            container('kubectl') {
                stageK8SRunAnsible(k8sserver,
                                   namespace, ansibletagname, true)
            }
        }
    }

}
