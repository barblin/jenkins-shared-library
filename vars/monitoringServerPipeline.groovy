#!/usr/bin/env groovy

import io.alanda.infra.jenkins.config.monitoring_environment

import static io.alanda.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    buildEnv = new monitoring_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()
    properties([
            office365ConnectorWebhooks(teamsConnector()),
            parameters([
                    choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Where do you want to deploy the artefact?'),
            ])])

    timeout(30) {
        node('master') {
            serverData = serverData.build(params.ENVIRONMENT, buildEnv)
            user = serverData[0]
            host = serverData[1]
            cur_branch = env.BRANCH_NAME
            environment = params.ENVIRONMENT ? params.ENVIRONMENT : buildEnv.getEnvFromBranch(cur_branch)
            try {
                repo.checkout()
                stage('Start deployment') {
                    ansible_extra_vars = "remotehost=${host} user=${user} " +
                            " localpath=. remoteservicepath=/var/lib"

                    command = "ansible-playbook"
                    if (params.ENVIRONMENT == 'setup') {
                        command += " -i ${host},"
                    }
                    command += " playbook-monitoring-server.yml --extra-vars \"${ansible_extra_vars}\""
                    sh command
                }
            }
            catch (e) {
                exception.handle(e)
                throw e
            }
        }
    }
}