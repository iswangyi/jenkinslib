import cn.com.rightcloud.jenkins.Constants

/*
 * run console container
 *
 * @param dockerserver docker server to run the container
 * @param consoleserver console ip address
 * @param port console port (http)
 * @param server cmp server ip/hostname
 * @param serverport cmp server https port
 * @param containername container name
 */
def call(dockerserver, consoleserver, port, server, serverport, containername) {
    if (containername == null) {
        containername = "rightcloud_console"
    }
    dockerRun(dockerserver, consoleserver,
              "-d -p ${port}:80 " +
              Constants.DOCKER_LOG_OPT +
              "-e CONSOLE_ADDRESS=${server} " +
              "-e CONSOLE_PORT=${port} " +
              "-e POC_ENV=true " +
              "-e PROTOCOL=http " +
              "-e UPSTREAM_SERVER=${server}:${serverport} " +
              "-e COOKIE_DOMAIN=${server} " +
              "-e PLATFORM_TYPE=private " +
              "-v /etc/localtime:/etc/localtime " +
              "--name ${containername}"
    )
}
