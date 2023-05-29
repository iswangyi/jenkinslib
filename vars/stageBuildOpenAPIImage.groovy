
/**
 * build openapi docker image
 *
 * @param group openapi group, cloudstart/, ProjectGroup/GABigData/...
 * @param repo openapi repo rightcloud-openapi, gabigdata-console
 * @param branchortag branch or tag used to build
 * @param namespace namespace used for docker image tag, e.g. rightcloud/xindehui, the final tag image should be image.rightcloud.com.cn/${namespace}/rightcloud-openapi
 * @param checkcout should checkout source code
 */
def call(group, repo, branchortag, namespace, istag, tagprefix=null, checkout=false) {
    def projectname
    def tagname
    def dirname = "./"
    if (checkout) {
        dirname = "tmp-${repo}"
    }

    def dockerserver = getDockerBuildServer()

    echo "docker build server is: ${dockerserver}"

    def repourl = "${group}${repo}"

    def fulltag
    dir(dirname) {
        if (checkout) {
            stage("checkout source") {
                gitCheckout(repourl, branchortag, istag)
            }
        }

        stage("build openapi image") {
            projectname = "image.rightcloud.com.cn/${namespace}/${repo}"
            tagname = "v.${branchortag}."
            if (tagprefix?.trim()) {
                tagname = tagprefix
            }

            fulltag= "${projectname}:${tagname}${env.BUILD_NUMBER}"
            echo "fulltag is: ${fulltag}"
            dockerBuildPush(fulltag, "docker/Dockerfile",
                            "--build-arg groupName=${group} --build-arg branchName=${branchortag} --build-arg projectName=${repo}")
        }

        if (checkout) {
            deleteDir()
        }
    }


    return fulltag
}
