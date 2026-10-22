import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.io.File

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

    // Signing and publishing read their secrets from environment variables; signing is skipped when they are not set.
    // Locally, point *_FILE at the files; in CI, put the PEM contents in CERTIFICATE_CHAIN and PRIVATE_KEY.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        certificateChainFile.fileProvider(providers.environmentVariable("CERTIFICATE_CHAIN_FILE").map(::File))
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        privateKeyFile.fileProvider(providers.environmentVariable("PRIVATE_KEY_FILE").map(::File))
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
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

    // verifyPluginSignature checks the archive produced by signPlugin but does not declare that dependency itself.
    verifyPluginSignature {
        dependsOn(signPlugin)
    }
}

// Generated lexer and parser sources are produced into build/generated and compiled with the plugin.
sourceSets {
    main {
        java.srcDir(tasks.generateLexer)
        java.srcDir(tasks.generateParser)
    }
}
