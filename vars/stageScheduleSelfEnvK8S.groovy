import cn.com.rightcloud.jenkins.Constants

def call(k8sserver,
         namespace,
         branch, checkoutsource=false, groupname) {
    def workingdir = './'
    if (checkoutsource) {
        workingdir = 'schedule'
    }

    def scheduletagname
    def dockerserver = "tcp://10.68.7.93:2375"

    stage("check schedule") {
        def scheduleserver

        def dbname = "rightcloud"
        def found = featureProject(branch)
        if (found) {
            dbname = "${found}rightcloud"
        }

        def branchname = branch
        def realgroupname = groupname.plus("/rightcloud-schedule")
        dir(workingdir) {
            if (checkoutsource) {
                branchname = tryGitCheckout(realgroupname, branchname)
            }

            scheduletagname = stageBuildScheduleImage(dockerserver, realgroupname,
                                                      branchname,
                                                      namespace,
                                                      false, null, false)
            container('kubectl') {
                stageK8SRunSchedule(k8sserver,
                                    namespace,
                                    scheduletagname, true)
            }
        }
    }
}
