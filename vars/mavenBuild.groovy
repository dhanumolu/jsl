#!/usr/bin/env groovy

def call(String mvnGoal = 'validate') {
	// Do something here...
	// Maven build
	sh '''
			mvn clean ${mvnGoal}
	   '''
}