def call(dockerserver, username, component) {
    stage('Check is running') {
        def totalwaittime = 3600
        totalwaittime = "${params.WAIT_TIME}" as Integer

        timeout(time: totalwaittime, unit: 'SECONDS') {
            def isrunning = isContainerRunning(dockerserver,
                                               "rightcloud_${component}_${username}")
            if (isrunning) {
                notifySelfEnvRunning("${username}@cloud-star.com.cn", username)
            }

            def step = 1
            def waittime = 0

            while (isrunning) {
                sleep(step)
                isrunning = isContainerRunning(dockerserver,
                                               "rightcloud_${component}_${username}")
                waittime += step
                if (waittime >= totalwaittime) {
                    notifySelfEnvJobCanceled("${username}@cloud-star.com.cn", username)
                    error("Time out, job canceled")
                }
            }
        }
    }
}
