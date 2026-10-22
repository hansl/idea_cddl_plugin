import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.intellij.platform.grammarkit")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

dependencies {
    testImplementation("junit:junit:4.13.2")

    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledModule("intellij.regexp")
        bundledModule("intellij.spellchecker")
        testFramework(TestFrameworkType.Platform)
        // The unified IntelliJ IDEA distribution fails to initialize in tests without its core module.
        testBundledPlugins("com.intellij.modules.ultimate")
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
    }

    pluginVerification {
        ides {
            // Verifies against the targeted IDE; use recommended() to check all compatible releases (large downloads).
            current()
        }
    }
}

tasks {
    generateLexer {
        sourceFile = layout.projectDirectory.file("src/main/grammar/Cddl.flex")
    }

    generateParser {
        sourceFile = layout.projectDirectory.file("src/main/grammar/Cddl.bnf")
    }
}

// Generated lexer and parser sources are produced into build/generated and compiled with the plugin.
sourceSets {
    main {
        java.srcDir(tasks.generateLexer)
        java.srcDir(tasks.generateParser)
    }
}
