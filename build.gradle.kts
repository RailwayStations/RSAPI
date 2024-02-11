import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "org.railwaystations"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

allprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict -java-parameters"
            jvmTarget = "21"
        }
    }

    tasks.register("compileAll") {
        group = "other"
        description = "Compiles all the modules."
        dependsOn(subprojects.map { it.tasks.named("compileKotlin") })
    }
}

subprojects {
    apply {
        plugin(rootProject.project.libs.plugins.kotlin.jvm.get().pluginId)
    }
    repositories {
        mavenCentral()
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            implementation("org.springframework.boot:spring-boot-starter-web")
            implementation("org.springframework.boot:spring-boot-starter-validation")
            implementation(rootProject.project.libs.swagger.annotations)
            implementation(rootProject.project.libs.jakarta.validation.api)
        }
    }
}


