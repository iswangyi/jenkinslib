def call(repo, branch, istag=false) {
    if (istag) {
        checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "http://10.68.6.20:8082/${repo}.git", credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2']], branches: [[name: branch]]], poll: false
    } else {
        git credentialsId: 'bdfdd9e4-1bd4-4332-a46e-b78347a61eb2', url: "http://10.68.6.20:8082/${repo}.git", branch: branch
    }
}
