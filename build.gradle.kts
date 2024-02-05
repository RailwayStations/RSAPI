import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.22"

    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.openapi.generator") version "7.2.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("kapt") version kotlinVersion
}

group = "org.railwaystations"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

springBoot {
    mainClass.set("org.railwaystations.rsapi.app.RsapiApplicationKt")
}

tasks {
    bootJar {
        mainClass.set("org.railwaystations.rsapi.app.RsapiApplicationKt")
    }
}

openApiValidate {
    inputSpec = "$rootDir/src/main/resources/static/openapi.yaml"
    recommend = true
}

openApiGenerate {
    generatorName = "kotlin-spring"
    inputSpec = "$rootDir/src/main/resources/static/openapi.yaml"
    outputDir = layout.buildDirectory.file("openapi").get().asFile.toString()
    apiPackage = "org.railwaystations.rsapi.adapter.web.api"
    modelPackage = "org.railwaystations.rsapi.adapter.web.model"
    modelNameSuffix = "Dto"
    cleanupOutput = true
    configOptions.set(
        mapOf(
            "sourceFolder" to "",
            "useTags" to "true",
            "interfaceOnly" to "true",
            "documentationProvider" to "none",
            "useBeanValidation" to "true",
            "useSpringBoot3" to "true",
            "enumPropertyNaming" to "UPPERCASE"
        )
    )
    typeMappings.set(
        mapOf(
            "number" to "Long",
        )
    )
    importMappings.set(
        mapOf(
            "Long" to "kotlin.Long"
        )
    )
}

sourceSets {
    main {
        kotlin.srcDirs("build/openapi")
    }
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs>().configureEach {
    dependsOn(tasks.openApiGenerate)
}

val testContainersVersion = "1.19.4"
val jdbiVersion = "3.44.0"
val swaggerRequestValidatorVersion = "2.40.0"

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.security:spring-security-oauth2-authorization-server:1.2.1")
    implementation("org.liquibase:liquibase-core")
    implementation("org.jdbi:jdbi3-spring5:$jdbiVersion")
    implementation("org.jdbi:jdbi3-kotlin:$jdbiVersion")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:$jdbiVersion")
    implementation("com.goterl:lazysodium-java:5.1.4")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("commons-codec:commons-codec")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.apache.commons:commons-lang3")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("org.webjars:bootstrap:5.3.2")

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("org.webjars:webjars-locator-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:mariadb:$testContainersVersion")
    testImplementation("com.atlassian.oai:swagger-request-validator-core:$swaggerRequestValidatorVersion")
    testImplementation("com.atlassian.oai:swagger-request-validator-spring-webmvc:$swaggerRequestValidatorVersion")
    testImplementation("com.atlassian.oai:swagger-request-validator-mockmvc:$swaggerRequestValidatorVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.xmlunit:xmlunit-assertj3")
    testImplementation("net.javacrumbs.json-unit:json-unit-spring:3.2.4")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.test {
    jvmArgs(
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict -java-parameters"
        jvmTarget = "21"
    }
}