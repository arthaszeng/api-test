def COLOR_MAP = [
        'SUCCESS': 'good',
        'FAILURE': 'danger',
]

pipeline {
    agent none

    environment {
        DOCKER_REG = "${DOCKER_REG}"
        TW_LOYALTY_REPOSITORY = "${DOCKER_REG}/tw-loyalty-point-api"
        TW_LOYALTY_POINT_API_IMAGE = "${TW_LOYALTY_REPOSITORY}:build-${BUILD_NUMBER}"
    }

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

                stage("dockerize") {
                    steps {
                        sh 'aws ecr get-login-password | docker login  -u AWS --password-stdin $DOCKER_REG'
                        sh 'docker build -t $TW_LOYALTY_POINT_API_IMAGE .'
                        sh 'docker push $TW_LOYALTY_POINT_API_IMAGE'
                    }
                }
            }
        }

        stage("build, deploy to different region") {
            parallel {
                stage("Macau") {
                    stages {
                        stage('Deploy to dev') {
                            agent any
                            steps {
                                lock(resource: 'devEnvironmentMacau', inversePrecedence: true) {
                                    sshagent(credentials: ['jenkins_nonprod']) {
                                        sh "ssh -o StrictHostKeyChecking=no -l ${EC2_USER} ${LOYALTY_POINT_API_DEV_ADDR} \
                                             'aws ecr get-login-password | sudo docker login -u AWS --password-stdin $DOCKER_REG;\
                                             docker rmi -f `docker images | grep build-* | awk '{print \$3}'`; \
                                             sudo docker pull ${TW_LOYALTY_POINT_API_IMAGE};\
                                             sudo docker rm -f loyalty-point-api-dev;\
                                             sudo docker run --name loyalty-point-api-dev -p 8080:8080 \
                                             -e PROJECT_ENV=dev -e REGION=MACAU -e ROC_NAME=ROC_MACAU \
                                             -e DB_USERNAME=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/db_username /dev/stdout` \
                                             -e DB_PASSWORD=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/db_password /dev/stdout` \
                                             -e JWT_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/jwt_private_key /dev/stdout` \
                                             -e JWT_PUBLIC_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/jwt_public_key /dev/stdout` \
                                             -e ROC_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/roc_private_key_dev /dev/stdout` \
                                             -e ROC_ADDRESS=${LOYALTY_POINT_ROC_ADDRESS_MACAU_DEV} \
                                             -e ADMIN_ADDRESS=0xed9d02e382b34818e88B88a309c7fe71E65f419d \
                                             -e ADMIN_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/admin_private_key /dev/stdout` \
                                             -e NODE_NUMBER=1 -d --restart=always ${TW_LOYALTY_POINT_API_IMAGE}'"
                                    }
                                }
                            }
                        }

                        stage('Continue intg?') {
                            agent none
                            steps {
                                input message: 'Deploy to intg?'
                            }
                        }

                        stage('Deploy to intg') {
                            agent any
                            steps {
                                lock(resource: 'qaEnvironmentMacau', inversePrecedence: true) {
                                    sshagent(credentials: ['jenkins_nonprod']) {
                                        sh "ssh -o StrictHostKeyChecking=no -l ${EC2_USER} ${LOYALTY_POINT_API_DEV_ADDR} \
                                          'aws ecr get-login-password | sudo docker login -u AWS --password-stdin $DOCKER_REG;\
                                          docker rmi -f `docker images | grep build-* | awk '{print \$3}'`; \
                                          sudo docker pull ${TW_LOYALTY_POINT_API_IMAGE};\
                                          sudo docker rm -f loyalty-point-api-intg;\
                                          sudo docker run --name loyalty-point-api-intg -p 18080:8080 -e PROJECT_ENV=intg \
                                          -e REGION=MACAU -e ROC_NAME=ROC_MACAU -e ROC_ADDRESS=${LOYALTY_POINT_ROC_ADDRESS_MACAU_INTG} \
                                          -e DB_USERNAME=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/db_username /dev/stdout` \
                                          -e DB_PASSWORD=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/db_password /dev/stdout` \
                                          -e JWT_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/jwt_private_key /dev/stdout` \
                                          -e JWT_PUBLIC_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/jwt_public_key /dev/stdout` \
                                          -e ROC_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/roc_private_key_intg /dev/stdout` \
                                          -e ADMIN_ADDRESS=0xed9d02e382b34818e88B88a309c7fe71E65f419d \
                                          -e ADMIN_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/admin_private_key /dev/stdout` \
                                          -e NODE_NUMBER=1 -d --restart=always ${TW_LOYALTY_POINT_API_IMAGE}'"
                                    }

                                }
                            }
                        }

                        stage('Continue uat?') {
                            agent none
                            steps {
                                input message: 'Deploy to uat?'
                            }
                        }

                        stage('Deploy to uat') {
                            agent any
                            steps {
                                lock(resource: 'uatEnvironmentMacau', inversePrecedence: true) {
                                    sshagent(credentials: ['jenkins_nonprod']) {
                                        sh "ssh -o StrictHostKeyChecking=no -l ${EC2_USER} ${LOYALTY_POINT_API_DEV_ADDR} \
                                          'aws ecr get-login-password | sudo docker login -u AWS --password-stdin $DOCKER_REG;\
                                          sudo docker pull ${TW_LOYALTY_POINT_API_IMAGE};\
                                          sudo docker rm -f loyalty-point-api-uat;\
                                          sudo docker run --name loyalty-point-api-uat -p 28080:8080 -e PROJECT_ENV=uat \
                                          -e REGION=MACAU -e ROC_NAME=ROC_MACAU -e ROC_ADDRESS=${LOYALTY_POINT_ROC_ADDRESS_MACAU_UAT} \
                                          -e DB_USERNAME=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/db_username /dev/stdout` \
                                          -e DB_PASSWORD=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/db_password /dev/stdout` \
                                          -e JWT_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/jwt_private_key /dev/stdout` \
                                          -e JWT_PUBLIC_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/jwt_public_key /dev/stdout` \
                                          -e ROC_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/roc_private_key_uat /dev/stdout` \
                                          -e ADMIN_ADDRESS=0xed9d02e382b34818e88B88a309c7fe71E65f419d \
                                          -e ADMIN_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-macau-secrets/services/point/admin_private_key /dev/stdout` \
                                          -e NODE_NUMBER=1 -d --restart=always ${TW_LOYALTY_POINT_API_IMAGE}'"
                                    }
                                }
                            }
                        }
                    }
                }
                stage("Manila") {
                    stages {
                        stage('Deploy to dev') {
                            agent any
                            steps {
                                lock(resource: 'devEnvironmentManila', inversePrecedence: true) {
                                    sshagent(credentials: ['jenkins_nonprod']) {
                                        sh "ssh -o StrictHostKeyChecking=no -l ${EC2_USER} ${LOYALTY_POINT_API_DEV_ADDR_MANILA} \
                                          'aws ecr get-login-password | sudo docker login -u AWS --password-stdin $DOCKER_REG;\
                                          docker rmi -f `docker images | grep build-* | awk '{print \$3}'`; \
                                          sudo docker pull ${TW_LOYALTY_POINT_API_IMAGE};\
                                          sudo docker rm -f loyalty-point-api-dev;\
                                          sudo docker run --name loyalty-point-api-dev -p 8080:8080 -e PROJECT_ENV=dev \
                                          -e REGION=MANILA -e ROC_NAME=ROC_MANILA -e ROC_ADDRESS=${LOYALTY_POINT_ROC_ADDRESS_MANILA_DEV} \
                                          -e DB_USERNAME=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/db_username /dev/stdout` \
                                          -e DB_PASSWORD=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/db_password /dev/stdout` \
                                          -e JWT_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/jwt_private_key /dev/stdout` \
                                          -e JWT_PUBLIC_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/jwt_public_key /dev/stdout` \
                                          -e ROC_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/roc_private_key_dev /dev/stdout` \
                                          -e ADMIN_ADDRESS=0xed9d02e382b34818e88B88a309c7fe71E65f419d \
                                          -e ADMIN_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/admin_private_key /dev/stdout` \
                                          -e NODE_NUMBER=2 -d --restart=always ${TW_LOYALTY_POINT_API_IMAGE}'"
                                    }
                                }
                            }
                        }

                        stage('Continue intg?') {
                            agent none
                            steps {
                                input message: 'Deploy to intg?'
                            }
                        }

                        stage('Deploy to intg') {
                            agent any
                            steps {
                                lock(resource: 'qaEnvironmentManila', inversePrecedence: true) {
                                    sshagent(credentials: ['jenkins_nonprod']) {
                                        sh "ssh -o StrictHostKeyChecking=no -l ${EC2_USER} ${LOYALTY_POINT_API_DEV_ADDR_MANILA} \
                                          'aws ecr get-login-password | sudo docker login -u AWS --password-stdin $DOCKER_REG;\
                                          docker rmi -f `docker images | grep build-* | awk '{print \$3}'`; \
                                          sudo docker pull ${TW_LOYALTY_POINT_API_IMAGE};\
                                          sudo docker rm -f loyalty-point-api-intg;\
                                          sudo docker run --name loyalty-point-api-intg -p 18080:8080 -e PROJECT_ENV=intg -e REGION=MANILA\
                                          -e ROC_NAME=ROC_MANILA -e ROC_ADDRESS=${LOYALTY_POINT_ROC_ADDRESS_MANILA_INTG} \
                                          -e DB_USERNAME=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/db_username /dev/stdout` \
                                          -e DB_PASSWORD=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/db_password /dev/stdout` \
                                          -e JWT_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/jwt_private_key /dev/stdout` \
                                          -e JWT_PUBLIC_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/jwt_public_key /dev/stdout` \
                                          -e ROC_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/roc_private_key_intg /dev/stdout` \
                                          -e ADMIN_ADDRESS=0xed9d02e382b34818e88B88a309c7fe71E65f419d \
                                          -e ADMIN_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/admin_private_key /dev/stdout` \
                                          -e NODE_NUMBER=2 -d --restart=always ${TW_LOYALTY_POINT_API_IMAGE}'"
                                    }
                                }
                            }
                        }

                        stage('Continue uat?') {
                            agent none
                            steps {
                                input message: 'Deploy to uat?'
                            }
                        }

                        stage('Deploy to uat') {
                            agent any
                            steps {
                                lock(resource: 'uatEnvironmentMacau', inversePrecedence: true) {
                                    sshagent(credentials: ['jenkins_nonprod']) {
                                        sh "ssh -o StrictHostKeyChecking=no -l ${EC2_USER} ${LOYALTY_POINT_API_DEV_ADDR_MANILA} \
                                          'aws ecr get-login-password | sudo docker login -u AWS --password-stdin $DOCKER_REG;\
                                          sudo docker pull ${TW_LOYALTY_POINT_API_IMAGE};\
                                          sudo docker rm -f loyalty-point-api-uat;\
                                          sudo docker run --name loyalty-point-api-uat -p 28080:8080 -e PROJECT_ENV=uat \
                                          -e REGION=MANILA -e ROC_NAME=ROC_MANILA -e ROC_ADDRESS=${LOYALTY_POINT_ROC_ADDRESS_MANILA_UAT} \
                                          -e DB_USERNAME=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/db_username /dev/stdout` \
                                          -e DB_PASSWORD=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/db_password /dev/stdout` \
                                          -e JWT_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/jwt_private_key /dev/stdout` \
                                          -e JWT_PUBLIC_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/jwt_public_key /dev/stdout` \
                                          -e ROC_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/roc_private_key_uat /dev/stdout` \
                                          -e ADMIN_ADDRESS=0xed9d02e382b34818e88B88a309c7fe71E65f419d \
                                          -e ADMIN_PRIVATE_KEY=`aws s3 cp --quiet s3://twbc-loyalty-program-manila-secrets/services/point/admin_private_key /dev/stdout` \
                                          -e NODE_NUMBER=2 -d --restart=always ${TW_LOYALTY_POINT_API_IMAGE}'"
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    post {
        always {
            echo 'Time to send slack message.'
            slackSend channel: '#loyalty-program-cicd',
                    color: COLOR_MAP[currentBuild.currentResult],
                    message: "*${currentBuild.currentResult}:* Job ${env.JOB_NAME} build ${env.BUILD_NUMBER} on (<${env.BUILD_URL}|Open>)"
        }
    }
}
