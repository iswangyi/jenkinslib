
def call(project) {
    def gettags = ("git ls-remote -t -h http://oauth2:bkWrchPdvKi2pT-MshEo@10.68.6.20:8082/${project}.git */*").execute()

    return gettags.text.readLines()
        .collect { it.split()[1].replaceAll('refs/heads/', '')  }
        .unique()
}
