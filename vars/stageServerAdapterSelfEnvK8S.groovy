import cn.com.rightcloud.jenkins.Constants

def call(k8sserver,
         branch, namespace, checkoutsource=false, groupname) {

    def dbmigrationtagname
    def servertagname
    def adaptertagname
    def buildserver = "tcp://10.68.7.93:2375"
    def workingdir = './'
    if (checkoutsource) {
        workingdir = 'server'
    }

    def realgroupname = groupname.plus("/rightcloud")

    def branchname=branch
    dir(workingdir) {
        if (checkoutsource) {
            branchname = tryGitCheckout(realgroupname, branch)
        }

        dbmigrationtagname = stageCreateDBMigration(buildserver, realgroupname,
                                                    branchname,
                                                    namespace,
                                                    false, null, false)

        servertagname = stageBuildServerImage(buildserver, realgroupname,
                                              branchname,
                                              namespace,
                                              false, null, false)
        adaptertagname = stageBuildAdapterImage(buildserver, realgroupname,
                                                branchname,
                                                namespace,
                                                false, null, false)

        //container('kubectl') {
            stageK8SRunServer(k8sserver,
                              namespace,
                              dbmigrationtagname,
                              servertagname, true)
            stageK8SRunAdapter(k8sserver,
                               namespace,
                               adaptertagname, true)
        //}
    }
}
