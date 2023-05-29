import cn.com.rightcloud.jenkins.Constants

def call(dockerserver,
         server, port,
         dbserver, dbport, 
         falconserver, falcondbserver,
         mqhost, mqport,
         redishost, redisport,
         portalport, cookiedomain,
         mongohost, mongoport, mongouser, mongopass,
         branch, username, checkoutsource=false) {

    def workingdir = './'
    if (checkoutsource) {
        workingdir = 'server'
    }

    def appserver;
    def appadapter;

    dir(workingdir) {
        if (checkoutsource) {
            tryGitCheckout("cloudstar/rightcloud", branch)
        }

        stage('build docker - server/adapter') {

            sh " sed -i 's/\$branchName/'${branch}'/g' docker/full_container_build/*   "

           appserver = dockerBuild(dockerserver,
                                    "rightcloud-server-${username}",
                                    "v.${branch}.",
                                    "docker/full_container_build/Dockerfile --no-cache")
           appadapter = dockerBuild(dockerserver,
                    "rightcloud-adapter-${username}",
                    "v.${branch}.",
                    "docker/full_container_build/Dockerfile --target build-adapter ")
           /* appserver = dockerBuild(dockerserver,
                    "rightcloud-server-${username}",
                    "v.${branch}.",
                    "docker/full_container_build/Dockerfile.server --no-cache")

            appadapter = dockerBuild(dockerserver,
                    "rightcloud-adapter-${username}",
                    "v.${branch}.",
                    "docker/full_container_build/Dockerfile.adapter --no-cache")*/

        }

        stageDbMigration(dbserver, dbport,
                         Constants.SELF_ENV_DB_USER, Constants.SELF_ENV_DB_PASS,
                         branch)

        stageRunServerSelfEnv(dockerserver, appserver,
                              server, port,
                              dbserver, dbport, Constants.SELF_ENV_DB_USER, Constants.SELF_ENV_DB_PASS,
                              falconserver, falcondbserver, Constants.SELF_ENV_FALCON_DB_USER, Constants.SELF_ENV_FALCON_DB_PASS,
                              mqhost, mqport, Constants.SELF_ENV_MQ_USER, Constants.SELF_ENV_MQ_PASS,
                              redishost, redisport,
                              portalport, cookiedomain,
                              mongohost, mongoport, mongouser, mongopass,
                              branch, username)
        stageRunAdapterSelfEnv(dockerserver, appadapter,
                               server, port,
                               dbserver, dbport, Constants.SELF_ENV_DB_USER, Constants.SELF_ENV_DB_PASS,
                               falconserver, falcondbserver, Constants.SELF_ENV_FALCON_DB_USER, Constants.SELF_ENV_FALCON_DB_PASS,
                               mqhost, mqport, Constants.SELF_ENV_MQ_USER, Constants.SELF_ENV_MQ_PASS,
                               redishost, redisport,
                               portalport, cookiedomain,
                               mongohost, mongoport, mongouser, mongopass,
                               branch, username)
    }

}
