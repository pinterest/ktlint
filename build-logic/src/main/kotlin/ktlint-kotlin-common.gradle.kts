plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    // All modules, the CLI included, must have an explicit API
    explicitApi()
}
