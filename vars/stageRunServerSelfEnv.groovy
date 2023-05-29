import cn.com.rightcloud.jenkins.Constants

def call(dockerserver, appserver,
         server, port,
         dbserver, dbport, dbuser, dbpass,
         falconserver, falcondbserver, falcondbuser, falcondbpass,
         mqhost, mqport, mquser, mqpass,
         redishost, redisport,
         portalport, cookiedomain,
         mongohost, mongoport, mongouser, mongopass,
         branch, username) {

    stage('run server') {
        def found = featureProject(branch)
        def DBNAME = "rightcloud"
        if (found) {
            DBNAME = "${found}rightcloud"
        }

        dockerRm(dockerserver, "rightcloud_server_${username}")
        runServer(dockerserver, appserver,
                  server, port,
                  DBNAME, dbserver, dbport, dbuser, dbpass,
                  falconserver, falcondbserver, falcondbuser, falcondbpass,
                  mqhost, mqport, mquser, mqpass,
                  redishost, redisport,
                  portalport, cookiedomain,
                  mongohost, mongoport, mongouser, mongopass,
                  "rightcloud_server_${username}"
        )
    }
}
