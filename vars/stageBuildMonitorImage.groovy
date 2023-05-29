
/**
 * build server docker image
 *
 * @param dockerserver docker server used to build the image
 * @param repo server repo which contains the db sql, eg. cloudstar/rightcloud
 * @param branchortag branch or tag used to build
 * @param namespace namespace used for docker image tag, e.g. rightcloud/xindehui, the final tag image should be image.rightcloud.com.cn/${namespace}/rightcloud-server
 * @param checkcout should checkout source code
 */
def call(server_or_agent, dockerserver, repo, branchortag, namespace, istag, tagprefix=null, checkout=false) {
    def projectname
    def tagname
    def dirname = "./"
    if (checkout) {
        dirname = "tmp-rightloud-console"
    }

    def fulltag
    dir(dirname) {
        if (checkout) {
            stage("checkout source") {
                gitCheckout(repo, branchortag, istag)
            }

          /*  compileSchedule()*/
        }

        stage("build monitor image") {
            projectname = "image.rightcloud.com.cn/${namespace}/rightcloud-monitor"
            tagname = "v.${branchortag}."
            if (tagprefix?.trim()) {
                tagname = tagprefix
            }

            fulltag= "${projectname}/${server_or_agent}:${tagname}${env.BUILD_NUMBER}"

            if ( server_or_agent == "server" ) {
                dockerBuildPush(fulltag,
                                "monitor-server/docker/Dockerfile --no-cache ",
                                "--build-arg PROJECT=${repo} --build-arg BRANCH=${branchortag}")
            } else {
                dockerBuildPush(fulltag,
                                "monitor-agent/docker/Dockerfile --no-cache ",
                                "--build-arg PROJECT=${repo} --build-arg BRANCH=${branchortag}")
            }

        }
        if (checkout) {
            deleteDir()
        }
    }


    return fulltag
}
