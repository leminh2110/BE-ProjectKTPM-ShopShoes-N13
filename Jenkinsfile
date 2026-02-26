pipeline {
    agent any
    
    environment {
        JAVA_VERSION = '21'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Eureka Server') {
            steps {
                dir('eureka-server') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build API Gateway') {
            steps {
                dir('api-gateway') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Authentication Service') {
            steps {
                dir('authentication-service') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Product Service') {
            steps {
                dir('product-service') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Cart Service') {
            steps {
                dir('cart-service') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Payment Service') {
            steps {
                dir('payment-service') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Invoice Service') {
            steps {
                dir('invoice-service') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Email Service') {
            steps {
                dir('emailsender-service') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                bat 'echo "Deploying to production..."'
                // Thêm lệnh deploy của bạn ở đây
                // Ví dụ: xcopy target\\*.jar \\\\server\\share\\path\\to\\deploy /Y
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
} 