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
        withMaven(jdk: 'jdk9', maven: 'mvn', mavenLocalRepo: 'C:\\Users\\reluc\\.m2\\repository', mavenSettingsFilePath: 'C:\\Users\\reluc\\.m2\\settings.xml') {
          bat 'mvn verify  -Dmaven.javadoc.skip=true'
        }
        
      }
    }
  }
}