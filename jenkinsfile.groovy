pipeline {
    agent any
    stages {
    stage ('prepare')
        { steps    {
               
          sh 'rm -rf ./infrastructure'
          sh ' rm -rf docker-compose.yml'
          sh 'cp -r /var/www/infrastructure/ .'
          sh 'cp -r  /var/www/infrastructure/docker/docker-compose.yml . '
          sh 'cp -r .env.example .env '  
            
             //prepare ansible-playbook 
          sh 'ansible-playbook -i ./infrastructure/ansible/inventory/hosts.yml ./infrastructure/ansible/playbooks/install-docker.yml ' }
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

        
        /*stage('Code Quality') {
            steps {
                sh 'docker run --rm nxtya:1.0 vendor/bin/phpstan analyze'
                // Additional commands for other code quality tools like SonarQube
            }
        }*/

        stage('Deploy') {
            steps {
               
               sh 'COMPOSE_HTTP_TIMEOUT=480 docker-compose up -d'
              
                // Use Ansible playbook to deploy to DigitalOcean server
                //ansiblePlaybook(
                   // playbook: '/var/lib/jenkins/workspace/nxtya/ansible.yml',
                    //inventory: '/var/lib/jenkins/workspace/nxtya/inventory.ini',
                    //extras: "-e 'docker_image=nxtya:1.0'"
                
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