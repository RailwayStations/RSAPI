import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt) apply false
}

group = "org.railwaystations.rsapi"
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
        plugin(rootProject.project.libs.plugins.spring.dependency.management.get().pluginId)
    }
    dependencyManagement {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }
    repositories {
        mavenCentral()
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        }
    }
}


