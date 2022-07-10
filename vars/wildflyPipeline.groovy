#!/usr/bin/env groovy

import io.build.infra.jenkins.config.wildfly_environment

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    buildEnv = new wildfly_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()
    String cron_string = BRANCH_NAME == "master" ? "0 23 * * *" : ""

    properties([
            pipelineTriggers([cron(cron_string)]),
            office365ConnectorWebhooks(teamsConnector()),
            parameters([
                    choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Where do you want to deploy wildfly?'),
                    booleanParam(defaultValue: false, name: 'VPN_REQUIRED', description: 'VPN Required?')
            ])])

    timeout(30) {
        node('master') {
            office365ConnectorSend message: "Starting automatic WILDFLY ${params.ENVIRONMENT} deployment"

            try {
                repo.checkout()

                serverData = serverData.build(params.ENVIRONMENT, buildEnv)
                user = serverData[0]
                host = serverData[1]

                if (params.VPN_REQUIRED) {
                    vpn.connectToH3AVPN()
                }

                stage('Prepare email') {
                    sh "echo \"AUTOMATIC JENKINS WILDFLY DEPLOYMENT\" >>./tmp/temp_mail_file.txt"
                }

                image_name = "${params.ENVIRONMENT}-image-" + buildEnv.getVersion()
                stage('Build docker image') {
                    script {
                        docker.withRegistry(buildEnv.registry(), buildEnv.credentials()) {
                            image = docker.image(buildEnv.image_name())
                            image.pull()
                            sh "./scripts/save_custom_base_image.sh ./ ${image_name}.tar.gz /tmp ${image_name}"
                        }
                    }
                }

                stage('Start deployment') {
                    remote_root_dir = "/appl/product/docker/"
                    remote_install_dir = remote_root_dir + "${params.ENVIRONMENT}-es7"
                    service_image_name = "${params.ENVIRONMENT}-es7-image"
                    container_name = "${params.ENVIRONMENT}-es7-node"
                    local_service_dir = "./docker_${params.ENVIRONMENT}"

                    ansible_extra_vars = "host=${host} user=${user} mode=${params.ENVIRONMENT} " +
                            "remoteservicepath=${remote_install_dir} devuser=JENKINS baseimagename=${image_name} " +
                            "serviceimagename=${service_image_name} servicecontainername=${container_name} " +
                            "localservicepath=${local_service_dir} "

                    command = "ansible-playbook"
                    if (params.ENVIRONMENT == 'setup') {
                        command += " -i ${host},"
                    }
                    command += " playbook_wildfly.yml --extra-vars \"${ansible_extra_vars}\""
                    sh command
                }

            } catch (e) {
                exception.handle(e)
                sh "rm ./tmp/temp_mail_file.txt"
                throw e
            } finally {
                vpn.close()
            }
        }
    }
}