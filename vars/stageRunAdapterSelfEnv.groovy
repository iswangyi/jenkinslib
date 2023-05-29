import cn.com.rightcloud.jenkins.Constants

def call(dockerserver, adapterserver,
         server, port,
         dbserver, dbport, dbuser, dbpass,
         falconserver, falcondbserver, falcondbuser, falcondbpass,
         mqhost, mqport, mquser, mqpass,
         redishost, redisport,
         portalport, cookiedomain,
         mongohost, mongoport, mongouser, mongopass,
         branch, username) {

    stage('run adapter') {
        def found = featureProject(branch)
        def DBNAME = "rightcloud"
        if (found) {
            DBNAME = "${found}rightcloud"
        }

        dockerRm(dockerserver, "rightcloud_adapter_${username}")
        runAdapter(dockerserver, adapterserver,
                   server, port,
                   DBNAME, dbserver, dbport, dbuser, dbpass,
                   falconserver, falcondbserver, falcondbuser, falcondbpass,
                   mqhost, mqport, mquser, mqpass,
                   redishost, redisport,
                   portalport, cookiedomain,
                   mongohost, mongoport, mongouser, mongopass,
                   "rightcloud_adapter_${username}"
        )
    }
}
