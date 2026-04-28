plugins {
    java
}

group = "me.tsunoda"
version = "1.0.0"

val pluginVersion = version.toString()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.jar {
    archiveBaseName.set("SharedHP")
}

tasks.register<Copy>("packageRelease") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    from(layout.projectDirectory.files("README.md", "README_ja.md", "CHANGELOG.md", "LICENSE"))
    into(layout.buildDirectory.dir("release"))
}
