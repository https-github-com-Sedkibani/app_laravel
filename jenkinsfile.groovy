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
                // Your existing prepare steps here
            }
        }

        stage('Build') {
            steps {
                // Your existing build steps here
            }
        }

        stage('Docker Login') {
            steps {
                // Your existing Docker login steps here
            }
        }

        stage('Deploy') {
            steps {
                script {
                    if (env.PREVIOUS_BUILD == 'blue') {
                        COMPOSE_FILE = 'docker-compose.yml'
                        env.PREVIOUS_BUILD = ''
                    } else {
                        COMPOSE_FILE = 'docker-compose-blue.yml'
                        env.PREVIOUS_BUILD = 'blue'
                    }
                }

                // Stop the inactive environment
                sh "docker-compose -f ${COMPOSE_FILE} down"

                // Remove orphans containers and networks
                sh "docker-compose -f ${COMPOSE_FILE} down --remove-orphans"
                sh "docker network rm nxtya_laravel_app-network"

                // Start the updated environment
                sh "docker-compose -f ${COMPOSE_FILE} up -d"

                // Execute post-deployment tasks
                sh 'docker exec php-fpm rm -rf composer.lock vendor'
                sh 'docker exec php-fpm composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
                sh 'docker exec php-fpm chmod -R 0777 /var/www/html/storage'
                sh 'docker exec php-fpm php artisan key:generate'
                sh 'docker exec php-fpm php artisan config:cache'
                sh 'docker exec php-fpm php artisan view:clear'
                sh 'docker exec php-fpm php artisan config:clear'
            }
        }

        stage('Clean') {
            steps {
                // Your existing clean steps here
            }
        }
    }
}
