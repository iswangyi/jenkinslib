
/**
 * build db migration docker image
 *
 * @param dockerserver docker server used to build the image
 * @param repo server repo which contains the db sql, eg. cloudstar/rightcloud
 * @param branchortag branch or tag used to build
 * @param project project name used for docker image tag, e.g. rightcloud/xindehui, the final tag image should be image.rightcloud.com.cn/${project}/rightcloud-db-migration
 * @param checkcout should checkout source code
 */
def call(dockerserver, repo, branchortag, namespace, istag, tagprefix=null, checkout=false) {
    def projectname
    def tagname
    def dirname = "./"
    if (checkout) {
        dirname = "tmp-dbmigration-dir"
    }

    def fulltag
    dir(dirname) {
        if (checkout) {
            stage("checkout source") {
                gitCheckout(repo, branchortag, istag)
            }
        }

        stage("build dbmigration image") {
            projectname = "image.rightcloud.com.cn/${namespace}/rightcloud-db-migration"
            tagname = "v.${branchortag}."
            if (tagprefix?.trim()) {
                tagname = tagprefix
            }

            fulltag= "${projectname}:${tagname}${env.BUILD_NUMBER}"
            dockerBuildPush(fulltag,
                            "docker/DBDockerfile")
            // def dbmigration=dockerBuild(dockerserver,
            //                             projectname,
            //                             tagname,
            //                             "docker/DBDockerfile")

            // dockerPush(dockerserver, dbmigration)
            // dockerRmi(dockerserver, fulltag)
            //验证db
            validateDBMigration(fulltag,namespace)
        }

        if (checkout) {
            deleteDir()
        }
    }

   
    return fulltag
}
