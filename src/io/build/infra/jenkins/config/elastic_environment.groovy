package io.build.infra.jenkins.config

class elastic_environment {

    def getDevUser() {
        return 'elastic'
    }

    def getDevHost() {
        throw new NotImplementedException("not implemented yet")
    }

    def getTestUser() {
        return 'elastic'
    }

    def getTestHost() {
        return 'linux-test-elastic'
    }

    def getStagingUser(){
        return 'root'
    }

    def getStagingHost(){
        throw new NotImplementedException("not implemented yet")
    }

    def getProdUser() {
        return 'elastic'
    }

    def getProdHost() {
        return 'linux-prod-elastic'
    }

    def isTest(branch) {
        return branch == 'develop' || branch.startsWith('uat')
    }

    def getReleaseEnvironments() {
        return ["dev", "test", "prod", "staging", "setup"]
    }

    def getVersion(){
        return "7.14.0"
    }
}