plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    // All modules, the CLI included, must have an explicit API
    explicitApi()
}

addAdditionalJdkVersionTests()
