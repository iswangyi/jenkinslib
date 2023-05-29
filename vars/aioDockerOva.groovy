def notifySuccessful(share_info) {
    emailext (
        subject: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        body: """<p>SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Please download ova install package ${params.PACKAGE_VERSION}.tar.gz via "<a href='${share_info}'>'${share_info}'</a>" </p>""",
        replyTo: "${env.INSTALL_PACKAGE_REPLYTO}",
        to: "${env.INSTALL_PACKAGE_RECIPIENTS}",
        attachLog: true
        //recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
    )
}
def call() {
    node {
        properties([parameters([
            string(name: "IMAGE_TAG", defaultValue: 'image.rightcloud.com.cn/rightcloud/allinone:v.d.1.81',
                   description: "image tag use to build the ova"),
            string(name: "BUILD_SERVER", defaultValue: '10.69.0.234',
                   description: "server used to build the ova"),
            string(name: "VERSION_PREFIX", defaultValue: '3.0.',
                   description: "version prefix"),
            booleanParam(defaultValue: false, description: 'import ova for testing?', name: 'IS_IMPORT_OVA'),
            string(name: "IP_FOR_VM", defaultValue: '10.62.0.121', description: "vm ip addres for testing"),
            booleanParam(defaultValue: false, description: 'use repo mirror?', name: 'IS_USE_REPOMIRROR'),
        ])])

        try {
            def version="${params.VERSION_PREFIX}"
            def cmdparam = ""
            def name="rightcloud_cmp_${version}"

            if (params.IS_IMPORT_OVA) {
                cmdparam += " -i -p ${params.IP_FOR_VM} "
            }

            if (params.IS_USE_REPOMIRROR) {
                cmdparam +=  " -r "
                name="rightcloud_cmp_r_${version}"
            }


            stage ("call script") {
                sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                    // sh "ssh -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} bash <(curl -fSsL --request GET --header 'PRIVATE-TOKEN: 3srGP9AmSuEWe3YzJxVs' http://10.68.6.20:8082/chenzaichun/allinone-docker/raw/master/ova/build_ova.sh) -v ${version} ${cmdparam} "
                    sh "ssh -T -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} \"curl -fSsL --request GET --header 'PRIVATE-TOKEN: j-2w5318uPTixK6TLYQ5' http://10.68.6.20:8082/cloudstar/allinone-docker/raw/feature-rightcloud-monitor/ova/build_ova.sh | bash -s -- -v ${version} ${cmdparam} -t ${params.IMAGE_TAG}\" "
                }
            }

            stage('upload to baidu wangpan') {
                def share_info
                sshagent(['ssh-key-10.69.0.234-aio-dev']) {
                    sh "ssh -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} \"curl --header 'PRIVATE-TOKEN: j-2w5318uPTixK6TLYQ5' http://10.68.6.20:8082/cloudstar/infrastructure-ops/raw/master/upload-baidu/upload.sh| bash -s -- -b ${env.BAIDU_BDUSS} -s /root/govc/${name}.ova -d /POC\" "
                    share_info = sh(returnStdout: true, script: "ssh -o StrictHostKeyChecking=no -l root ${params.BUILD_SERVER} \"cat /tmp/share_link.txt\" ").trim()
                }
                notifySuccessful("${share_info}")
            }

            // stage ("clone the vm from the template") {
            //     sshagent(['ssh-key-10.69.0.234-aio-dev']) {
            //         sh "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} bash -c \"export GOVC_USERNAME=\"rightcloud_dev@vsphere.local\"; export GOVC_PASSWORD='Rightcloud@dev123'; export GOVC_URL=10.62.0.100; export GOVC_INSECURE=true; export GOVC_GUEST_LOGIN=root:Root-123;  /usr/bin/govc vm.clone -pool=/RightCloud-CQ/host/rc-test/Resources -ds=POC-CQ-26-Local-1.6T -folder=rightcloud_dev -vm /RightCloud-CQ/vm/rightcloud_dev/rightcloud-baseOS-centos7-docker-template -waitip=true ${vm_name} \""
            //     }
            // }

            // stage ("provison the env") {
            //     sshagent(['ssh-key-10.69.0.234-aio-dev']) {
            //         // wait docker to start
            //         sh "ssh -o StrictHostKeyChecking=no -l root ${IP_FOR_VM} bash -c \"while /bin/true; if  docker ps &>/dev/null; then break; else sleep 1; fi\""

            //         sh "ssh -o StrictHostKeyChecking=no -l root ${IP_FOR_VM} bash -c \"mkdir -p /usr/local/rightcloud && curl --request GET --header 'PRIVATE-TOKEN: 3srGP9AmSuEWe3YzJxVs' http://10.68.6.20:8082/chenzaichun/allinone-docker/raw/master/ova/rightcloud -o /etc/init.d/rightcloud && curl --request GET --header 'PRIVATE-TOKEN: 3srGP9AmSuEWe3YzJxVs' http://10.68.6.20:8082/chenzaichun/allinone-docker/raw/master/ova/rightcloud_env -o /usr/local/rightcloud/rightcloud_env && docker pull ${params.IMAGE_TAG} && halt -p\""
            //     }
            // }

            // stage ("export ovf") {
            //     sshagent(['ssh-key-10.69.0.234-aio-dev']) {
            //         def powerstate = sh(script: "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} export GOVC_USERNAME='rightcloud_dev@vsphere.local'; export GOVC_PASSWORD='Rightcloud@dev123'; export GOVC_URL=10.62.0.100; export GOVC_INSECURE=true; export GOVC_GUEST_LOGIN=root:Root-123; /usr/bin/govc vm.info \"${vm_name}\" | grep \"Power state\"|awk -F':' '{print \$2}'",returnStdout: true).trim()

            //         while (powerstate != "poweredOff") {
            //             sleep(1)
            //             powerstate = sh(script: "ssh -o StrictHostKeyChecking=no -l root ${BUILD_SERVER} export GOVC_USERNAME='rightcloud_dev@vsphere.local'; export GOVC_PASSWORD='Rightcloud@dev123'; export GOVC_URL=10.62.0.100; export GOVC_INSECURE=true; export GOVC_GUEST_LOGIN=root:Root-123; /usr/bin/govc vm.info \"${vm_name}\" | grep \"Power state\"|awk -F':' '{print \$2}'",returnStdout: true).trim()
            //         }

            //         sh "ssh -o StrictHostKeyChecking=no -l root ${IP_FOR_VM} /usr/bin/govc export.ovf -vm ${vm_name} -sha=1 -f=true /root//usr/bin/govc/"

            //         sh "ssh -o StrictHostKeyChecking=no -l root ${IP_FOR_VM} /usr/bin/govc export.ovf -vm ${vm_name} -sha=1 -f=true /root//usr/bin/govc/"
            //         def uuid=sh(script:"ssh -o StrictHostKeyChecking=no -l root ${IP_FOR_VM} export GOVC_USERNAME='rightcloud_dev@vsphere.local'; export GOVC_PASSWORD='Rightcloud@dev123'; export GOVC_URL=10.62.0.100; export GOVC_INSECURE=true; export GOVC_GUEST_LOGIN=root:Root-123; /usr/bin/govc vm.info -e -t ${VM_NAME} |grep UUID |awk '{print \$2}'", returnStdout: true).trim()

            //         sh "ssh -o StrictHostKeyChecking=no -l root ${IP_FOR_VM} /usr/bin/govc vm.destroy ${uuid}"
            //     }
            // }

            // stage("set ova version/vm ip") {
            //     sshagent(['ssh-key-10.69.0.234-aio-dev']) {
            //         // some block
            //         sh "ssh -o StrictHostKeyChecking=no -l root 10.69.0.234 sed -i \"s|<Version>.*</Version>|<Version>$version</Version>|g\" /root//usr/bin/govc/rightcloud-baseOS-centos7-docker-template/rightcloud-baseOS-centos7-docker-template.ovf"
            //         sh "ssh -o StrictHostKeyChecking=no -l root 10.69.0.234 /usr/bin/govc import.spec /root//usr/bin/govc/rightcloud-baseOS-centos7-docker-template > /root/spec.json"
            //         sh "ssh -o StrictHostKeyChecking=no -l root 10.69.0.234 sed -i \"s|10.62.0.119|${params.IP_FOR_VM}|g\" /root//usr/bin/govc/spec.json"
            //     }

            // }
            // stage("import ova") {
            //     echo "haha"
            //     sshagent(['ssh-key-10.69.0.234-aio-dev']) {
            //         // some block

            //         sh "ssh -o StrictHostKeyChecking=no -l root 10.69.0.234 .//usr/bin/govc import.ovf -ds=POC-CQ-26-Local-1.6T -folder=rightcloud_dev -pool=/RightCloud-CQ/host/rc-test/Resources -options=/root//usr/bin/govc/spec.json -name aio-import-jenkins /root//usr/bin/govc/rightcloud-baseOS-centos7-docker-template/"
            //     }
            // }
        } catch (e) {
            println(e);
            println(e.getMessage());
            println(e.getStackTrace());
            // notifyFailed(mail)
            error(e.getMessage())
        }
    }
}
