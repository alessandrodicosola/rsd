import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.41"
}

group = "it.poliba.adicosola1.tesi"
version = "1.0"


repositories {
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/exposed")
    mavenCentral()

}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(kotlin("test"))
    implementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.1")


    implementation("org.jetbrains.exposed:exposed:0.16.3")
    implementation("mysql:mysql-connector-java:5.1.46")
    implementation("com.beust:klaxon:5.0.1")
    implementation("org.slf4j:slf4j-nop:1.7.25")

}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    register("getGradleHome") {
        doLast {
            println("Gradle user home dir : ${gradle.gradleUserHomeDir!!.absolutePath}")
            println("Gradle home dir : ${gradle.gradleHomeDir!!.absolutePath}")

        }
    }
}
