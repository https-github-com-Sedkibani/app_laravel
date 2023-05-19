pipeline {
    agent any

    environment {
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

        stage('Deploy') {
            steps {
                script {
                    if (env.PREVIOUS_BUILD == 'blue') {
                        COMPOSE_FILE = 'docker-compose-blue.yml'
                        env.PREVIOUS_BUILD = ''
                    } else {
                        COMPOSE_FILE = 'docker-compose.yml'
                        env.PREVIOUS_BUILD = 'blue'
                    }
                }
                sh "docker-compose -f ${COMPOSE_FILE} up -d --remove-orphans"
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
                sh 'docker system prune -af --filter "until=24h"'
            }
        }
    }
}
