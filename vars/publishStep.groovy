class ApiFile {
    String fileName
    String folder

    ApiFile(fileName, folder){
        this.fileName = fileName
        this.folder =  folder
    }
}

def uploadToRegistry(profile) {
    stage('Upload to Registry') {
        sh 'mvn -s general-settings.xml clean deploy -Dmaven.test.skip=true ' + profile
    }
}

def apiToRegistry(apiFiles) {
    if(apiFiles != null) {
        pom = readMavenPom file: 'pom.xml'
        stage('Upload API Definition to Registry') {
            for (file in apiFiles) {
                // needs to be implemented
            }
        }
    }
}

def uploadToRegistry(imagename, filename, dockerImage, credentials){
    stage('Upload Docker Image to registry') {
        sh "docker save ${imagename + ':$BUILD_NUMBER'} > " + filename + ".tar"

        script {
            // needs to be implemented
        }
    }
}