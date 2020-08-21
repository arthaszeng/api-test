def COLOR_MAP = [
        'SUCCESS': 'good',
        'FAILURE': 'danger',
]

pipeline {
    agent none

    stages {

        stage('CI') {
            agent any

            stages {
                stage("build") {
                    steps {
                        sh './gradlew --no-daemon clean build'
                    }
                }

                stage("Sonar Analysis") {
                    environment {
                        scannerHome = tool 'SonarQubeScanner'
                    }
                    steps {
                        script {
                            withSonarQubeEnv('sonarqube') {
                                sh "${scannerHome}/bin/sonar-scanner -Dproject.settings=sonar-project.properties"
                            }

                            sleep(10)

                            qualitygate = waitForQualityGate()
                            if (qualitygate.status != "OK") {
                                currentBuild.result = "FAILURE"
                                slackSend(channel: '#loyalty-program-cicd', color: '#F01717', message: "*$JOB_NAME*, <$BUILD_URL|Build #$BUILD_NUMBER>: Code coverage threshold was not met! <http://****.com:9000/sonarqube/projects|Review in SonarQube>.")
                            }
                        }
                    }
                }
            }
        }
    }
}
