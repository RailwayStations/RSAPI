import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType
import org.jooq.meta.jaxb.Matchers
import org.jooq.meta.jaxb.MatchersFieldType
import org.jooq.meta.jaxb.MatchersTableType
import org.jooq.meta.jaxb.Strategy
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.testcontainers.postgresql)
        classpath(libs.flyway.postgresql)
        classpath(libs.postgresql)
    }
}

sourceSets {
    main {
        kotlin.srcDirs("jooq-src/main")
        resources.srcDirs("flyway")
    }
}

jooq {
    version.set(dependencyManagement.importedProperties["jooq.version"])
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "rsapi"
                        recordVersionFields = "version"
                        forcedTypes.apply {
                            add(
                                ForcedType().apply {
                                    name = "INSTANT"
                                    includeExpression = ".*"
                                    includeTypes = "TIMESTAMP.*"
                                },
                            )
                        }
                        includes = ".*"
                        excludes = "flyway_schema_history"
                        isIncludeExcludeColumns = true
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isKotlinNotNullInterfaceAttributes = true
                        isKotlinNotNullRecordAttributes = true
                        isKotlinNotNullPojoAttributes = true
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "org.railwaystations.rsapi.adapter.db.jooq"
                        directory = "jooq-src/main"
                    }

                    val camelCaseWithSuffix = MatcherRule()
                        .withExpression("$0_Table")
                        .withTransform(
                            MatcherTransformType.PASCAL
                        )

                    strategy = Strategy().withMatchers(
                        Matchers()
                            .withFields(
                                MatchersFieldType()
                                    .withFieldIdentifier(
                                        MatcherRule()
                                            .withTransform(MatcherTransformType.CAMEL)
                                            .withExpression("$0")
                                    ),
                            )
                            .withTables(
                                MatchersTableType()
                                    .withTableIdentifier(
                                        camelCaseWithSuffix,
                                    )
                                    .withTableClass(
                                        camelCaseWithSuffix,
                                    ),
                            ),
                    )
                }
            }
        }
    }
}

plugins {
    alias(libs.plugins.jooq.plugin)
}

dependencies {
    jooqGenerator(libs.postgresql)
    implementation(libs.jooq.codegen)
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    jooqGenerator("org.slf4j:slf4j-simple:2.0.17")
}

tasks.named<JooqGenerate>("generateJooq") {
    val migrationFilesLocation = "flyway/db/migration"
    inputs.files(fileTree(migrationFilesLocation))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.ABSOLUTE)

    allInputsDeclared.set(true)
    outputs.cacheIf {
        true
    }

    var container: JdbcDatabaseContainer<*>? = null

    val absoluteMigrationsPath =
        inputs.files.first { it.path.contains("flyway/db/migration") }.parentFile.parent

    doFirst {
        val newContainer = startContainer("postgres:17")
        flywayMigrate(newContainer, absoluteMigrationsPath)
        modifyJooqConfiguration(this as JooqGenerate, newContainer)
        container = newContainer
    }

    doLast {
        container?.stop()
    }
}

fun startContainer(imageName: String): JdbcDatabaseContainer<*> {
    val container = PostgreSQLContainer<Nothing>(DockerImageName.parse(imageName))
    container.start()
    return container
}

fun flywayMigrate(container: JdbcDatabaseContainer<*>, migrationsLocation: String) {
    val withPrefix = "filesystem:$migrationsLocation"

    val configuration: FluentConfiguration = Flyway.configure()
        .dataSource(container.jdbcUrl, container.username, container.password)
        .defaultSchema("rsapi")
        .locations(withPrefix)
    val flyway: Flyway = configuration.load()
    flyway.migrate()
}

fun modifyJooqConfiguration(jooqGenerate: JooqGenerate, container: JdbcDatabaseContainer<*>) {
    val jooqConfigurationField = JooqGenerate::class.java.getDeclaredField("jooqConfiguration")
    jooqConfigurationField.isAccessible = true
    val jooqConfiguration = jooqConfigurationField[jooqGenerate] as org.jooq.meta.jaxb.Configuration
    jooqConfiguration.jdbc.apply {
        url = container.jdbcUrl
        user = container.username
        password = container.password
    }
}