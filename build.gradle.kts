import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("gg.jte.gradle") version ("2.0.2")
    application
}

group = "me.sam"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    testImplementation(kotlin("test"))
    // https://mvnrepository.com/artifact/io.javalin/javalin
    implementation("io.javalin:javalin:5.3.1")
    implementation("io.javalin:javalin-rendering:5.3.1")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:2.0.5")
    // https://mvnrepository.com/artifact/gg.jte/jte
    implementation("gg.jte:jte:2.2.4")
    implementation("gg.jte:jte-kotlin:2.2.4")
    // https://mvnrepository.com/artifact/org.jetbrains.exposed/exposed
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    // https://mvnrepository.com/artifact/me.liuwj.ktorm/ktorm-core
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.1")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-joda
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.14.0")

    // https://mvnrepository.com/artifact/com.password4j/password4j
    implementation("com.password4j:password4j:1.6.3")

    // https://mvnrepository.com/artifact/org.ocpsoft.prettytime/prettytime
    implementation("org.ocpsoft.prettytime:prettytime:5.0.4.Final")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.11.0")


    val tinylog = "2.5.0"
    // Tinylog
    // https://mvnrepository.com/artifact/org.tinylog/tinylog-api-kotlin
    implementation("org.tinylog:tinylog-api-kotlin:$tinylog")
    // https://mvnrepository.com/artifact/org.tinylog/tinylog-impl
    implementation("org.tinylog:tinylog-impl:$tinylog")

    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:31.1-jre")

    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // https://mvnrepository.com/artifact/com.stripe/stripe-java
    implementation("com.stripe:stripe-java:22.5.0")

    // https://mvnrepository.com/artifact/dev.samstevens.totp/totp
    implementation("dev.samstevens.totp:totp:1.7.1")

    // https://mvnrepository.com/artifact/io.konform/konform
    implementation("io.konform:konform:0.4.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "13"
}

java {
    sourceCompatibility = JavaVersion.VERSION_13
    targetCompatibility = JavaVersion.VERSION_13
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

        // Exclude Jetbrains Exposed
        exclude(dependency("org.jetbrains.exposed:exposed"))

        // Exclude Javalin
        exclude(dependency("io.javalin:javalin"))
        exclude(dependency("io.javalin:javalin-rendering"))

        // Exclude jte-Kotlin
        exclude(dependency("gg.jte:jte-kotlin")) // Sadly, this is very large

        // exclude prettytime
        exclude(dependency("org.ocpsoft.prettytime:prettytime"))

        // exclude totp
        exclude(dependency("dev.samstevens.totp:totp"))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

// https://github.com/casid/jte/blob/7de70921c2832a119517cc135d387a03790645e8/DOCUMENTATION.md?plain=1#L684
jte {
    generate()
}