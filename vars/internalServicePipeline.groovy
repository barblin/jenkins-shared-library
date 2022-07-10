#!/usr/bin/env groovy

import io.build.infra.jenkins.config.artefact_environment

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    buildEnv = new artefact_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()
    properties([
            office365ConnectorWebhooks(teamsConnector()),
            parameters([
                    choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Where do you want to deploy the artefact?'),
            ])])

    timeout(30) {
        node('master') {
            buildEnv = new artefact_environment()

            cur_branch = env.BRANCH_NAME
            filename = pipelineParams.name
            port = pipelineParams.port

            // Environment variables
            imagename = params.ENVIRONMENT ? "${filename}" + "-" + "${params.ENVIRONMENT}" : buildEnv.getImageNameFromBranch("${filename}", cur_branch)
            environment = params.ENVIRONMENT ? params.ENVIRONMENT : buildEnv.getEnvFromBranch(cur_branch)

            // Docker configuration
            dockerImage = ''

            try {
                jdk = tool name: 'jdk-11'
                env.JAVA_HOME = "${jdk}"
                echo "jdk installation path is: ${jdk}"
                withMaven(maven: 'Maven 3.8.1') {
                    repo.checkout()
                    build.buildMaven(buildEnv.getSpringBootProfile(cur_branch))
                    dockerImage = build.buildImage(dockerImage, imagename)
                    qualityGate.sonarQubeAnalysis()
                    if (cur_branch == 'master' || cur_branch == 'develop' || cur_branch.startsWith('PR')) {
                        integration.integrationTest()
                    }
                }
                publishStep.uploadToRegistry(imagename, filename, dockerImage, buildEnv.repoCredentials())
                deploy.deployDockerImageToServer(environment, imagename)
                publishStep.apiToRegistry(pipelineParams.apiFiles)
            }
            catch (e) {
                exception.handle(e)
                throw e
            }
        }
    }
}