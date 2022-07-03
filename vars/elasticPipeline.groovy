import io.build.infra.jenkins.config.elastic_environment

import static io.build.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    buildEnv = new elastic_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()
    String cron_string = BRANCH_NAME == "master" ? "0 22 * * *" : ""

    properties([
            pipelineTriggers([cron(cron_string)]),
            office365ConnectorWebhooks(teamsConnector()),
            parameters([
                    choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Where do you want to deploy elastic?'),
                    booleanParam(defaultValue: false, name: 'VPN_REQUIRED', description: 'Drei VPN Required?')
            ])])

    timeout(30) {
        node('master') {
            office365ConnectorSend message: "Starting automatic ELASTIC ${params.ENVIRONMENT} deployment"

            try {
                serverData = serverData.build(params.ENVIRONMENT, buildEnv)
                user = serverData[0]
                host = serverData[1]

                repo.checkout()

                if (params.VPN_REQUIRED) {
                    vpn.connectToH3AVPN()
                }

                stage('Prepare email') {
                    sh "echo \"AUTOMATIC JENKINS ELASTIC DEPLOYMENT\" >>./tmp/temp_mail_file.txt"
                }

                stage('Start deployment') {
                    remote_root_dir = "/appl/data/elastic/bin"
                    remote_install_dir = remote_root_dir + "/elasticsearch-" + buildEnv.getVersion()

                    root_dir = "./elastic"
                    local_src_path = root_dir + "/${params.ENVIRONMENT}/"

                    ansible_extra_vars = "host=${host} user=${user} mode=${params.ENVIRONMENT} " +
                            "remoteelasticpath=${remote_install_dir} devuser=JENKINS "

                    command = "ansible-playbook"
                    if (params.ENVIRONMENT == 'setup' || params.ENVIRONMENT == 'dev'|| params.ENVIRONMENT == 'staging') {
                        local_src_path = "./elastic/dev"

                        ansible_extra_vars += "localelasticpath=${local_src_path}/elasticsearch-7.14.0 " +
                                "localservicepath=${local_src_path} " +
                                "localservicerootpath=${root_dir} " +
                                "remoteservicerootpath=${remote_root_dir} " +
                                "ansible_python_interpreter=/usr/bin/python3"
                        command += " -i ${host},"

                    } else {
                        local_src_path = "./elastic/${params.ENVIRONMENT}/elasticsearch-7.14.0"

                        ansible_extra_vars += "localelasticpath=${local_src_path}/elasticsearch-7.14.0 " +
                                "localservicepath=${local_src_path} " +
                                "localservicerootpath=${root_dir} " +
                                "remoteservicerootpath=${remote_root_dir}"
                    }
                    command += " playbook_elastic.yml --extra-vars \"${ansible_extra_vars}\""
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