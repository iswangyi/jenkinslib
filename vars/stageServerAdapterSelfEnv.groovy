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

        stage('Build server') {
            compileServer()
        }

        stage('build docker - server/adapter') {
            appserver = dockerBuild(dockerserver,
                                    "rightcloud-server-${username}",
                                    "v.${branch}.",
                                    "docker/Dockerfile.alpine")

            appadapter = dockerBuild(dockerserver,
                                     "rightcloud-adapter-${username}",
                                     "v.${branch}.",
                                     "docker/Dockerfile.adapter")

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
