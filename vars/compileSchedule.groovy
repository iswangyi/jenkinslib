def call() {
    def mvnHome = tool 'maven3.3.9'
    env.JAVA_HOME = tool 'java8.121'
    sh "${mvnHome}/bin/mvn clean package -Dmaven.test.skip=true -U"
}
