package io.build.infra.jenkins.config

class artefact_environment {

    def repoCredentials() {
        throw new NotImplementedException("not implemented yet")
    }

    def getDevUser() {
        return 'root'
    }

    def getStagingUser() {
        return 'root'
    }

    def getTestUser() {
        return 'elastic'
    }

    def getProdUser() {
        return 'elastic'
    }

    def getTargetServerFromBranch(branch) {
        if (branch == 'master') {
            throw new NotImplementedException("not implemented yet")
        } else if (isTest(branch)) {
            throw new NotImplementedException("not implemented yet")
        } else {
            throw new NotImplementedException("not implemented yet")
        }
    }

    def getDeployFolderFromBranch(branch) {
        if (branch == 'master') {
            return "prod-es7"
        } else if (isTest(branch)) {
            return "test-es7"
        } else {
            return "dev-es7"
        }
    }

    def getTargetServerFromEnvironment(environment) {
        if (environment == 'dev') {
            throw new NotImplementedException("not implemented yet")
        }
        if (environment == 'staging') {
            throw new NotImplementedException("not implemented yet")
        }
        if (environment == 'test') {
            throw new NotImplementedException("not implemented yet")
        }
        if (environment == 'prod') {
            throw new NotImplementedException("not implemented yet")
        }
    }

    def getDeployFolderFromEnvironment(environment) {
        if (environment == 'prod') {
            return "prod-es7"
        }
        if (environment == 'test') {
            return "test-es7"
        }
        if (environment == 'dev') {
            return "dev-es7"
        }
        if (environment == 'staging') {
            return "staging-es7"
        }
    }

    def getDockerNameFromEnvironment(environment) {
        if (environment == 'prod') {
            return "prod-es7-node"
        }
        if (environment == 'test') {
            return "test-node"
        }
        if (environment == 'dev') {
            return "dev-es7-node"
        }
        if (environment == 'staging') {
            return "staging-node"
        }
    }

    def getUserFromEnvironment(environment) {
        if (environment == 'prod') {
            return getProdUser()
        }
        if (environment == 'test') {
            return getTestUser()
        }
        if (environment == 'dev') {
            return getDevUser()
        }
        if (environment == 'staging') {
            return getStagingUser()
        }
    }

    def getDockerNameFromBranch(branch) {
        if (branch == 'master') {
            return "prod-es7-node"
        } else if (isTest(branch)) {
            return "test-es7-node"
        } else {
            return "dev-es7-node"
        }
    }

    def getEnvFromBranch(branch) {
        if (branch == 'master') {
            return "prod"
        } else if (isTest(branch)) {
            return "test"
        } else {
            return "dev"
        }
    }

    def getUserFromBranch(branch) {
        if (branch == 'master') {
            return getProdUser()
        } else if (isTest(branch)) {
            return getTestUser()
        } else {
            return getDevUser()
        }
    }

    def getImageNameFromBranch(name, branch) {
        if (branch == 'master') {
            return name
        } else if (branch == 'develop') {
            return name + "-test"
        } else if (branch == 'uat') {
            return name + "-uat"
        } else {
            return name + "-dev"
        }
    }

    def getDockerEnvFromBranch(branch) {
        if (branch == 'master') {
            return "-e 'SPRING_PROFILES_ACTIVE=prod'"
        } else if (isTest(branch)) {
            return "-e 'SPRING_PROFILES_ACTIVE=test'"
        } else {
            return "-e 'SPRING_PROFILES_ACTIVE=dev'"
        }
    }

    def getBackupDirFromBranch(branch) {
        if (branch == 'master') {
            return "sentry-prod"
        } else if (isTest(branch)) {
            return "sentry-test"
        } else {
            return "sentry-test"
        }
    }

    def getProfile(branch) {
        if (branch == 'master') {
            return "-Pprod"
        } else if (isTest(branch)) {
            return "-Ptest"
        } else {
            return "-Ptestcontainers"
        }
    }

    def getProfiles() {
        return ["-Pdev", "-Ptest", "-Pprod"]
    }

    def getSpringBootProfile(branch) {
        if (branch == 'master') {
            return "-Pprod"
        } else if (isTest(branch)) {
            return "-Ptest"
        } else {
            return "-Pdev"
        }
    }

    def isTest(branch) {
        return branch == 'develop' || branch.startsWith('uat')
    }

    def getPlaybook(branch) {
        if (branch == 'master') {
            return "playbook-prod.yml"
        } else if (isTest(branch)) {
            return "playbook-test.yml"
        } else {
            return "playbook-dev.yml"
        }
    }

    def isRelease(branch) {
        return branch == 'master'
    }

    def isVersion(branch) {
        return branch == 'develop'
    }

    def isReleaseable(branch) {
        return isRelease(branch) || isVersion(branch)
    }

    def isPullRequest(branch) {
        return branch.startsWith('PR')
    }

    def getReleaseEnvironments() {
        return ["dev", "test", "prod", "staging"]
    }
}