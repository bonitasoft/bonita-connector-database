timestamps {
        node {
            stage('Setup') {
                configGitCredentialHelper()
                checkout scm
            }

            stage('Release version') {
                  withCredentials([usernamePassword(
                            credentialsId: 'github',
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                       sh './mvnw release:prepare -B -f bonita-connector-database-mssqlserver/pom.xml'
                  }
            }
        }
}
