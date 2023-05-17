pipeline {
    agent any
    
    stages {
        stage('Prepare') {
            steps {
                script {
                    // Backup the current docker-compose.yml
                    sh 'mv docker-compose.yml docker-compose-blue.yml.bak'
                    // Copy the blue version of docker-compose.yml
                    sh 'cp -r /var/www/infrastructure/docker/docker-compose-blue.yml docker-compose.yml'
                    
                    // Stop the containers gracefully
                    try {
                        sh 'docker-compose down'
                    } catch (err) {
                        echo "No existing containers to stop."
                    }
                    
                    // Remove any existing volumes if needed
                    sh 'docker-compose rm -v -f'
                    
                    // Start the new containers
                    sh 'docker-compose up -d'
                }
            }
        }

        stage('Build') {
            steps {
                sh 'docker build -t banisedki/php-fpm:latest -f ./infrastructure/docker/php-fpm/Dockerfile .'
                sh 'docker build -t banisedki/nxtya_nginx:latest -f ./infrastructure/docker/nginx/Dockerfile .'
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([string(credentialsId: 'dockerHubPwd2', variable: 'dockerHubPwd2')]) {
                    sh "docker login -u banisedki -p ${dockerHubPwd2}"
                }
            }
        }

        stage('Deploy with docker-compose.yml') {
            environment {
                COMPOSE_FILE = 'docker-compose.yml'
            }
            steps {
                // Verify if the docker-compose.yml file exists
                script {
                    if (!fileExists(COMPOSE_FILE)) {
                        error "File '${COMPOSE_FILE}' does not exist."
                    }
                }

                // Deploy using docker-compose.yml
                sh 'COMPOSE_HTTP_TIMEOUT=480 docker-compose up -d'
            }
        }

        stage('Deploy with docker-compose-blue.yml') {
            environment {
                COMPOSE_FILE = 'docker-compose-blue.yml'
            }
            steps {
                // Verify if the docker-compose-blue.yml file exists
                script {
                    if (!fileExists(COMPOSE_FILE)) {
                        error "File '${COMPOSE_FILE}' does not exist."
                    }
                }

                // Deploy using docker-compose-blue.yml
                sh 'COMPOSE_HTTP_TIMEOUT=480 docker-compose up -d'
                sh 'docker exec php-fpm-blue rm -rf composer.lock vendor'
                sh 'docker exec php-fpm-blue composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
                sh 'docker exec php-fpm-blue chmod -R 0777 /var/www/html/storage'
                sh 'docker exec php-fpm-blue php artisan key:generate'
                sh 'docker exec php-fpm-blue php artisan config:cache'
                sh 'docker exec php-fpm-blue php artisan view:clear'
                sh 'docker exec php-fpm-blue php artisan config:clear'
            }
        }

        stage('Clean') {
            steps {
                sh 'docker system prune -af --filter "until=24h"'
            }
        }
    }
}
