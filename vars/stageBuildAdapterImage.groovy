
/**
 * build adapter docker image
 *
 * @param dockerserver docker server used to build the image
 * @param repo server repo which contains the db sql, eg. cloudstar/rightcloud
 * @param branchortag branch or tag used to build
 * @param namespace namespace used for docker image tag, e.g. rightcloud/xindehui, the final tag image should be image.rightcloud.com.cn/${namespace}/rightcloud-adapter
 * @param checkcout should checkout source code
 */
def call(dockerserver, repo, branchortag, namespace, istag, tagprefix=null, checkout=false) {
    def projectname
    def tagname
    def dirname = "./"
    if (checkout) {
        dirname = "tmp-rightloud-adapter"
    }

    def fulltag
    dir(dirname) {
        if (checkout) {
            stage("checkout source") {
                gitCheckout(repo, branchortag, istag)
            }
        }

        stage("build adapter image") {
            projectname = "image.rightcloud.com.cn/${namespace}/rightcloud-adapter"
            tagname = "v.${branchortag}."
            if (tagprefix?.trim()) {
                tagname = tagprefix
            }
            fulltag= "${projectname}:${tagname}${env.BUILD_NUMBER}"
//            sh " sed -i 's/\$branchName/'${branchortag}'/g' docker/full_container_build/Dockerfile  "

            dockerBuildPush(fulltag,
                            "docker/full_container_build/Dockerfile --target build-adapter ",
            "--build-arg PROJECT=${repo} --build-arg BRANCH=${branchortag}")
        }

        if (checkout) {
            deleteDir()
        }
    }

    return fulltag
}
