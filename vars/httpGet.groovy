def call(url) {
    def get = new URL(url).openConnection();
    def getRC = get.getResponseCode();
    def ret
    if(getRC.equals(200)) {
        ret = get.getInputStream().getText();
        echo "httpGet ${url} return: ${ret}"
    } else {
        error ("httpGet failed. ${url}, ret: ${getRC}")
    }

    return ret
}
