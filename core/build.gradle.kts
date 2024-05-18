plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.spring.boot.starter.security)
    implementation(libs.commons.lang3)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
}
