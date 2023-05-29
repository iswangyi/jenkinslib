def call() {

    String[] dockerservers = ["dev.rd.rightcloud.com.cn",
                              "10.68.7.92",
                              "10.68.7.93",
                              "10.67.1.91",
                              "10.68.7.90",
                              "10.68.7.91",
    ]
    def num = env.BUILD_NUMBER as Integer

    def choice = dockerservers[num % dockerservers.size()]

    return "tcp://${choice}:2375"
}
