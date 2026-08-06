pipeline {
    agent any

    environment {
        COMPOSE_FILE = 'docker-compose.prod.yml'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Deploy') {
            steps {
                dir('backend') {
                    sh "docker compose -f ${COMPOSE_FILE} down"
                    sh "docker compose -f ${COMPOSE_FILE} up -d --build"
                }
            }
        }
    }

    post {
        success {
            echo 'Deploy completed.'
        }
        failure {
            echo 'Deploy failed.'
        }
    }
}
