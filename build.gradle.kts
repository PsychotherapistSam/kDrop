import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.BIN
}

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("gg.jte.gradle") version "3.1.9"
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
    id("com.github.ben-manes.versions") version "0.51.0"
    application
}

group = "de.sam"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Kotlin
    val kotlin = "1.9.23"
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin")

    // Javalin
    val javalin = "6.1.3"
    implementation("io.javalin:javalin:$javalin")
    implementation("io.javalin:javalin-rendering:$javalin")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.12")
    val tinylog = "2.7.0"
    implementation("org.tinylog:tinylog-api-kotlin:$tinylog")
    implementation("org.tinylog:tinylog-impl:$tinylog")

    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.1")
    val jdbi = "3.45.1"
    implementation("org.jdbi:jdbi3-core:$jdbi")
    implementation("org.jdbi:jdbi3-kotlin:$jdbi")
    implementation("org.jdbi:jdbi3-postgres:$jdbi")

    val flyway = "10.9.0"
    implementation("org.flywaydb:flyway-core:$flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flyway")

    // Jackson
    val jackson = "2.17.0"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jackson")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:$jackson")

    // JTE
    val jte = "3.1.9" // also update the plugin!!!
    implementation("gg.jte:jte:$jte")
    implementation("gg.jte:jte-kotlin:$jte")

    // Misc
    implementation("com.password4j:password4j:1.8.1")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.7.Final")
    implementation("commons-io:commons-io:2.15.1")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.12")
    implementation("dev.samstevens.totp:totp:1.7.1")
    implementation("org.jetbrains:markdown:0.6.1")
    implementation("io.insert-koin:koin-core:3.5.3")

    // TUS
    implementation("me.desair.tus:tus-java-server:1.0.0-3.0")

    // krate (Rate Limiter)
    implementation("io.github.lpicanco:krate-core:1.0.2")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Cacheing
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "18"
}

application {
    mainClass.set("de.sam.base.MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "de.sam.base.MainKt"
    }
    minimize {
        // Exclude Tinylog & slf4j; Tinylog uses reflection to load the logger implementation, they are not huge anyway
        exclude(dependency("org.slf4j:slf4j-simple"))
        exclude(dependency("org.tinylog:tinylog-impl"))
        exclude(dependency("org.tinylog:tinylog-api-kotlin"))

        // Exclude the postgres driver, hikari doesn't find it otherwise
        exclude(dependency("org.postgresql:postgresql"))

        // Exclude Javalin
        exclude(dependency("io.javalin:javalin"))
        exclude(dependency("io.javalin:javalin-rendering"))

        // Exclude jte-Kotlin
        exclude(dependency("gg.jte:jte-kotlin")) // Sadly, this is very large

        // exclude prettytime
        exclude(dependency("org.ocpsoft.prettytime:prettytime"))

        // exclude totp
        exclude(dependency("dev.samstevens.totp:totp"))

        // exclude koin
        exclude(dependency("io.insert-koin:koin-core"))

        // exclude flyway
        exclude(dependency("org.flywaydb:flyway-core"))
        exclude(dependency("org.flywaydb:flyway-database-postgresql"))
    }
    // https://github.com/flyway/flyway/issues/3757#issuecomment-1748825125
    mergeServiceFiles()
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    processResources {
        dependsOn(generateJte)
        dependsOn(generateGitProperties)
    }
}


// https://github.com/casid/jte/blob/7de70921c2832a119517cc135d387a03790645e8/DOCUMENTATION.md?plain=1#L684
jte {
    generate()
}
kotlin {
    jvmToolchain(18)
}