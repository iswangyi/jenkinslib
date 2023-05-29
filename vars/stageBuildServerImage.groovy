
/**
 * build server docker image
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
        dirname = "tmp-rightloud-server"
    }

    def fulltag
    dir(dirname) {
        if (checkout) {
            stage("checkout source") {
                gitCheckout(repo, branchortag, istag)
            }

         /*   stage('Build server') {
                compileServer()
            }*/
        }

        stage("build server image") {
            projectname = "image.rightcloud.com.cn/${namespace}/rightcloud-server"
            tagname = "v.${branchortag}."
            if (tagprefix?.trim()) {
                tagname = tagprefix
            }
            fulltag = "${projectname}:${tagname}${env.BUILD_NUMBER}"
//            sh " sed -i 's/\$branchName/'${branchortag}'/g' docker/full_container_build/Dockerfile  "

            dockerBuildPush(fulltag,
                            "docker/Dockerfile",
                            "--build-arg SERVER_PROJECT=${repo}/ --build-arg BRANCH=${branchortag}")
            // def app=dockerBuild(dockerserver,
            //                     projectname,
            //                     tagname,
            //                     "docker/full_container_build/Dockerfile --no-cache")

            // dockerPush(dockerserver, app)

            // dockerRmi(dockerserver, fulltag)
        }

        if (checkout) {
            deleteDir()
        }

    }


    return fulltag
}
