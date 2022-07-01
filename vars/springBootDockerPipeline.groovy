#!/usr/bin/env groovy

import io.alanda.infra.jenkins.config.artefact_environment

import static io.alanda.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    properties([office365ConnectorWebhooks(teamsConnector())])

    timeout(30) {
        node('master') {
            buildEnv = new artefact_environment()

            cur_branch = env.BRANCH_NAME
            filename = pipelineParams.name
            port = pipelineParams.port

            // Environment variables
            deployServer = buildEnv.getTargetServerFromBranch(cur_branch)
            deployUser = buildEnv.getUserFromBranch(cur_branch)
            deployFolder = buildEnv.getBackupDirFromBranch(cur_branch)
            imagename = buildEnv.getImageNameFromBranch("erp/${filename}", cur_branch)

            // Docker configuration
            dockerImage = ''
            volume = "--mount type=bind,src=/appl/pmc/docker/${deployFolder}/backup,dst=/home/backup"
            ports = "--expose " + port + " -p " + port + ":" + port
            springEnv = buildEnv.getDockerEnvFromBranch(cur_branch)
            command = "docker run --network=host -d --name=${filename} ${springEnv} ${volume} ${ports} " +
                    "--env JAVA_OPTS='-Duser.timezone=UTC' ${imagename + ':$BUILD_NUMBER'}"

            try {
                jdk = tool name: 'jdk-11'
                env.JAVA_HOME = "${jdk}"
                echo "jdk installation path is: ${jdk}"

                withMaven(maven: 'Maven 3.8.1') {
                    repo.checkout()
                    build.buildMaven(buildEnv.getProfile(cur_branch))
                    qualityGate.sonarQubeAnalysis()
                    if (cur_branch == 'master' || cur_branch == 'develop' || cur_branch.startsWith('PR')) {
                        integration.integrationTest()
                    }
                }

                dockerImage = build.buildImage(dockerImage, imagename)
                publishStep.uploadToRegistry(imagename, filename, dockerImage, buildEnv.repoCredentials())

                def proceed = true;
                if (cur_branch == 'master' || cur_branch == 'develop' || cur_branch.startsWith('uat')) {
                    proceed = vpn.connectToH3AVPN()
                }

                if(proceed) {
                    deploy.deployDockerImageToH3AServer(deployUser, deployServer, deployFolder, filename, command)
                    publishStep.apiToRegistry(pipelineParams.apiFiles)
                }
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