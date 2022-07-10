def deployDockerImageToH3AServer(deployUser, deployServer, deployFolder, filename, command){
    stage('Deploy Docker Image to Stage') {
        sh "scp ./${filename}.tar ${deployUser}@${deployServer}:/appl/product/docker/${deployFolder}/tmp/"
        sh "ssh ${deployUser}@${deployServer} chmod 777 /appl/product/docker/${deployFolder}/tmp/${filename}.tar"
        sh "ssh ${deployUser}@${deployServer} docker load --input /appl/product/docker/${deployFolder}/tmp/${filename}.tar"
        sh "ssh ${deployUser}@${deployServer} docker stop ${filename} || true"
        sh "ssh ${deployUser}@${deployServer} docker rm ${filename} || true"
        sh "ssh ${deployUser}@${deployServer} ${command}"
        sh "ssh ${deployUser}@${deployServer} docker image prune"
    }
}

def deployDockerImageToServer(context, imagename){
    stage('Deploy Docker Image to Stage') {
        sh "docker --context ${context} network inspect ${imagename}_bridge >/dev/null 2>&1 || docker --context ${context} network create ${imagename}_bridge"
        sh "docker context use ${context}"
        sh "docker-compose --context ${context} --env-file env/env.${context} --project-name ${imagename} down "
        sh "docker-compose --context ${context} --env-file env/env.${context} pull"
        sh "docker-compose --context ${context} --env-file env/env.${context} --project-name ${imagename} up --force-recreate -d"
        sh "docker context use default"
    }
}

def deployWarToH3AServer(deployServer, deployFolder, deployUser, filename, dockerName) {
    stage('Deploy War to Wildfly') {
        sleep(time:2,unit:"SECONDS")
        sh "ssh ${deployUser}@${deployServer} docker stop ${dockerName}"
        sh "ssh ${deployUser}@${deployServer} rm /appl/product/docker/test/mount_deployment/${filename} || true"
        sh "scp target/${filename} ${deployUser}@${deployServer}:/appl/product/docker/${deployFolder}/mount_deployment/"
        sh "ssh ${deployUser}@${deployServer} docker start ${dockerName}"
    }
}

def deployPAppToH3AServer(playbook) {
    stage('Deploy pApp to Wildfly') {
        sh "ansible-playbook ${playbook}"
    }
}