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
                sh 'rm -rf ./infrastructure'
                sh 'rm -rf docker-compose.yml'
                sh 'cp -r /var/www/infrastructure/ .'
                sh 'cp -r /var/www/infrastructure/docker/docker-compose.yml docker-compose.yml'
                sh 'cp -r .env.example .env'
                sh 'ansible-playbook -i ./infrastructure/ansible/inventory/hosts.yml ./infrastructure/ansible/playbooks/install-docker.yml'
            }
        }

        stage('Build') {
            steps {
                // Blue environment
                sh "docker-compose -f ${BLUE_COMPOSE_FILE} build"

                // Green environment
                sh "docker-compose -f ${GREEN_COMPOSE_FILE} build"
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
                    if (env.PREVIOUS_BUILD == 'blue') {
                        COMPOSE_FILE = 'docker-compose.yml'
                        env.PREVIOUS_BUILD = ''
                    } else {
                        COMPOSE_FILE = 'docker-compose-blue.yml'
                        env.PREVIOUS_BUILD = 'blue'
                    }
                }

                // Stop the inactive environment
                sh "docker-compose -f ${COMPOSE_FILE} down --remove-orphans"

                // Remove orphan containers (if any)
                sh "docker-compose -f ${COMPOSE_FILE} rm -f -v"

                // Remove unused networks (if any)
                sh 'docker network prune -f'

                // Start the desired environment
                sh "docker-compose -f ${COMPOSE_FILE} up -d"

                // Execute post-deployment tasks
                sh 'docker exec php-fpm composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
                sh 'docker exec php-fpm chmod -R 0777 /var/www/html/storage'
                sh 'docker exec php-fpm php artisan key:generate'
                sh 'docker exec php-fpm php artisan config:cache'
                sh 'docker exec php-fpm php artisan view:clear'
                sh 'docker exec php-fpm php artisan config:clear'

                // Run health checks and tests
                // Modify the commands below based on your specific testing needs
                //sh 'docker exec php-fpm vendor/bin/phpunit'
                //sh 'docker exec webserver curl http://localhost:81/health-check'
            }
        }

        stage('Clean') {
            steps {
                sh 'docker system prune -af --filter "until=24h"'
            }
        }
    }
}
