
/**
 * build ansible docker image
 *
 * @param dockerserver docker server used to build the image
 * @param repo server repo which contains the db sql, eg. cloudstar/rightcloud
 * @param branchortag branch or tag used to build
 * @param namespace namespace used for docker image tag, e.g. rightcloud/xindehui, the final tag image should be image.rightcloud.com.cn/${namespace}/rightcloud-server
 * @param checkcout should checkout source code
 */
def call(dockerserver, repo, branchortag, namespace, istag, tagprefix=null, checkout=false) {
    def projectname
    def tagname
    def dirname = "./"
    if (checkout) {
        dirname = "tmp-rightloud-analyzer"
    }

    dir(dirname) {
        if (checkout) {
            stage("checkout source") {
                gitCheckout(repo, branchortag, istag)
            }
        }

        stage("build analyzer image") {
            projectname = "image.rightcloud.com.cn/${namespace}/rightcloud-analyzer"
            tagname = "v.${branchortag}."
            if (tagprefix?.trim()) {
                tagname = tagprefix
            }
            fulltag= "${projectname}:${tagname}${env.BUILD_NUMBER}"
            dockerBuildPush(fulltag, "Dockerfile")

            // def app=dockerBuild(dockerserver,
            //                     projectname,
            //                     tagname,
            //                     "Dockerfile.alpine")

            // dockerPush(dockerserver, app)

        }

        if (checkout) {
            deleteDir()
        }
    }


    return "${projectname}:${tagname}${env.BUILD_NUMBER}"
}
