def buildMaven(profile){
    stage('Build & Unit Test') {
        sh 'mvn -s general-settings.xml clean package -U ' + profile
    }
}
def buildFrontend(){
    stage('Build Frontend') {
        sh 'yarn --cwd install'
        sh 'yarn --cwd run dist'
    }
}

def buildImage(dockerImage, imagename){
    stage('Build Docker Image') {
        script {
            dockerImage = docker.build(imagename + ":$BUILD_NUMBER")
        }
    }
    return dockerImage
}