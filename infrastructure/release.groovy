timestamps {
        node {
            stage('Setup') {
                checkout scm
            }

            stage('Release version') {
                  withCredentials([usernamePassword(
                            credentialsId: 'github',
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                       sh """
                           ./mvnw -B release:prepare release:perform -Darguments="-Djvm=${env.JAVA_HOME_11}/bin/java -DaltDeploymentRepository=${env.ALT_DEPLOYMENT_REPOSITORY_TAG}"
                          """
                  }
            }
        }
}
