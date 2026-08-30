plugins {
    java
    id("com.gradleup.shadow") version "8.3.11"
}

group = "com.leidcool"
version = "0.1.0-SNAPSHOT"
description = "LINC-Chat for Paper 1.20.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.12.3")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
}

val relocateBase = "com.leidcool.lincchat.libs"

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("LINC-Chat-1.20.1")

    relocate("org.spongepowered.configurate", "$relocateBase.configurate")
    relocate("io.leangen.geantyref", "$relocateBase.geantyref")
    relocate("org.yaml.snakeyaml", "$relocateBase.snakeyaml")

    exclude("META-INF/maven/**")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
