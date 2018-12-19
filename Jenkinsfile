timestamps {
    ansiColor('xterm') {
        node {
            stage('Setup') {
                checkout scm
            }

            stage('Build') {
                try {
                    sh "./mvnw clean verify -Djvm=${env.JAVA_HOME_11}/bin/java"
                    archiveArtifacts '**/target/*.zip'
                } finally {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
