#!/usr/bin/env groovy
import static io.build.infra.jenkins.config.notifications.teamsWebhook
import static io.build.infra.jenkins.config.notifications.teamsWebhookVersioning
import groovy.transform.Field

@Field DUPLICATE_VERSION_ERROR = " already exists in develop. The pull request must have a unique version. <br>" +
        "Please consider increasing the version/revision number."

def revision() {
    pom = readMavenPom file: "pom.xml"
    assign(pom.properties['revision'])
}

def version() {
    pom = readMavenPom file: "pom.xml"
    assign(pom.version)
}

def revisionCheck() {
    pom = readMavenPom file: "pom.xml"
    doesExist(pom.properties['revision'])
}

def doesExist(version) {
    stage('Verify Version') {
        setGitUser()

        withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: '36c9de39-0be3-43e4-ba15-8f2c38b1f335\t',
                          usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
            repo = getRepo()

            sh "git fetch https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@${repo} --tags"

            if(isAlreadyTagged()){
                return
            }

            foundVersion = sh(script: "git tag -l \"${version}\"", returnStdout: true)

            if (foundVersion?.trim()) {
                sendVersioningAlreadyExistsError(version)
                error "Version tag: ${version}" + DUPLICATE_VERSION_ERROR
            }
        }
    }
}

def assign(version) {
    stage('Tag Version') {
        try {
            tag(version)
            sendReleaseCandidate(version)
        } catch (ex) {
            sendVersioningError(version)
            throw ex
        }
    }
}

def releaseRevision() {
    pom = readMavenPom file: "pom.xml"
    release(pom.properties['revision'])
}

def releaseVersion() {
    pom = readMavenPom file: "pom.xml"
    release(pom.version)
}

def release(version) {
    stage('Tag Release') {
        try {
            release = "Release_${version}"
            RELEASE_NOTES = tag(release)
            sendReleaseNotes(release, RELEASE_NOTES)
        } catch (ex) {
            sendReleaseError(release)
            throw ex
        }
    }
}

def tag(tag) {
    setGitUser()

    release_notes = ""

    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: '36c9de39-0be3-43e4-ba15-8f2c38b1f335\t',
                      usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
        repo = getRepo()

        sh "git fetch https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@${repo} --tags"
        release_notes = sh(script: "git log \$(git describe --tags --match \"[Release]*\" --abbrev=0)..HEAD --oneline", returnStdout: true).trim()

        echo "The current tag is ${tag}"
        sh "git tag -a ${tag} -m \"${tag}\""

        def command = "git push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@${repo} --tags"
        sh command
    }

    return release_notes
}

def sendReleaseNotes(release, message) {
    message = message.replaceAll("[\\t\\n\\r]+", "<br>");
    office365ConnectorSend webhookUrl: teamsWebhookVersioning(),
            message: """
                        <b>Release notes:</b> <br>
                        ${message} 
                    """,
            status: "${release} is ready!",
            color: '#00FF00'
}

def sendReleaseCandidate(version) {
    office365ConnectorSend webhookUrl: teamsWebhookVersioning(),
            message: "Release canddiate ${version} is ready for Release!",
            status: "Release canddiate ${version} is ready for Release!",
            color: '#0000FF'
}

def sendVersioningError(version) {
    office365ConnectorSend webhookUrl: teamsWebhookVersioning(),
            message: "Jenkins was not able to apply the current version: ${version}. " +
                    "\nAre you sure the current version is unique? Please increase version and rebuild.",
            status: 'Failed to create version',
            color: '#ffff00'
}

def sendReleaseError(release) {
    office365ConnectorSend webhookUrl: teamsWebhookVersioning(),
            message: "Jenkins was not able to tag ${release}. A release is not possible",
            status: 'Failed to tag release',
            color: '#ffff00'
}

def sendVersioningAlreadyExistsError(version) {
    office365ConnectorSend webhookUrl: teamsWebhook(),
            message: "Version tag: ${version}" + DUPLICATE_VERSION_ERROR,
            status: 'Version verification failed.',
            color: '#ffff00'
}

def setGitUser() {
    sh "git config --global user.email \"jenkins@no-reply.io\""
    sh "git config --global user.name \"Jenkins\""
}

def getRepo(){
    repo = sh(script: 'git config --get remote.origin.url', returnStdout: true).trim()
    def reg = "https://"
    return repo - reg
}

def isAlreadyTagged(){
    try {
        sh(script: 'git describe --exact-match HEAD', returnStdout: true)
    } catch (ex) {
        echo "Is not tagged"
        return false
    }
    echo "Is tagged"
    return true
}