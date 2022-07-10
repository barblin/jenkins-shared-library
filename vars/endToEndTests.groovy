import static io.build.infra.jenkins.config.notifications.teamsEndToEnd


def run(server, user, bindPort, environment) {
    stage("End To End Tests") {
        sh "ssh -N -L ${bindPort}:localhost:8080 ${user}@${server} >/dev/null 2>&1 &"

        try {
            sh "yarn cypress run"
        } catch (ex) {
            testsFailed(environment)
            throw ex
        }
    }

    testsPassed(environment)
}

def testsFailed(environment) {
    office365ConnectorSend webhookUrl: teamsEndToEnd(),
            message: "Stage ${environment.toUpperCase()} was not able to pass the (e2e) RELEASE checks.",
            status: "${environment} is not releasbale!",
            color: '#0000FF'
}

def testsPassed(environment) {
    office365ConnectorSend webhookUrl: teamsEndToEnd(),
            message: "Stage ${environment.toUpperCase()} successfully passed all (e2e) RELEASE checks.",
            status: 'SUCCESS',
            color: '##00FF00'
}