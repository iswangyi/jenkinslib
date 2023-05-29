import cn.com.rightcloud.jenkins.Constants

// k8s_env  project、dev、demo(prod)、self_test
def call(namespace) {
    def env
    switch(namespace) {
        case 'cmp-dev-a':
            env = 'self_env'
            break
        case 'cmp-dev-b':
            env = 'project'
            break
        case 'cmp-dev-c':
            env = 'self_env'
            break
        case 'bss':
            env = 'self_env'
            break
        case 'cmdb':
            env = 'self_env'
            break
        case 'cmdb-project':
            env = 'project'
            break
        default:
            error("unknown namespace")
            break
    }
    return env
}
