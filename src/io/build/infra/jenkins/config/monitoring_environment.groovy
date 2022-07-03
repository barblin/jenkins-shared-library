package io.build.infra.jenkins.config

class monitoring_environment {

    def getDevUser() {
        return 'root'
    }

    def getDevHost() {
        return 'linux-dev'
    }

    def getTestUser() {
        return 'elastic'
    }

    def getTestHost() {
        return 'linux-test'
    }

    def getStagingUser(){
        return 'root'
    }

    def getStagingHost(){
        return 'staging'
    }

    def getProdUser() {
        return 'elastic'
    }

    def getProdHost() {
        return 'linux-prod'
    }

    def isTest(branch) {
        return branch == 'develop' || branch.startsWith('uat')
    }

    def getReleaseEnvironments() {
        return ["dev", "test", "prod", "staging", "setup"]
    }

    def getVersion() {
        return "7.10.0"
    }
}