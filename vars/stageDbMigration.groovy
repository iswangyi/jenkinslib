def call(dbhost, dbport,
         dbuser, dbpassword, branch) {
    stage('upgrade database') {
        def dbname = "rightcloud"
        def found = featureProject(branch)
        if (found) {
            dbname = "${found}rightcloud"
            echo "branch is ${branch}, use prefix $found for db migration"
        } else {
            echo "branch name not match feature-*-*, use rightcloud for db migration"
        }

        dbMigration(dbhost, dbport,
                    dbuser, dbpassword,
                    dbname)
    }
}
