import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.4.0" apply false
}

allprojects {
    group = "io.pleo"
    version = "1.0"

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}