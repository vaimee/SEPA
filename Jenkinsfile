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
      parallel {
        stage('Blazegraph') {
          steps {
            echo 'Copy blazegraph configuration file ...'
            script {
              def ws = pwd()
              def blazeConfig = ws + '/engine/target/endpoints/endpoint-blazegraph.jpar'
              def target = ws + '/engine/target'
              sh 'mv ' + blazeConfig + ' ' + target + '/endpoint.jpar'
            }
            
            dir(path: 'engine/target') {
              sh 'java -server -jar engine-0-SNAPSHOT.jar > engine.log &'
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
            
          }
        }
        stage('Fuseki') {
          steps {
            dir(path: 'fuseki') {
              echo 'Copy Fuseki test configuration files'
              script {
                def blazeConfig = '../engine/target/endpoints/endpoint-fuseki.jpar'
                sh 'cp ' + blazeConfig + ' endpoint.jpar'
                echo 'Inject endpoint configuration'
                writeFile file: 'engine.jpar', text: '''{
"parameters" : {
"scheduler" : {
"queueSize" : 100}
,
"processor" : {
"updateTimeout" : 5000 ,
"queryTimeout" : 5000,
"maxConcurrentRequests" : 5}
,
"spu" : {
"keepalive" : 5000,
"timeout" : 10000}
,
"ports" : {
"http" : 8001 ,
"ws" : 9001 ,
"https" : 8444 ,
"wss" : 9444}
,
"paths" : {
"update" : "/update" ,
"query" : "/query" ,
"subscribe" : "/subscribe" ,
"register" : "/oauth/register" ,
"tokenRequest" : "/oauth/token" ,
"securePath" : "/secure"}
}
}'''
                  writeFile file: 'client.jpar', text: '''
{
"parameters": {
"host": "localhost",
"ports": {
"http": 8001,
"https": 8443,
"ws": 9001,
"wss": 9443
},
"paths": {
"query": "/query",
"update": "/update",
"subscribe": "/subscribe",
"register": "/oauth/register",
"tokenRequest": "/oauth/token",
"securePath": "/secure"
},
"methods": {
"query": "POST",
"update": "URL_ENCODED_POST"
},
"formats": {
"query": "JSON",
"update": "HTML"
},
"security": {
"client_id": "jaJBrmgtqgW9jTLHeVbzSCH6ZIN1Qaf3XthmwLxjhw3WuXtt7VELmfibRNvOdKLs",
"client_secret": "fkITPTMsHUEb9gVVRMP5CAeIE1LrfBYtNLdqtlTVZ/CqgqcuzEw+ZcVegW5dMnIg",
"jwt": "xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfvx34vSSnhpkfcdYbZ+7KDaK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkfP7DEKe7LScGYaT4RcuIfNmywI4fAWabAI4zqedYbd5lXmYhbSmXviPTOQPKxhmZptZ6F5Q178nfK6Bik4/0PwUlgMsC6oVFeJtyPWvjfEP0nx9tGMOt+z9Rvbd7enGWRFspUQJS2zzmGlHW1m5QNFdtOCfTLUOKkyZV4JUQxI1CaP+QbIyIihuQDvIMbmNgbvDNBkj9VQOzg1WB7mj4nn4w7T8I9MpOxAXxnaPUvDk8QnL/5leQcUiFVTa1zlzambQ8xr/BojFB52fIz8LsrDRW/+/0CJJVTFYD6OZ/gepFyLK4yOu/rOiTLT5CF9H2NZQd7bi85zSmi50RHFa3358LvL50c4G84Gz7mkDTBV9JxBhlWVNvD5VR58rPcgESwlGEL2YmOQCZzYGWjTc5cyI/50ZX83sTlTbfs+Tab3pBlsRQu36iNznleeKPj6uVvql+3uvcjMEBqqXvj8TKxMi9tCfHA1vt9RijOap8ROHtnIe4iMovPzkOCMiHJPcwbnyi+6jHbrPI18WGghceZQT23qKHDUYQo2NiehLQG9MQZA1Ncx2w4evBTBX8lkBS4aLoCUoTZTlNFSDOohUHJCbeig9eV77JbLo0a4+PNH9bgM/icSnIG5TidBGyJpEkVtD7+/KphwM89izJam3OT",
"expires": "04/5tRBT5n/VJ0XQASgs/w==",
"type": "XPrHEX2xHy+5IuXHPHigMw=="
}
}
}
'''
                  echo 'Copy sepa.jks'
                  def keys = '../engine/target/sepa.jks'
                  sh 'cp ' + keys + ' sepa.jks'
                  sh 'java -jar ../engine/target/engine-0-SNAPSHOT.jar > engine.log &'
                }
                
              }
              
              timeout(time: 10) {
                waitUntil() {
                  script {
                    def r = sh script: 'wget -q http://localhost:8001 -O /dev/null', returnStatus: true
                    return (r == 8)
                  }
                  
                }
                
              }
              
              sh 'FUSEKI_HOME=/home/cristianoaguzzi/apache-jena-fuseki-3.6.0/ /home/cristianoaguzzi/apache-jena-fuseki-3.6.0/fuseki-server --update --mem /ds &'
              timeout(time: 10) {
                waitUntil() {
                  script {
                    def r = sh script: 'wget -q http://localhost:3030 -O /dev/null', returnStatus: true
                    return (r == 0)
                  }
                  
                }
                
              }
              
              withMaven(jdk: 'JDK9', maven: 'maven_jekins', mavenLocalRepo: 'fuseki/maven') {
                sh 'mvn verify  -Dmaven.javadoc.skip=true -DtestConfiguration=../fuseki/client.jpar'
              }
              
            }
          }
        }
      }
    }
    post {
      always {
        archiveArtifacts 'engine/target/engine.log'
        archiveArtifacts 'fuseki/engine.log'
        cleanWs(notFailBuild: true)
        
      }
      
    }
  }