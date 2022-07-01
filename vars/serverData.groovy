def build(systemEnv, buildEnv) {
    stage('Collect Environment Info') {
        def user = ''
        def host = ''

        if (systemEnv == 'dev') {
            user = buildEnv.getDevUser()
            host = buildEnv.getDevHost()
        } else if (systemEnv == 'test') {
            user = buildEnv.getTestUser()
            host = buildEnv.getTestHost()
        } else if (systemEnv == 'prod') {
            user = buildEnv.getProdUser()
            host = buildEnv.getProdHost()
        } else if (systemEnv == 'staging'){
            user = buildEnv.getStagingUser()
            host = buildEnv.getStagingHost()
        }
        else {
            def userInput = input(
                    id: 'userInput', message: 'Please provide new server information',
                    parameters: [
                            string(defaultValue: 'root',
                                    description: 'Host server user',
                                    name: 'user'),
                            string(defaultValue: '',
                                    description: 'Host server IP address',
                                    name: 'host'),
                    ])
            user = userInput.user ?: ''
            host = userInput.host ?: ''
        }

        return [user, host]
    }
}