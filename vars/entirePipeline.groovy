#!/usr/bin/env groovy

def call() {
    pipeline {
        agent {
            kubernetes { label 'maven-sonar-scanner' }
        }
        stages {
            stage('Maven Build') {
                steps {
                    script {
                        // Maven build
                        mvn clean validate
                    }
                }
            }
        }
    }
}