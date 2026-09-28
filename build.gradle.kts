plugins {
    id("base.java")
    id("base.application")
    id("configuration.shaded_dependencies")
}

dependencies {
    shadedDependencies(libs.gson)
    shadedDependencies(libs.json.path)
    shadedDependencies(libs.log4j.api)
    shadedDependencies(libs.log4j.core)
}

