#!/usr/bin/env groovy

import io.alanda.infra.jenkins.utils.dump

import static io.alanda.infra.jenkins.config.notifications.teamsConnector

def call(pipelineParams) {
    properties([
            parameters([
                    booleanParam(name: 'CLEAN_WS', defaultValue: false, description: 'Clean Workspace before Build?'),
                    booleanParam(name: 'E2E_TESTS', defaultValue: false, description: 'Run E2E Tests?'),
                    booleanParam(name: 'QUALITY_GATE', defaultValue: true, description: 'Check SonarQube Quality Gate?'),
            choice(name: 'NODE_VERSION',defaultValue: 'v16.13.2',choices: ['v16.13.2','v12.16.2'], description:
                        'Set Node ' +
                        'Version')
        ]),

            office365ConnectorWebhooks(teamsConnector())
    ])

    timeout(30) {
        node('master') {
            cur_branch = env.BRANCH_NAME

            // override default cache directories
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

                nvm(params.NODE_VERSION) {
                    stage('Node Packages') {
                        sh 'yarn --frozen-lockfile'
                    }

                    stage('Static Analysis') {
                        sh 'yarn run lint'
                    }

                    stage('Unit Tests') {
                        sh 'yarn run test'
                    }

                    stage('Build') {
                        sh 'yarn run build'
                    }
                }

                stage('SonarQube Analysis') {
                    def scannerHome = tool 'My SonarQube Server';
                    withSonarQubeEnv('My SonarQube Server') {
                        sh "${scannerHome}/bin/sonar-scanner"
                    }
                }

                stage('Quality Gate') {
                    if (params.QUALITY_GATE) {
                        timeout(time: 5, unit: 'MINUTES') {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                error "Pipeline aborted due to quality gate failure: ${qg.status}"
                            }
                        }
                    } else {
                        echo 'skipping stage...'
                        org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Quality Gate')
                    }
                }

                stage('E2E Tests') {
                    if (params.E2E_TESTS || cur_branch == 'master' || cur_branch == 'develop' || cur_branch.startsWith('PR')) {

                        nvm(params.NODE_VERSION) {
                            sh 'yarn run e2e'
                        }
                    } else {
                        echo 'skipping stage...'
                        org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('E2E Tests')
                    }
                }
            }
            catch (e) {
                exception.handle(e)
                throw e
            }
        }
    }
}
