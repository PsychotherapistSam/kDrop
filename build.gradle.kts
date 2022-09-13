import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
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
    implementation("io.javalin:javalin:4.6.4")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:2.0.0")
    // https://mvnrepository.com/artifact/gg.jte/jte
    implementation("gg.jte:jte:2.1.2")
    implementation("gg.jte:jte-kotlin:2.1.2")
    // https://mvnrepository.com/artifact/org.jetbrains.exposed/exposed
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    // https://mvnrepository.com/artifact/me.liuwj.ktorm/ktorm-core
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.0")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.4")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-joda
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.13.4")

    // https://mvnrepository.com/artifact/com.password4j/password4j
    implementation("com.password4j:password4j:1.6.0")

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
    implementation("com.stripe:stripe-java:21.7.0")

    // https://mvnrepository.com/artifact/dev.samstevens.totp/totp
    implementation("dev.samstevens.totp:totp:1.7.1")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("de.sam.base.MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "de.sam.base.MainKt"
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