plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
}

springBoot {
    mainClass.set("org.railwaystations.rsapi.RsapiApplicationKt")
}

tasks {
    bootJar {
        mainClass.set("org.railwaystations.rsapi.RsapiApplicationKt")
    }
}

tasks.withType<Test> {
    environment(
        "LIQUIBASE_DUPLICATE_FILE_MODE",
        "WARN"
    ) // because of bug: https://stackoverflow.com/questions/77301370/unable-to-set-duplicatefilemode-property-in-application-yaml-for-liquibase
}

dependencies {
    implementation(project("::core"))
    implementation(project("::openapi"))
    implementation(project("::adapter"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.security.oauth2.authorization.server)
    implementation(libs.liquibase.core)
    implementation("commons-codec:commons-codec")
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    kapt(libs.spring.boot.configuration.processor)

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("org.webjars:webjars-locator-core")

    testImplementation(testFixtures(project("::core")))
    testImplementation(testFixtures(project("::adapter")))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.swagger.request.validator.core)
    testImplementation(libs.swagger.request.validator.spring.webmvc)
    testImplementation(libs.swagger.request.validator.mockmvc)
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.xmlunit:xmlunit-assertj3")
    testImplementation(libs.json.unit.spring)
    testImplementation(libs.wiremock.jre8.standalone)
    testImplementation(libs.mockk)
    testImplementation(libs.springmockk)
    testImplementation(libs.greenmail)

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
