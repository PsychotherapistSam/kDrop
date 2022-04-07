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
    implementation("io.javalin:javalin:4.4.0")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:1.7.36")
    // https://mvnrepository.com/artifact/gg.jte/jte
    implementation("gg.jte:jte:2.0.2")
    implementation("gg.jte:jte-kotlin:2.0.2")
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