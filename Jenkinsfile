pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        echo 'Hello World'
        withMaven(maven: 'maven_jekins', jdk: 'JDK9') {
          sh 'mvn clean package'
        }
        
      }
    }
    stage('Test') {
      steps {
        withMaven(jdk: 'JDK9', maven: 'maven_jekins') {
          sh 'mvn verify  -Dmaven.javadoc.skip=true'
        }
        
      }
    }
  }
}