import cn.com.rightcloud.jenkins.Constants

def call(success=true, env, message, notifyPeople) {
    stage('send dingding message') {
        def envInfo = Constants.DINGDING_ENV_TOKEN.get(env)
        if (envInfo == null) {
            echo "notifyDingDing error!-----------------------------------------failed to get the dingding env : " + env
            return
        }
        def token = envInfo.get("token")
        def  imageUrl = 'https://www.easyicon.net/download/png/4740/72/'
        if (success) {
            imageUrl = 'https://www.easyicon.net/download/png/27967/72/'
        }
        dingTalk accessToken: token, imageUrl: imageUrl, jenkinsUrl: 'http://jenkins.rctest.com/', messageTitle: "构建消息", message: message, notifyPeople: notifyPeople

    }
}
