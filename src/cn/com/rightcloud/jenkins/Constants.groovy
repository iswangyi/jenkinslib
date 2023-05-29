package cn.com.rightcloud.jenkins

class Constants {
    // self test env mapping table.
    static final SELF_ENV_MAPPING = [
        "liuxiaolu@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"liuxiaolu.rd.rightcloud.com.cn", httpport:8100],
        "gaoxin@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"gaoxin.rd.rightcloud.com.cn", httpport:8101],
        "yangsen@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"yangsen.rd.rightcloud.com.cn", httpport:8100],
        "shiwenqiang@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"shiwenqiang.rd.rightcloud.com.cn", httpport:8101],
        "liyi@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"liyi.rd.rightcloud.com.cn", httpport:8100],
        "wuhong@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"wuhong.rd.rightcloud.com.cn", httpport:8101],
        "huanghuajun@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"huanghuajun.rd.rightcloud.com.cn", httpport:8100],
        "gaofei@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"gaofei.rd.rightcloud.com.cn", httpport:8101],
        "baiwei@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"baiwei.rd.rightcloud.com.cn", httpport:8100],
        "liufujun@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"liufujun.rd.rightcloud.com.cn", httpport:8101],
        "chenzaichun@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"chenzaichun.rd.rightcloud.com.cn", httpport:8100],
        "jipeigui@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"jipeigui.rd.rightcloud.com.cn", httpport:8101],
        "zhangyuan@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"zhangyuan.rd.rightcloud.com.cn", httpport:8100],
        "wangbangquan@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"wangbangquan.rd.rightcloud.com.cn", httpport:8101],
        "taoyongshan@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"taoyongshan.rd.rightcloud.com.cn", httpport:8100],
        "maochaohong@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"maochaohong.rd.rightcloud.com.cn", httpport:8101],
        "qudehu@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"qudehu.rd.rightcloud.com.cn", httpport:8100],
        "xucong@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"xucong.rd.rightcloud.com.cn", httpport:8101],
        "wangchao@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"wangchao.rd.rightcloud.com.cn", httpport:8100],
        "dengyanan@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"dengyanan.rd.rightcloud.com.cn", httpport:8101],
        "zhuchunyu@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"zhuchunyu.rd.rightcloud.com.cn", httpport:8100],
        "lixun@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"lixun.rd.rightcloud.com.cn", httpport:8101],
        "hexiu@cloud-star.com.cn": [mysqlport:3316, redisport:6389, mqport:5682, mqmgrport:15682, mongoport:27027, port:8555, server:"hexiu.rd.rightcloud.com.cn", httpport:8100],
        "zengyong@cloud-star.com.cn": [mysqlport:3317, redisport:6390, mqport:5683, mqmgrport:15683, mongoport:27028, port:8556, server:"zengyong.rd.rightcloud.com.cn", httpport:8101],
    ]

    //define dingding env token
    // v3.7.0 b515ad6b1984a0fe29d62d5ee25814aa147fe63356552b4aff355c6ae396e7ab
    static final DINGDING_ENV_TOKEN = [
            "test-env": [token:'28acce75353cecca59d0b048b7c7d177bad98ee354d46a55da4ae91dbf2bb79f'],
            "vue-project": [token: '37121cf5efc5cfec5cf3aab98b635fe4e297b3d85c83cb02824c548fe47f24df'],
            "aliyuncost-project": [token: 'b285205e7a572243b58bba6e15ee0ad803d2304cf4d39a38dca5fe4332aa2444'],
            "gzw-project": [token: 'f53bbc0604c6ef1568d7fad85d50e0c69e28e392390e0549a452f14bfc5b6eae'],
            "cloud-boss": [token: '36db486cf261a57174ba02a86901689939308817b386cddca130eb60fbe7610a'],
    ]

    static final BUILD_NOTIFY_PEOPEL = "15111824527,19942225639,19923587380"

    // quality build :
    static final QUALITY_BUILD_NOTIFY_PEOPEL = "15823512727,15823571244,18908338113"

    static final BUILD_SUCCESS_MESSAGE = "构建成功,开始测试吧！"

    static final BUILD_FAILURE_MESSAGE = "构建失败，速速检查！"

    static final QUALITY_GATE_FAILURE_MESSAGE = "质量验证未通过！"

    static final TEST_ENV = "vue-project"

    // default mail cc:
    static final DEFAULT_CC = "cc:maxiao@cloud-star.com.cn, cc:shiwenqiang@cloud-star.com.cn, cc:wuxiaobing@cloud-star.com.cn, cc:maochaohong@cloud-star.com.cn, cc:jipeigui@cloud-star.com.cn, cc:rdc_test@cloud-star.com.cn"

    // default docker opt
    static final DOCKER_LOG_OPT = "--log-driver json-file --log-opt max-size=100m --log-opt max-file=3 "

    // default branch used if branch not found for building self env
    static final DEFAULT_SELF_ENV_BRANCH = "release-test3.2.0"

    static final DEFAULT_GIT_CREDENTIAL = 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2'

    static final SELF_ENV_DB_USER = "root"
    static final SELF_ENV_DB_PASS = "root-mysql-pw"
    static final SELF_ENV_FALCON_DB_USER = "root"
    static final SELF_ENV_FALCON_DB_PASS = ""
    static final SELF_ENV_MQ_USER = "admin"
    static final SELF_ENV_MQ_PASS = "1"
    static final SELF_ENV_MONGO_USER = "rightcloud"
    static final SELF_ENV_MONGO_PASS = "H89lBgAg"

    static final PRODUCT_K8S_NFS = "10.69.0.57"
    static final OEM_K8S_NFS = "10.69.5.60"
    static final PRODUCT_K8S_SERVER = "10.69.0.51:8080"
    static final SELFENV_K8S_NFS = "10.68.7.6"
    static final SELFENV_K8S_SERVER = "10.68.7.7:8080"
    static final DEV_K8S_NFS = "10.67.7.18"

}

