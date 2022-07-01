#!/usr/bin/env groovy

def select() {
    pom = readMavenPom file: "pom.xml"
    version = pom.properties['maven.compiler.source']

    if (version == "11") {
        return "jdk-11"
    }

    return "jdk-8"
}