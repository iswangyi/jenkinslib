import cn.com.rightcloud.jenkins.Constants

def call(dockerserver, scheduleserver,
         dbname, dbhost, dbport, dbuser, dbpass,
         redishost, redisport,
         mqhost, mqport, mquser, mqpass,
         containername) {

    dockerRun(dockerserver,
              scheduleserver,
              "-d " +
              Constants.DOCKER_LOG_OPT +
              "-e DB_NAME=${dbname} " +
              "-e DB_HOST=${dbhost} " +
              "-e DB_PORT=${dbport} " +
              "-e DB_USERNAME=${dbuser} " +
              "-e DB_PASSWORD=${dbpass} " +
              "-e REDIS_HOST=${redishost} " +
              "-e REDIS_PORT=${redisport} " +
              "-e MQ_HOST=${mqhost} " +
              "-e MQ_PORT=${mqport} " +
              "-e MQ_USERNAME=${mquser} " +
              "-e MQ_PASSWORD=${mqpass} " +
              "-e PROFILES_ACTIVE=cloudstar " +
              "-v /etc/localtime:/etc/localtime " +
              "--name ${containername}")
}
