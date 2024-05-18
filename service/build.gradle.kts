plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

springBoot {
    mainClass.set("org.railwaystations.rsapi.app.RsapiApplicationKt")
}

tasks {
    bootJar {
        mainClass.set("org.railwaystations.rsapi.app.RsapiApplicationKt")
    }
}

dependencies {
    implementation(project("::openapi"))
    implementation(project("::core"))
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation(libs.spring.security.oauth2.authorization.server)
    implementation("org.liquibase:liquibase-core")
    implementation(libs.jdbi3.spring5)
    implementation(libs.jdbi3.kotlin)
    implementation(libs.jdbi3.kotlin.sqlobject)
    implementation(libs.lazysodium.java)
    implementation(libs.jna)
    implementation("commons-codec:commons-codec")
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.bootstrap)

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("org.webjars:webjars-locator-core")

    testImplementation(testFixtures(project("::core")))
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
