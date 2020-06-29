#!/usr/bin/env groovy

def call() {
    pipeline {
        agent {
            kubernetes { label 'maven-sonar-scanner' }
        }
        stages {
            stage('Maven Build') {
                steps {
                    // Maven build
                    sh '''
                        mvn clean verify
                    '''
                    // Publish Reports
                    junit keepLongStdio: true, \
                        testResults: '**/target/surefire-reports/*.xml'
                    jacoco()
                }
            }
        }
    }
}