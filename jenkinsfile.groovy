pipeline {
    agent any
     //prepare ansible-playbook 
    stages {
    /*stage ('prepare')
        { steps    {

          sh 'rm -rf ./infrastructure'
          sh ' rm -rf docker-compose.yml'
          sh 'cp -r /var/www/infrastructure/ .'
          sh 'cp -r  /var/www/infrastructure/docker/docker-compose.yml . '
          sh 'cp -r .env.example .env '  
         sh 'ansible-playbook -i ./infrastructure/ansible/inventory/hosts.yml ./infrastructure/ansible/playbooks/install-docker.yml '
        }
         }*/
        
        
        /*stage('Prepare') {
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
}*/
 stage('Prepare') {
            steps {
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
  
       /* stage('Checkout') {
            steps {  
                git branch: 'main', credentialsId: 'SedkiBani', url: 'git@github.com:https-github-com-Sedkibani/app_laravel.git'
            }
        }*/
        
        stage('Build') {
            steps {
                sh 'docker build -t banisedki/php-fpm:latest -f ./infrastructure/docker/php-fpm/Dockerfile . '
                
                sh 'docker build -t banisedki/nxtya_nginx:latest -f ./infrastructure/docker/nginx/Dockerfile . '

			 // 'docker build -t nxtya:1.0 -f docker/Dockerfile .'
            }
        }
 
        stage('Docker Login') {
            steps {
                  withCredentials([string(credentialsId: 'dockerHubPwd2', variable: 'dockerHubPwd2')]) {
               // some block
               sh "docker login -u banisedki -p ${dockerHubPwd2}"
                  }
            }           
            }      
       /*stage('Push to Docker Hub') {
            steps {
                            sh 'docker push banisedki/php-fpm:latest' 
                            sh 'docker push banisedki/nxtya_nginx:latest'
                  }
                                   }*/
     
      /*  stage('Deploy') {
            steps {
               
               sh 'COMPOSE_HTTP_TIMEOUT=480 docker-compose up -d'
               sh 'docker exec  php-fpm rm -rf composer.lock vendor'
               sh  'docker exec  php-fpm composer install --ignore-platform-reqs --optimize-autoloader --prefer-dist --no-scripts -o --no-dev'
               sh ' docker exec  php-fpm chmod -R 0777 /var/www/html/storage'
                sh 'docker exec  php-fpm php artisan key:generate'
                sh 'docker exec  php-fpm php artisan config:cache'
                sh 'docker exec  php-fpm php artisan view:clear'
                sh 'docker exec  php-fpm php artisan config:clear'
        
          
                
            }
        
    }*/
        
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
        
        // Additional steps for docker-compose.yml
        // Add your specific steps here
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
        
        // Additional steps for docker-compose-blue.yml
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
               // Stop and remove old  docker container
               // sh 'docker stop $(docker ps -a -q)'
                //sh 'docker rm $(docker ps -a -q)'
          sh   'docker system prune -af --filter "until=24h" '
            }
        }
}
}
