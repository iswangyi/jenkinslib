def call(server, port, user, password, schema) {
    def mvnHome = tool 'maven3.3.9'
    env.JAVA_HOME = tool 'java8.121'
    sh "${mvnHome}/bin/mvn -f db-service/pom.xml -Dflyway.url=\"jdbc:mysql://${server}:${port}/${schema}?useUnicode=true&characterEncoding=utf-8\" -Dflyway.user=${user} -Dflyway.password=${password} -Dflyway.schemas=${schema} flyway:migrate"
}
