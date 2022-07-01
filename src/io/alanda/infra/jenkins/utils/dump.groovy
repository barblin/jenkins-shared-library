package io.alanda.infra.jenkins.utils

class dump {
    Script script;

    def dumpParameter(params) {
        script.echo("*** PARAMETER ***")

        params.each {
            script.echo(it.key + " = " + it.value)
        }
    }
}