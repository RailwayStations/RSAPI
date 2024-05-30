plugins {
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        // workaround for https://github.com/OpenAPITools/openapi-generator/issues/18753
        classpath("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml") {
            version { strictly("2.14.2") }
        }
    }
}

openApiValidate {
    inputSpec = "$projectDir/src/main/resources/static/openapi.yaml"
    recommend = true
}

openApiGenerate {
    generatorName = "kotlin-spring"
    inputSpec = "$projectDir/src/main/resources/static/openapi.yaml"
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(rootProject.project.libs.swagger.annotations)
    implementation("jakarta.validation:jakarta.validation-api")
}