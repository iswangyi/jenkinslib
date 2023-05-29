def call(k8sserver, namespace, app) {
    stage("Check ${app} is running") {
        def totalwaittime = 3600
        totalwaittime = "${params.WAIT_TIME}" as Integer

        timeout(time: totalwaittime+20, unit: 'SECONDS') {
            def isrunning = isPodRunningOnK8S(k8sserver,
                                              namespace,
                                              app)
            if (isrunning) {
                notifySelfEnvRunning("${namespace}@cloud-star.com.cn", namespace)
            }

            def step = 1
            def waittime = 0

            while (isrunning) {
                sleep(step)
                isrunning = isPodRunningOnK8S(k8sserver,
                                              namespace,
                                              app)
                waittime += step
                if (waittime >= totalwaittime) {
                    notifySelfEnvJobCanceled("${namespace}@cloud-star.com.cn", namespace)
                    echo "Time out, job canceled"
                    currentBuild.result = 'ABORTED'
                    return;
                }
            }
        }
    }
}
