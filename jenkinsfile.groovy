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
                // Your existing code for preparing the environment
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

        // The rest of your stages...
    }
}
