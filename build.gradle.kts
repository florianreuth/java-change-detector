import de.florianreuth.baseproject.configureApplication
import de.florianreuth.baseproject.configureShadedDependencies
import de.florianreuth.baseproject.setupProject

plugins {
    id("de.florianreuth.baseproject")
}

setupProject()
configureApplication()

val shade = configureShadedDependencies()

dependencies {
    shade("com.google.code.gson:gson:2.13.2")
    shade("com.jayway.jsonpath:json-path:2.10.0")
    shade("org.apache.logging.log4j:log4j-api:2.25.3")
    shade("org.apache.logging.log4j:log4j-core:2.25.3")
}

