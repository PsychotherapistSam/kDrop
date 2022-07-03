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
    implementation("io.javalin:javalin:4.6.1")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:1.7.36")
    // https://mvnrepository.com/artifact/gg.jte/jte
    implementation("gg.jte:jte:2.0.3")
    implementation("gg.jte:jte-kotlin:2.0.3")
    // https://mvnrepository.com/artifact/org.jetbrains.exposed/exposed
    implementation("org.jetbrains.exposed:exposed:0.17.14")
    // https://mvnrepository.com/artifact/me.liuwj.ktorm/ktorm-core
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.6")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")

    // https://mvnrepository.com/artifact/com.password4j/password4j
    implementation("com.password4j:password4j:1.5.4")

    // https://mvnrepository.com/artifact/org.ocpsoft.prettytime/prettytime
    implementation("org.ocpsoft.prettytime:prettytime:5.0.3.Final")

    // https://mvnrepository.com/artifact/io.micrometer/micrometer-core
    implementation("io.micrometer:micrometer-core:1.9.1")
// https://mvnrepository.com/artifact/io.micrometer/micrometer-registry-new-relic
    implementation("io.micrometer:micrometer-registry-new-relic:1.9.1")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.11.0")



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