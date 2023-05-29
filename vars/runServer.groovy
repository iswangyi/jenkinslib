import cn.com.rightcloud.jenkins.Constants

def call(dockerserver, appserver,
         server, port,
         dbname, dbserver, dbport, dbuser, dbpass,
         falconserver, falcondbserver, falcondbuser, falcondbpass,
         mqhost, mqport, mquser, mqpass,
         redishost, redisport,
         portalport, cookiedomain,
         mongohost, mongoport, mongouser, mongopass,
         container_name) {

    dockerRun(dockerserver, appserver,
              "-d -p ${port}:8443 " +
              Constants.DOCKER_LOG_OPT +
              "-e DB_NAME=${dbname} " +
              "-e DB_URL=${server}:${dbport} " +
              "-e DB_PASSWORD=${dbpass} " +
              "-e DB_USER=${dbuser} " +
              "-e FALCON_DB_URL=${falcondbserver} " +
              "-e FALCON_DB_PASSWORD=${falcondbpass} " +
              "-e RABBIT_MQ_URL=${mqhost} " +
              "-e RABBIT_MQ_PORT=${mqport} " +
              "-e REDIS_URL=${redishost}:${redisport} " +
              "-e HOST_IP=${server} " +
              "-e AGENT_SERVERS=${server}:${port} " +
              "-e FALCON_HEART_ADDR=${falconserver}:6030 " +
              "-e FALCON_TRANSFER_ADDR=${falconserver}:8433 " +
              "-e FALCON_TRANSFER_ADDR_HTTP=${falconserver}:6060 " +
              "-e QUERY_HISTORY_URL=http://${falconserver}:9966/graph/history " +
              "-e QUERY_LAST_URL=http://${falconserver}:9966/graph/last " +
              "-e FALCON_FE_URL=http://${falconserver}:1234 " +
              "-e FALCON_PORTAL_URL=http://${falconserver}:5050 " +
              "-e PORTAL_URL=https://${server}:${portalport} " +
              "-e COOKIE_DOMAIN=${cookiedomain} " +
              "-e MONGODB_HOST=${mongohost} " +
              "-e MONGODB_USERNAME=${mongouser} " +
              "-e MONGODB_PASSWORD=${mongopass} " +
              "-e MONGODB_PORT=${mongoport} " +
              "-v /etc/localtime:/etc/localtime " +
              "-e PLATFORM_TYPE=private " +
              "--name ${container_name}")
}
