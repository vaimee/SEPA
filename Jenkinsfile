pipeline {
  agent {
    docker {
      image 'maven:3.5.0-jdk-8'
    }
    
  }
  stages {
    stage('Build') {
      steps {
        echo 'Hello World'
        sh 'mvn package'
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