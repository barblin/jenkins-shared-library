#!/usr/bin/env groovy
import io.build.infra.jenkins.config.artefact_environment

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    properties([office365ConnectorWebhooks(teamsConnector())])

    timeout(20) {
        node('master') {
            cur_branch = env.BRANCH_NAME
            buildEnv = new artefact_environment()

            try {
                jdk = tool name: 'jdk-8'
                env.JAVA_HOME = "${jdk}"
                echo "jdk installation path is: ${jdk}"


                withMaven(maven: 'Maven 3.6.3') {
                    repo.checkout()
                    build.buildMaven(buildEnv.getProfile(cur_branch))
                }

                def proceed = true;
                if (cur_branch == 'master' || cur_branch == 'develop' || cur_branch.startsWith('uat')) {
                    echo "VPN connection for branch: " + cur_branch
                    proceed = vpn.connectToH3AVPN()
                }

                if (proceed) {
                    deploy.deployPAppToH3AServer(buildEnv.getPlaybook(cur_branch))
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