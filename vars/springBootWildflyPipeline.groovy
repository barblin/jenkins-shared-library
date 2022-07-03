#!/usr/bin/env groovy

import io.build.infra.jenkins.config.artefact_environment

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    buildEnv = new artefact_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()
    artifactProfiles = buildEnv.getProfiles()
    cur_branch = env.BRANCH_NAME
    smoke_tests_enabled = env.enableSmokeTest

    properties([
            office365ConnectorWebhooks(teamsConnector()),
            parameters([
                    choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Where do you want to deploy the artefact?'),
                    choice(name: 'PROFILE', choices: artifactProfiles, description: 'Where do you want to deploy the artefact?')
            ])
    ])
    timeout(30) {
        node('master') {

            if (params.ENVIRONMENT == null) {
                deployServer = buildEnv.getTargetServerFromBranch(cur_branch)
                deployFolder = buildEnv.getDeployFolderFromBranch(cur_branch)
                deployUser = buildEnv.getUserFromBranch(cur_branch)
                dockerName = buildEnv.getDockerNameFromBranch(cur_branch)
            } else {
                deployServer = buildEnv.getTargetServerFromEnvironment(params.ENVIRONMENT)
                deployFolder = buildEnv.getDeployFolderFromEnvironment(params.ENVIRONMENT)
                deployUser = buildEnv.getUserFromEnvironment(params.ENVIRONMENT)
                dockerName = buildEnv.getDockerNameFromEnvironment(params.ENVIRONMENT)
            }

            try {
                jdk = tool name: 'jdk-8'
                env.JAVA_HOME = "${jdk}"
                echo "jdk installation path is: ${jdk}"


                withMaven(maven: 'Maven 3.6.3') {
                    repo.checkout()

                    def profile = params.PROFILE != null ? params.PROFILE : buildEnv.getProfile(cur_branch)
                    build.buildMaven(profile)
                    qualityGate.sonarQubeAnalysis()

                    if (cur_branch == 'master' || cur_branch == 'develop' || cur_branch.startsWith('PR')) {
                        integration.integrationTest()
                    }

                    if (cur_branch == 'master' || buildEnv.isTest(cur_branch)) {
                        publishStep.uploadToRegistry(profile)
                    }
                }

                def proceed = true;
                if (cur_branch == 'master' || buildEnv.isTest(cur_branch)) {
                    echo "VPN connection for " + cur_branch
                    proceed = vpn.connectToH3AVPN()
                }

                if (proceed) {
                    deploy.deployWarToH3AServer(
                            deployServer, deployFolder, deployUser, pipelineParams.filename, dockerName)
                    publishStep.apiToRegistry(pipelineParams.apiFiles)
                }
            }
            catch (e) {
                exception.handle(e)
                throw e
            }
            finally {
                vpn.close()
            }
        }
    }
}