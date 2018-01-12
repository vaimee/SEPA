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
        echo 'Copy blazegraph configuration file ...'
        script {
          def ws = pwd()
          def blazeConfig = ws + '/engine/target/endpoints/endpoint-blazegraph.jpar'
          def target = ws + '/engine/target'
          sh 'mv ' + blazeConfig + ' ' + target + '/endpoint.jpar'
          sh 'java -server -jar '+ws+'/engine/target/engine-0-SNAPSHOT.jar > '+ws+'/engine/target/engine.log &'
        }
        
        timeout(time: 10) {
          waitUntil() {
            script {
              def r = sh script: 'wget -q http://localhost:8000 -O /dev/null', returnStatus: true
              return (r == 8)
            }
            
          }
          
        }
        
        sh 'java -server -Xmx4g -jar /home/cristianoaguzzi/blazegraph.jar &'
        timeout(time: 10) {
          waitUntil() {
            script {
              def r = sh script: 'wget -q http://localhost:9999 -O /dev/null', returnStatus: true
              return (r == 0)
            }
            
          }
          
        }
        
        withMaven(jdk: 'JDK9', maven: 'maven_jekins') {
          sh 'mvn verify  -Dmaven.javadoc.skip=true'
        }
        
        archiveArtifacts 'engine/target/engine.log'
      }
    }
  }
}