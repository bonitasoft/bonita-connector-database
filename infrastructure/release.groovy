def mvn(args) {
    sh "${tool 'maven'}/bin/mvn ${args}"
}

node {
    checkout scm

    stage('release version')
    mvn 'release:prepare -B -f bonita-connector-database-mssqlserver/pom.xml'
}