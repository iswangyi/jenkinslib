def call(String dbmigrationTag, String namespace, mysqlUrl = 'vue-project.rc.com', k8sEnv = 'dev') {
    def app
    def mysqluser
    def mysqlpwd
    def mysqlport
    def kubectl = getKubectl(k8sEnv)
    container('kubectl') {
        def user = sh(script: """
           echo \$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_USERNAME\" | awk -F':' '{print \$2}' | tr -d ' ');
          """,returnStdout: true).trim();

        def pwd = sh(script: """
           echo \$(${kubectl} -n ${namespace} get configmap -o yaml | grep \"[[:space:]]DB_PASSWORD\" | awk -F':' '{print \$2}' | tr -d ' ');
          """,returnStdout: true).trim();
        
        def port
        if  (namespace.trim() == "boss-test" && k8sEnv.trim() == "dev") {
            port = sh(script: """
               echo \$(${kubectl} -n ${namespace} get svc | grep "proxy-sql-cluster" | awk -F ':' '{print \$2}' | awk -F '/' '{print \$1}');
              """,returnStdout: true).trim();
        } else {
            port = sh(script: """
               echo \$(${kubectl} -n ${namespace} get svc | grep "rightcloud-mysql" | awk -F ':' '{print \$2}' | awk -F '/' '{print \$1}');
              """,returnStdout: true).trim();
        }

        mysqluser = "${user}"
        mysqlpwd = "${pwd}"
        mysqlport = "${port}"

        def result = sh(
                script:"""
                 flyway \
                 -table=schema_version \
                 -outOfOrder=true \
                 -locations=filesystem:db-service/src/main/resources/db/migrations \
                 -ignoreMissingMigrations=true \
                 -ignorePendingMigrations=true \
                 -placeholderReplacement=true \
                 -placeholderPrefix=#[ \
                 -placeholderSuffix=] \
                 -url='jdbc:mysql://${mysqlUrl}:${mysqlport}/rightcloud?useUnicode=true&characterEncoding=utf-8&useSSL=false' -schemas=rightcloud -user=${mysqluser} -password=${mysqlpwd} validate
                  """,
                returnStdout: true,
        ).trim()
        echo "sh result " + result
    }
//    container('docker') {
//       def result = sh(
//           script:"""
//                 docker run --rm \
//                  ${dbmigrationTag} \
//                  -table=schema_version \
//                  -outOfOrder=true \
//                  -ignoreMissingMigrations=true \
//                  -ignorePendingMigrations=true \
//                  -placeholderReplacement=true \
//                  -placeholderPrefix=#[ \
//                  -placeholderSuffix=] \
//                  -url='jdbc:mysql://${mysqlUrl}:${mysqlport}/rightcloud?useUnicode=true&characterEncoding=utf-8&useSSL=false' -schemas=rightcloud -user=${mysqluser} -password=${mysqlpwd} validate
//                  """,
//               returnStdout: true,
//        ).trim()
//        echo "sh result " + result
//    }


    return app
}
