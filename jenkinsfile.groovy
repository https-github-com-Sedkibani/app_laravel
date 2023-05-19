pipeline {
    agent any
    
    environment {
        BLUE_INFRA_DIR = 'infrastructure'
        GREEN_INFRA_DIR = 'infrastructure1'
        COMPOSE_FILE = ''
        PREVIOUS_BUILD = ''
    }

    stages {
        stage('Prepare') {
            steps {
                sh 'rm -rf ./${BLUE_INFRA_DIR}'
                sh 'rm -rf ./${GREEN_INFRA_DIR}'
                sh 'rm -rf ./docker-compose.yml'
                
                script {
                    def previousBuild = currentBuild.previousBuild
                    if (previousBuild != null) {
                        def previousBuildResult = previousBuild.result
                        if (previousBuildResult == 'SUCCESS') {
                            PREVIOUS_BUILD = 'green'
                        } else if (previousBuildResult == 'ABORTED' || previousBuildResult == 'FAILURE') {
                            PREVIOUS_BUILD = 'blue'
                        }
                    }
                    
                    if (PREVIOUS_BUILD == 'green') {
                        COMPOSE_FILE = 'docker-compose.yml'
                        sh "cp -r /var/www/${BLUE_INFRA_DIR}/ ./${BLUE_INFRA_DIR}"
                    } else {
                        COMPOSE_FILE = 'docker-compose-blue.yml'
                        sh "cp -r /var/www/${GREEN_INFRA_DIR}/ ./${GREEN_INFRA_DIR}"
                    }
                    
                    sh "cp -r ./${BLUE_INFRA_DIR}/docker-compose.yml ."
                    sh "cp -r ./${GREEN_INFRA_DIR}/docker-compose-blue.yml ."
                    sh "cp -r .env.example .env"
                    
                    sh "ansible-playbook -i ./${BLUE_INFRA_DIR}/ansible/inventory/hosts.yml ./${BLUE_INFRA_DIR}/ansible/playbooks/install-docker.yml"
                }
            }
        }

        stage('Build') {
            steps {
                sh "docker build -t banisedki/php-fpm:latest -f ./${GREEN_INFRA_DIR}/php-fpm/Dockerfile ."
                sh "docker build -t banisedki/nxtya_nginx:latest -f ./${GREEN_INFRA_DIR}/nginx/Dockerfile ."
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([string(credentialsId: 'dockerHubPwd2', variable: 'dockerHubPwd2')]) {
                    sh "docker login -u banisedki -p ${dockerHubPwd2}"
                }
            }
        }
/*
        stage('Push to Docker Hub') {
            steps {
                sh 'docker push banisedki/php-fpm:latest' 
                sh 'docker push banisedki/nxtya_nginx:latest'
            }
        }
*/
        stage('Deploy') {
            steps {
                sh "COMPOSE_HTTP_TIMEOUT=480 docker-compose -f ${COMPOSE_FILE} up -d"
                sh "docker exec php-fpm rm -rf composer.lock vendor"
                sh "docker exec php-fpm composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev"
                sh "docker exec php-fpm chmod -R 0777 /var/www/html/storage"
                sh "docker exec php-fpm php artisan key:generate"
                sh "docker exec php-fpm php artisan config:cache"
                sh "docker exec php-fpm php artisan view:clear"
                sh "docker exec php-fpm php artisan config:clear"
            }
        }

        stage('Clean') {
            steps {
                sh 'docker system prune -af --filter "until=24h"'
            }
        }
    }
}
