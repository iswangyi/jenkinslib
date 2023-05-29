import cn.com.rightcloud.jenkins.Constants

def call(k8sserver,
         branch, namespace, checkoutsource=false, groupname) {

    def dbmigrationtagname
    def adaptertagname
    def buildserver = "tcp://10.68.7.93:2375"
    def workingdir = './'
    if (checkoutsource) {
        workingdir = 'adapter'
    }

    def realgroupname = groupname.plus("/rightcloud-adapter")

    def branchname=branch
    dir(workingdir) {
        if (checkoutsource) {
            branchname = tryGitCheckout(realgroupname, branch)
        }

        adaptertagname = stageBuildAdapterImage(buildserver, realgroupname,
                                                branchname,
                                                namespace,
                                                false, null, false)

        stageK8SRunAdapter(k8sserver,
                           namespace,
                           adaptertagname, true)
    }
}
