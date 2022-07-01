def sonarQubeAnalysis(qualityGateEnabled = true) {
    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh 'mvn -s general-settings.xml sonar:sonar'
        }
    }

    if (qualityGateEnabled) {
        stage('Quality Gate') {
            timeout(time: 5, unit: 'MINUTES') {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
    }
}