#!/usr/bin/env groovy

import io.build.infra.jenkins.config.artefact_environment

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    properties([office365ConnectorWebhooks(teamsConnector())])

    timeout(30) {
        node('master') {
            cur_branch = env.BRANCH_NAME
            buildEnv = new artefact_environment()

            try {
                withMaven(maven: 'Maven 3.6.3') {
                    repo.checkout()
                    dir('backend') {
                        jdk = tool name: javaRuntime.select()
                        env.JAVA_HOME = "${jdk}"
                        echo "jdk installation path is: ${jdk}"

                        if (buildEnv.isPullRequest(cur_branch)) {
                            versions.revisionCheck()
                        }

                        build.buildMaven('')

                        if (buildEnv.isRelease(cur_branch)) {
                            versions.releaseRevision()
                        }

                        if (buildEnv.isVersion(cur_branch)) {
                            versions.revision()
                        }

                        qualityGate.sonarQubeAnalysis()
                    }
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