import cn.com.rightcloud.jenkins.Constants

def call(dockerserver,
         server, dbport,
         redishost,redisport,
         mqhost, mqport,
         username,
         branch, checkoutsource=false) {
    def workingdir = './'
    if (checkoutsource) {
        workingdir = 'schedule'
    }

    stage("check schedule") {
        def scheduleserver

        def dbname = "rightcloud"
        def found = featureProject(branch)
        if (found) {
            dbname = "${found}rightcloud"
        }

        dir(workingdir) {
            def container_name = "rightcloud_schedule_${username}"

            if (checkoutsource) {
                tryGitCheckout("cloudstar/rightcloud-schedule", branch)
            }

            compileSchedule()

            scheduleserver = dockerBuild(dockerserver,
                                         "rightcloud-schedule-${username}",
                                         "v.${branch}.",
                                         "docker/Dockerfile")
            dockerRm(dockerserver, container_name)

            runSchedule(dockerserver,
                        scheduleserver,
                        dbname,
                        server,
                        dbport,
                        Constants.SELF_ENV_DB_USER, Constants.SELF_ENV_DB_PASS,
                        redishost,
                        redisport,
                        mqhost,
                        mqport,
                        Constants.SELF_ENV_MQ_USER, Constants.SELF_ENV_MQ_PASS,
                        container_name)
        }
    }
}
