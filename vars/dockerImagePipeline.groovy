#!/usr/bin/env groovy

import io.alanda.infra.jenkins.config.artefact_environment

import static io.alanda.infra.jenkins.config.notifications.teamsConnector
import static io.alanda.infra.jenkins.config.notifications.teamsLibraryRelease

def call(pipelineParams) {
    properties([office365ConnectorWebhooks(teamsConnector())])

    timeout(30) {
        node('master') {
            buildEnv = new artefact_environment()

            cur_branch = env.BRANCH_NAME
            filename = pipelineParams.name
            port = pipelineParams.port

            imagename = buildEnv.getImageNameFromBranch("alanda/${filename}", cur_branch)

            try {
                jdk = tool name: 'jdk-11'
                env.JAVA_HOME = "${jdk}"
                echo "jdk installation path is: ${jdk}"

                withMaven(maven: 'Maven 3.8.1') {
                    repo.checkout()

                    if (buildEnv.isPullRequest(cur_branch)) {
                        versions.versionCheck(teamsLibraryRelease())
                    }

                    build.buildMaven('')

                    if (buildEnv.isRelease(cur_branch)) {
                        versions.releaseVersion(teamsLibraryRelease())
                    }

                    if (buildEnv.isVersion(cur_branch)) {
                        versions.version(teamsLibraryRelease())
                    }
                }

                dockerImage = build.buildImage('', imagename)
                publishStep.uploadToRegistry(imagename, filename, dockerImage, buildEnv.repoCredentials())
            }
            catch (e){
                exception.handle(e)
                throw e
            }
            finally{
                vpn.close()
            }
        }
    }
}