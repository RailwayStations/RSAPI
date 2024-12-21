plugins {
    alias(libs.plugins.spring.boot)
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

dependencies {
    implementation(project("::core"))
    implementation(project("::openapi"))
    implementation(project("::db-migration"))
    implementation(project("::adapter"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.spring.security.oauth2.authorization.server)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.jooq.core)

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.webjars:webjars-locator-lite")

    testImplementation(testFixtures(project("::core")))
    testImplementation(testFixtures(project("::adapter")))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(libs.swagger.request.validator.core)
    testImplementation(libs.swagger.request.validator.spring.webmvc)
    testImplementation(libs.swagger.request.validator.mockmvc)
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.xmlunit:xmlunit-assertj3")
    testImplementation(libs.json.unit.spring)
    testImplementation(libs.wiremock.jre8.standalone)
    testImplementation(libs.mockk)
    testImplementation(libs.springmockk)

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
