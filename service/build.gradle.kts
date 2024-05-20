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
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.spring.security.oauth2.authorization.server)
    implementation("org.liquibase:liquibase-core")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

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
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mariadb")
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
