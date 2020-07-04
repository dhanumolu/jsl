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
                    junit \
                        keepLongStdio: true, \
                        testResults: '**/target/surefire-reports/*.xml'
                    jacoco()
                }
            }
            stage('Sonarqube Analysis') {
                steps {
                    // Sonarqube Analysis
                    withSonarQubeEnv('sonarqube') {
                        sh '''
                            mvn sonar:sonar \
                                -Dintegration-tests.skip=true
                        '''
                    }
                    // Quality Gate Check
                    withSonarQubeEnv('sonarqube') {
                        timeout(time: 5, unit: 'MINUTES') {
                            retry(3) {
                                script {
                                    def qg = waitForQualityGate()
                                    if (qg.status != 'OK') {
                                        error "Pipeline aborted due to quality gate failure: ${qg.status}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            stage('OWASP Dependency Check') {
                agent {
                    kubernetes { label 'owasp' }
                }
                steps {
                    // OWASP Dependency Check
                    sh '''
                        $RUN_DEPENDENCY_CHECK \
                            -s . \
                            -f XML \
                            -o **/target/owasp-reports
                    '''
                }
            }
            stage('Docker Image Build') {
                agent {
                    kubernetes { label 'docker' }
                }
                steps {
                    // Build Docker Image
                    sh '''
                        appName=$(basename `git config --get remote.origin.url` .git)
                        appJar=$(find . -type f -path '**/target/**' -name '*SNAPSHOT.jar')
                        jarName=$(basename $appJar .jar)
                        dockerTag=${jarName#"$appName-"}
                        gitCommitID=$(git log -n 1 --format="%h")
                        docker build . \
                            -t $appName:$dockerTag \
                            --build-arg APP_JAR=$appJar \
                            --build-arg GIT_COMMIT_ID=$gitCommitID \
                            --build-arg JENKINS_BUILD_TAG=${BUILD_TAG}
                        docker system prune -f
                    '''
                }
            }
        }
        // post {
        //     always {
        //         // Delete Workspace
        //         deleteDir()
        //     }
        // }
    }
}