plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.andrewzurn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:7.2.2")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:7.2.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
