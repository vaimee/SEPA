pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        echo 'Hello World'
        withMaven(jdk: 'jdk9', maven: 'mvn', mavenSettingsFilePath: 'C:\\Users\\reluc\\.m2\\settings.xml', mavenLocalRepo: 'C:\\Users\\reluc\\.m2\\repository') {
          bat 'mvn compile -Dmaven.javadoc.skip=true'
        }
        
        echo 'Ok Next'
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