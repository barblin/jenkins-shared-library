package io.alanda.infra.jenkins.config

class notifications {
    static teamsWebhook() {
        throw new NotImplementedException("not implemented yet")
    }

    static teamsWebhookVersioning(){
        throw new NotImplementedException("not implemented yet")
    }

    static teamsConnector() {
        return [[
                 startNotification    : true, notifyBackToNormal: true, notifyFailure: true,
                 notifyRepeatedFailure: true, notifySuccess: true, notifyUnstable: true,
                 url                  : teamsWebhook()
         ]]
    }
}