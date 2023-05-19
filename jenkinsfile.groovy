pipeline {
    agent any

    environment {
        BLUE_COMPOSE_FILE = 'docker-compose-blue.yml'
        GREEN_COMPOSE_FILE = 'docker-compose.yml'
        COMPOSE_FILE = ''
        PREVIOUS_BUILD = ''
    }

    stages {
        
        
     stage('Prepare') {
    steps {
        script {
            def blueExists = fileExists('docker-compose-blue.yml')
            def greenExists = fileExists('docker-compose.yml')

            if (blueExists && greenExists) {
                // Both blue and green environments exist
                def blueComposerExists = sh(
                    script: 'docker-compose -f docker-compose-blue.yml exec php-fpm-blue which composer',
                    returnStatus: true
                )

                if (blueComposerExists == 0) {
                    // Blue environment has composer, set COMPOSE_FILE to blue
                    COMPOSE_FILE = 'docker-compose-blue.yml'
                    COPY_TO_DIR = 'infrastructure'
                    REMOVE_FROM_DIR = 'infrastructure1'
                } else {
                    // Blue environment does not have composer, set COMPOSE_FILE to green
                    COMPOSE_FILE = 'docker-compose.yml'
                    COPY_TO_DIR = 'infrastructure1'
                    REMOVE_FROM_DIR = 'infrastructure'
                }
            } else if (blueExists) {
                // Only blue environment exists
                COMPOSE_FILE = 'docker-compose-blue.yml'
                COPY_TO_DIR = 'infrastructure'
                REMOVE_FROM_DIR = 'infrastructure1'
            } else if (greenExists) {
                // Only green environment exists
                COMPOSE_FILE = 'docker-compose.yml'
                COPY_TO_DIR = 'infrastructure1'
                REMOVE_FROM_DIR = 'infrastructure'
            } else {
                // No blue or green environment exists, install with green (default)
                COMPOSE_FILE = 'docker-compose.yml'
                COPY_TO_DIR = 'infrastructure1'
                REMOVE_FROM_DIR = 'infrastructure'
            }

            // Check the previous build and set PREVIOUS_BUILD accordingly
            def previousBuild = currentBuild.previousBuild
            if (previousBuild != null) {
                def previousBuildResult = previousBuild.result
                if (previousBuildResult == 'SUCCESS') {
                    PREVIOUS_BUILD = 'green'
                } else if (previousBuildResult == 'ABORTED' || previousBuildResult == 'FAILURE') {
                    PREVIOUS_BUILD = 'blue'
                }
            }

            sh "rm -rf ./${REMOVE_FROM_DIR}"
            sh "rm -rf docker-compose.yml"
            sh "cp -r /var/www/${COPY_TO_DIR}/ ."
            sh "cp -r /var/www/${COPY_TO_DIR}/docker/${COMPOSE_FILE} docker-compose.yml"
            sh "cp -r .env.example .env"
            sh "ansible-playbook -i ./${COPY_TO_DIR}/ansible/inventory/hosts.yml ./${COPY_TO_DIR}/ansible/playbooks/install-docker.yml"
        }
    }
}


      
            stage('Build') {
            steps {
                script {
                    def blueExists = fileExists('docker-compose-blue.yml')
                    def greenExists = fileExists('docker-compose.yml')
                    
                    if (blueExists && greenExists) {
                        // Both blue and green environments exist
                        def blueComposerExists = sh(
                            script: 'docker-compose -f docker-compose-blue.yml exec php-fpm-blue which composer',
                            returnStatus: true
                        )
                        
                        if (blueComposerExists == 0) {
                            // Blue environment has composer, set COMPOSE_FILE to blue
                            COMPOSE_FILE = 'docker-compose-blue.yml'
                        } else {
                            // Blue environment does not have composer, set COMPOSE_FILE to green
                            COMPOSE_FILE = 'docker-compose.yml'
                        }
                    } else if (blueExists) {
                        // Only blue environment exists
                        COMPOSE_FILE = 'docker-compose-blue.yml'
                    } else if (greenExists) {
                        // Only green environment exists
                        COMPOSE_FILE = 'docker-compose.yml'
                    } else {
                        // No blue or green environment exists, install with green (default)
                        COMPOSE_FILE = 'docker-compose.yml'
                    }
                    
                    // Build the selected environment
                    sh "docker-compose -f ${COMPOSE_FILE} build"
                }
            }
        }


        stage('Docker Login') {
            steps {
                withCredentials([string(credentialsId: 'dockerHubPwd2', variable: 'dockerHubPwd2')]) {
                    sh "docker login -u banisedki -p ${dockerHubPwd2}"
                }
            }
        }

   
                stage('Deploy') {
            steps {
                script {
                    if (COMPOSE_FILE == 'docker-compose-blue.yml') {
                        env.PREVIOUS_BUILD = 'blue'
                        //sh 'docker-compose -f docker-compose-blue.yml up -d'
                            sh 'COMPOSE_HTTP_TIMEOUT=480 docker-compose-blue up -d'
               sh 'docker exec  php-fpm-blue rm -rf composer.lock vendor'
               sh  'docker exec  php-fpm-blue composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
               sh ' docker exec  php-fpm-blue chmod -R 0777 /var/www/html/storage'
                sh 'docker exec  php-fpm-blue php artisan key:generate'
                sh 'docker exec  php-fpm-blue php artisan config:cache'
                sh 'docker exec  php-fpm-blue php artisan view:clear'
                sh 'docker exec  php-fpm-blue php artisan config:clear'
             //   sh 'docker-compose -f docker-compose.yml down --remove-orphans'
                    } else {
                        COMPOSE_FILE = 'docker-compose.yml'
                        env.PREVIOUS_BUILD = ''
                        sh 'docker-compose -f docker-compose.yml up -d'
                         sh 'COMPOSE_HTTP_TIMEOUT=480 docker-compose up -d'
               sh 'docker exec  php-fpm rm -rf composer.lock vendor'
               sh  'docker exec  php-fpm composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
               sh ' docker exec  php-fpm chmod -R 0777 /var/www/html/storage'
                sh 'docker exec  php-fpm php artisan key:generate'
                sh 'docker exec  php-fpm php artisan config:cache'
                sh 'docker exec  php-fpm php artisan view:clear'
                sh 'docker exec  php-fpm php artisan config:clear'
                                        //sh 'docker-compose -f docker-compose-blue.yml down --remove-orphans'

                  //            sh 'rm -rf ./infrastructure'
              // sh 'rm -rf docker-compose.yml'
            }
        }
            }
                }

        stage('Clean') {
            steps {
                sh 'docker system prune -af --filter "until=24h"'
            }
        }
    }
}
