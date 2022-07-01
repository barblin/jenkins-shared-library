def close(){
    sh "sudo killall -SIGINT openconnect || true"
}

def connectToH3AVPN(){
    connectToGlobalProtectVPN('')
}

def connectToGlobalProtectVPN(url){
    stage('VPN Connect') {
        def proceed = true
        try {
            timeout(time: 5, unit: 'MINUTES') {


                def userInput = input(
                        id: 'userInput', message: 'Enter your VPN Credentials:?',
                        parameters: [

                                string(defaultValue: '',
                                        description: 'User',
                                        name: 'User'),
                                string(defaultValue: '',
                                        description: 'Token',
                                        name: 'Token'),
                        ])

                // Save to variables. Default to empty string if not found.
                def user = userInput.User ?: ''
                def token = userInput.Token ?: ''
                sh "echo ${token} | sudo openconnect --background --authgroup=Employees_MAIN " +
                        "--dump-http-traffic --protocol=gp --user=${user} ${url}"
            }
        } catch (err) {
            proceed = false
        }

        return proceed
    }
}