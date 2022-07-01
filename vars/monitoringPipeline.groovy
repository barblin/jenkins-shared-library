#!/usr/bin/env groovy

import io.alanda.infra.jenkins.config.monitoring_environment

import static io.alanda.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    buildEnv = new monitoring_environment()
    releaseEnvironments = buildEnv.getReleaseEnvironments()

    properties([
            office365ConnectorWebhooks(teamsConnector()),
            parameters([
                    choice(name: 'ENVIRONMENT', choices: releaseEnvironments, description: 'Where do you want to deploy the monitoring server-side setup?'),
                    string(name: "HOST", trim: true, description: "IP for the server you wish to monitor"),
                    string(name: "USER", defaultValue: "root", trim: true, description: "User for the server you wish to monitor"),
                    string(name: "ELASTIC_HOST", defaultValue: "elasticsearch", trim: true, description: "Hostname of the monitoring elastic server"),
                    string(name: "ELASTIC_PORT", defaultValue: "9200", trim: true, description: "Port of the monitoring elastic server"),
                    string(name: "ELASTIC_USER", defaultValue: "elastic", trim: true, description: "User for the monitoring elastic server"),
                    password(name: "ELASTIC_PASSWORD", description: "Hostname of the monitoring elastic server"),
                    string(name: "LOGSTASH_HOST", defaultValue: "logstash", trim: true, description: "Hostname of the logstash server"),
                    string(name: "LOGSTASH_PORT", defaultValue: "5044", trim: true, description: "Port of the logstash server"),
                    booleanParam(defaultValue: false, name: 'VPN_REQUIRED', description: 'Drei VPN Required?')
            ])])

    timeout(30) {
        node('master') {
            try {
                repo.checkout()

                if (params.VPN_REQUIRED) {
                    vpn.connectToH3AVPN()
                }
                stage('Start deployment') {
                    ansible_extra_vars = "remotehost=${params.HOST} user=${params.USER} mode=${params.ENVIRONMENT} " +
                            "localconfigpath=./config localpath=. remoteservicepath=/var/lib " +
                            "elastichost=${params.ELASTIC_HOST} elasticport=${params.ELASTIC_PORT} " +
                            "elasticuser=${params.ELASTIC_USER} elasticpassword=${params.ELASTIC_PASSWORD} " +
                            "logstashhost=${params.LOGSTASH_HOST} logstashport=${params.LOGSTASH_PORT}"

                    command = "ansible-playbook"
                    if (params.ENVIRONMENT == 'setup') {
                        command += " -i ${host},"
                    }
                    command += " playbook_monitoring.yml --extra-vars \"${ansible_extra_vars}\""
                    sh command
                }

            } catch (e) {
                exception.handle(e)
                throw e
            } finally {
                vpn.close()
            }
        }
    }
}