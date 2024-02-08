import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
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
    implementation(libs.spring.security.oauth2.authorization.server)
    implementation("org.liquibase:liquibase-core")
    implementation(libs.jdbi3.spring5)
    implementation(libs.jdbi3.kotlin)
    implementation(libs.jdbi3.kotlin.sqlobject)
    implementation(libs.lazysodium.java)
    implementation(libs.jna)
    implementation("commons-codec:commons-codec")
    implementation(libs.commons.io)
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.swagger.annotations)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.jakarta.validation.api)
    implementation(libs.bootstrap)

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("org.webjars:webjars-locator-core")

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