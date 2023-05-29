import cn.com.rightcloud.jenkins.Constants

def call(repo, branch) {
    try {
        gitCheckout(repo, branch)
        return branch
    } catch (e) {
        gitCheckout(repo, Constants.DEFAULT_SELF_ENV_BRANCH)
        return Constants.DEFAULT_SELF_ENV_BRANCH
    }
}
