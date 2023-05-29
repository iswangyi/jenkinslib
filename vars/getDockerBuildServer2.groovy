def call() {
    def ret = httpGet("http://10.68.7.92:8111/")
    return "${ret}"
}
