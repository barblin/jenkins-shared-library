def testWithProfile(profilename) {
    stage('Integration Tests') {
        sh "mvn -s general-settings.xml integration-test -P${profilename}"
    }
}

def integrationTest(){
    testWithProfile('integrationtest');
}