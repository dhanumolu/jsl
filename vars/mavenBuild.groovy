#!/usr/bin/env groovy

def call(String mvnGoal = 'validate') {
	// Do something here...
	// Maven build
	echo "Building maven goal ${mvnGoal}"
	mvn clean "${mvnGoal}"
}