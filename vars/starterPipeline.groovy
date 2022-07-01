#!/usr/bin/env groovy

import io.alanda.infra.jenkins.config.artefact_environment

import static io.alanda.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    properties([office365ConnectorWebhooks(teamsConnector())])

    timeout(15) {
        node('master') {
            buildEnv = new artefact_environment()

            cur_branch = env.BRANCH_NAME

            try {
                jdk = tool name: 'jdk-8'
                env.JAVA_HOME = "${jdk}"
                echo "jdk installation path is: ${jdk}"

                withMaven(maven: 'Maven 3.6.3') {
                    repo.checkout()
                    build.buildMaven(buildEnv.getProfile(cur_branch))
                    qualityGate.sonarQubeAnalysis()
                    integration.integrationTest()
                    publishStep.uploadToRegistry(buildEnv.getProfile(cur_branch))
                }
            }
            catch (e) {
                exception.handle(e)
                throw e
            }
        }
    }
}