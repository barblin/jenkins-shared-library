def checkout(){
    stage('Checkout from Repo') {
        checkout scm
    }
}