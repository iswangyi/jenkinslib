///

def call(dockerserver,
         consoleport,
         server,
         port,
         username,
         branch, checkoutsource=false) {
    stage("check user console") {
        def workingdir = './'
        if (checkoutsource) {
            workingdir = 'console'
        }

        dir(workingdir) {
            def consoleserver
            def container_name = "rightcloud_console_${username}"

            if (checkoutsource) {
                tryGitCheckout("cloudstar/rightcloud-portal", branch)
            }

            consoleserver = dockerBuild(dockerserver,
                                        "rightcloud-console-${username}",
                                        "v.${branch}.",
                                        "docker/ConsoleDockerfile")

            dockerRm(dockerserver, container_name)

            runConsole(dockerserver,
                       consoleserver,
                       consoleport,
                       server,
                       port,
                       container_name)
        }
    }
}
