import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "2.0.0"
}

//Constants:

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")
val elementaVersion = 710
val ucVersion = 415

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
        runConfigs {
            "client" {
                if (SystemUtils.IS_OS_MAC_OSX) {
                    // This argument causes a crash on macOS
                    vmArgs.remove("-XstartOnFirstThread")
                }
            }
            remove(getByName("server"))
        }
        forge {
            pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
            mixinConfig("mixins.$modid.json")
            if (transformerFile.exists()) {
                println("Installing access transformer")
                accessTransformer(transformerFile)
            }
        }
        mixin {
            defaultRefmapName.set("mixins.$modid.refmap.json")
        }
    }

    tasks.compileJava {
        dependsOn(tasks.processResources)
    }

    sourceSets.main {
        output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
        java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
        kotlin.destinationDirectory.set(java.destinationDirectory)
    }

// Dependencies:

    repositories {
        mavenCentral()
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.essential.gg/repository/maven-public")
    }

    val shadowImpl: Configuration by configurations.creating {
        configurations.implementation.get().extendsFrom(this)
    }

    dependencies {
        minecraft("com.mojang:minecraft:1.8.9")
        mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
        forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

        shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
            isTransitive = false
        }
        annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

        shadowImpl("org.reflections:reflections:0.10.2")
        shadowImpl("gg.essential:elementa:$elementaVersion")
        shadowImpl("gg.essential:universalcraft-1.8.9-forge:$ucVersion")
        shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
        shadowImpl("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    }

// Tasks:

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    tasks.withType(org.gradle.jvm.tasks.Jar::class) {
        archiveBaseName.set("zen-1.8.9-forge")
        manifest.attributes.run {
            this["FMLCorePluginContainsFMLMod"] = "true"
            this["ForceLoadAsMod"] = "true"
            this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
            this["MixinConfigs"] = "mixins.$modid.json"
            if (transformerFile.exists())
                this["FMLAT"] = "${modid}_at.cfg"
        }

        tasks.processResources {
            inputs.property("version", project.version)
            inputs.property("mcversion", mcVersion)
            inputs.property("modid", modid)
            inputs.property("basePackage", baseGroup)

            filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
                expand(inputs.properties)
            }

            rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
        }

        val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
            archiveClassifier.set("")
            from(tasks.shadowJar)
            input.set(tasks.shadowJar.get().archiveFile)
        }

        tasks.jar {
            archiveClassifier.set("without-deps")
            destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
        }

        tasks.shadowJar {
            destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
            archiveClassifier.set("non-obfuscated-with-deps")
            configurations = listOf(shadowImpl)
            minimize()
            exclude("kotlin/**")
            exclude("META-INF/kotlin*")
            fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
            relocate("gg.essential.elementa")
            relocate("gg.essential.universal")
            doLast {
                configurations.forEach {
                    println("Copying dependencies into mod: ${it.files}")
                }
            }
        }

        tasks.assemble.get().dependsOn(tasks.remapJar)
    }
}
