pipeline {
    agent any
    stages {
       stage('Prepare') {
    steps {
        script {
            // Check if the deploy file is docker-compose.yml or docker-compose-blue.yml
            def deployFile = fileExists('docker-compose-blue.yml') ? 'docker-compose-blue.yml' : 'docker-compose.yml'

            // Backup the current deploy file
            sh "mv ${deployFile} ${deployFile}.bak"

            // Copy the new deploy file
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
   stage('Deploy with docker-compose-blue.yml') {
    steps {
        script {
            // Check if docker-compose-blue.yml exists
            def composeFile = fileExists('docker-compose-blue.yml') ? 'docker-compose-blue.yml' : 'docker-compose.yml'

            // Output the selected compose file for logging purposes
            echo "Using compose file: ${composeFile}"

            // Verify if the selected compose file exists
            if (!fileExists(composeFile)) {
                error "File '${composeFile}' does not exist."
            }

            // Deploy using the selected compose file
            sh "COMPOSE_HTTP_TIMEOUT=480 docker-compose -f ${composeFile} up -d"

            // Additional steps for the deployment
            if (composeFile == 'docker-compose.yml') {
                sh 'docker exec php-fpm rm -rf composer.lock vendor'
                sh 'docker exec php-fpm composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
                sh 'docker exec php-fpm chmod -R 0777 /var/www/html/storage'
                sh 'docker exec php-fpm php artisan key:generate'
                sh 'docker exec php-fpm php artisan config:cache'
                sh 'docker exec php-fpm php artisan view:clear'
                sh 'docker exec php-fpm php artisan config:clear'

            } else {if (composeFile == 'docker-compose-blue.yml') {
                sh 'docker exec php-fpm-blue rm -rf composer.lock vendor'
                sh 'docker exec php-fpm-blue composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
                sh 'docker exec php-fpm-blue chmod -R 0777 /var/www/html/storage'
                sh 'docker exec php-fpm-blue php artisan key:generate'
                sh 'docker exec php-fpm-blue php artisan config:cache'
                sh 'docker exec php-fpm-blue php artisan view:clear'
                sh 'docker exec php-fpm-blue php artisan config:clear'
            }}

            sh 'docker-compose down --remove-orphans'
        }
    }
}



        stage('Clean') {
            steps {
                // Stop and remove old Docker containers
                sh 'docker system prune -af --filter "until=24h"'
            }
        }
    }
}
