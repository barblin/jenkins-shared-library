#!/usr/bin/env groovy

import io.build.infra.jenkins.config.artefact_environment
import io.build.infra.jenkins.utils.dump

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {

    buildEnv = new artefact_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()


    properties([office365ConnectorWebhooks(teamsConnector()),
                parameters([
                        choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Which environment do you want to test?'),
                        booleanParam(defaultValue: false, name: 'VPN_REQUIRED', description: 'VPN Required?')
                ])])

    timeout(30) {
        node('master') {

            cur_branch = env.BRANCH_NAME

            server = buildEnv.getTargetServerFromEnvironment(params.ENVIRONMENT)
            user = buildEnv.getUserFromBranch(cur_branch)
            endToEndPort = buildEnv.getEndToEndPort()

            env.NPM_CONFIG_CACHE = "${WORKSPACE}/.npm"
            env.CYPRESS_CACHE_FOLDER = "${JENKINS_HOME}/.cache/Cypress"

            try {
                utilsDump = new dump(script: this)

                utilsDump.dumpParameter(params)

                if (params.CLEAN_WS) {
                    echo 'Cleaning Workspace before Build ...'
                    cleanWs();
                }

                repo.checkout()

                if (params.VPN_REQUIRED) {
                    vpn.connectToH3AVPN()
                }

                sh "yarn install"

                endToEndTests.run(server, user, endToEndPort, params.ENVIRONMENT)
            }
            catch (e) {
                exception.handle(e)
                throw e
            }
            finally {
                sh "ps -o pid,cmd|grep \"ssh -L\" | awk '{print \$1}'|xargs kill || true"
                vpn.close()
            }
        }
    }
}