///

def call(k8sserver,
         namespace,
         branch, checkoutsource=false, groupname) {
    stage("check $namespace console") {
        def workingdir = './'
        if (checkoutsource) {
            workingdir = 'console'
        }

        def consoletagname
        def dockerserver = "tcp://10.68.7.93:2375"
        def branchname = branch

        def realgroupname = groupname.plus("/rightcloud-console")

        dir(workingdir) {
            if (checkoutsource) {
                branchname = tryGitCheckout(realgroupname, branch)
            }

            groupname = groupname.trim()
            if (!groupname.endsWith('/')) {
                groupname = groupname.plus('/')
            }
            consoletagname = stageBuildConsoleImageVue(groupname,
                                                       "rightcloud-console",
                                                       branchname,
                                                       namespace,
                                                       false, null, false)

            container('kubectl') {
                stageK8SRunConsole(k8sserver,
                                   namespace, consoletagname, true)
            }
        }
    }
}
