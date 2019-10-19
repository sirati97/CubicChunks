import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.champeau.gradle.JMHPlugin
import net.minecraftforge.gradle.tasks.DeobfuscateJar
import net.minecraftforge.gradle.user.ReobfMappingType
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.internal.HasConvention
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin

// Gradle repositories and dependencies
buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://files.minecraftforge.net/maven")
        }
        maven {
            setUrl("http://repo.spongepowered.org/maven")
        }
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.spongepowered:mixingradle:0.5-SNAPSHOT")
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.4")
        classpath("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0")
        classpath("me.champeau.gradle:jmh-gradle-plugin:0.4.6")
    }
}

plugins {
    base
    java
    idea
    eclipse
    `maven-publish`
    maven
    signing
    id("io.github.opencubicchunks.gradle.mixingen")
}

apply {
    plugin<ShadowPlugin>()
    plugin<MixinGradlePlugin>()
    plugin<LicensePlugin>()
    plugin<JMHPlugin>()
}

// tasks
val build by tasks
val jar: Jar by tasks
val shadowJar: ShadowJar by tasks
val javadoc: Javadoc by tasks
val test: Test by tasks
val processResources: ProcessResources by tasks
val deobfMcSRG: DeobfuscateJar by tasks
val deobfMcMCP: DeobfuscateJar by tasks

defaultTasks = listOf("licenseFormat", "build")

val licenseYear: String by project
val projectName: String by project

val versionSuffix: String by project
val versionMinorFreeze: String by project
val release: String by project

val sourceSets = the<JavaPluginConvention>().sourceSets
val mainSourceSet = sourceSets["main"]!!

group = "io.github.opencubicchunks"
(mainSourceSet as ExtensionAware).extra["refMap"] = "cubicchunks.mixins.refmap.json"

sourceSets {
    create("optifine_dummy")
}

// configurations, needed for extendsFrom
val jmh by configurations
val forgeGradleMc by configurations
val forgeGradleMcDeps by configurations
val forgeGradleGradleStart by configurations
val compile by configurations
val testCompile by configurations

val embed by configurations.creating
val coreShadow by configurations.creating

jmh.extendsFrom(compile)
jmh.extendsFrom(forgeGradleMc)
jmh.extendsFrom(forgeGradleMcDeps)
testCompile.extendsFrom(forgeGradleGradleStart)
testCompile.extendsFrom(forgeGradleMcDeps)
compile.extendsFrom(embed)
compile.extendsFrom(coreShadow)

// this is needed because it.ozimov:java7-hamcrest-matchers:0.7.0 depends on guava 19, while MC needs guava 21
configurations.all { resolutionStrategy { force("com.google.guava:guava:21.0") } }

dependencies {
    embed("com.flowpowered:flow-noise:1.0.1-SNAPSHOT")
    // https://mvnrepository.com/artifact/com.typesafe/config
    embed("com.typesafe:config:1.2.0")

    compileOnly(sourceSets["optifine_dummy"].output)

    testCompile("junit:junit:4.11")
    testCompile("org.hamcrest:hamcrest-junit:2.0.0.0")
    testCompile("it.ozimov:java7-hamcrest-matchers:0.7.0")
    testCompile("org.mockito:mockito-core:2.1.0-RC.2")
    testCompile("org.spongepowered:launchwrappertestsuite:1.0-SNAPSHOT")

    coreShadow("org.spongepowered:mixin:0.7.10-SNAPSHOT") {
        isTransitive = false
    }

    embed("io.github.opencubicchunks:regionlib:0.61.0-SNAPSHOT")
}

base {
    archivesBaseName = "CubicChunks"
}

mixin {
    token("MC_FORGE", extractForgeMinorVersion())
}

mixinGen {
    filePattern = "cubicchunks.mixins.%s.json"
    defaultRefmap = "cubicchunks.mixins.refmap.json"
    defaultPackagePrefix = "io.github.opencubicchunks.cubicchunks.core.asm.mixin"
    defaultCompatibilityLevel = "JAVA_8"
    defaultMinVersion = "0.7.10"

    config("core") {
        required = true
        conformVisibility = true
        injectorsDefaultRequire = 1
    }
    config("fixes") {
        required = true
        conformVisibility = true
        injectorsDefaultRequire = 1
    }
    config("noncritical") {
        required = false
        conformVisibility = true
    }
    config("selectable") {
        required = true
        conformVisibility = true
        injectorsDefaultRequire = 1
        configurationPlugin = "io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinConfig"
    }
}

license {
    val ext = (this as HasConvention).convention.extraProperties
    ext["project"] = projectName
    ext["year"] = licenseYear
    exclude("**/*.info")
    exclude("**/package-info.java")
    exclude("**/*.json")
    exclude("**/*.xml")
    exclude("assets/*")
    exclude("io/github/opencubicchunks/cubicchunks/core/server/chunkio/async/forge/*") // Taken from forge
    exclude("net/optifine/**/*")
    header = file("HEADER.txt")
    ignoreFailures = false
    strictCheck = true
    mapping(mapOf("java" to "SLASHSTAR_STYLE"))
}

jmh {
    iterations = 10
    benchmarkMode = listOf("thrpt")
    batchSize = 16
    timeOnIteration = "1000ms"
    fork = 1
    threads = 1
    timeUnit = "ms"
    verbosity = "NORMAL"
    warmup = "1000ms"
    warmupBatchSize = 16
    warmupForks = 1
    warmupIterations = 10
    profilers = listOf("perfasm")
    jmhVersion = "1.17.1"
}

javadoc.apply {
    source = sourceSets["api"].allJava
    (options as StandardJavadocDocletOptions).tags = listOf("reason")
}
val apiJavadocJar by tasks.creating(Jar::class) {
    classifier = "javadoc"
    from(tasks["javadoc"])
}
val apiSourcesJar by tasks.creating(Jar::class) {
    classifier = "api-sources"
    from(sourceSets["api"].java.srcDirs)
}
val apiJar by tasks.creating(Jar::class) {
    classifier = "api"
    from(sourceSets["api"].output)
}
val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].java.srcDirs)
}
val devShadowJar by tasks.creating(ShadowJar::class) {
    configureShadowJar(this, "dev")
}
reobf {
    create("shadowJar").apply {
        mappingType = ReobfMappingType.SEARGE
    }
    create("apiJar").apply {
        mappingType = ReobfMappingType.SEARGE
    }
}
build.dependsOn("reobfShadowJar", "reobfApiJar", devShadowJar)

publishing {
    repositories {
        maven {
            val user = (project.properties["sonatypeUsername"] ?: System.getenv("sonatypeUsername")) as String?
            val pass = (project.properties["sonatypePassword"] ?: System.getenv("sonatypePassword")) as String?
            val local = user == null || pass == null
            if (local) {
                logger.warn("Username or password not set, publishing to local repository in build/mvnrepo/")
            }
            val localUrl = "$buildDir/mvnrepo"
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl =  "https://oss.sonatype.org/content/repositories/snapshots"

            setUrl(if (local) localUrl else if (release.toBoolean()) releasesRepoUrl else snapshotsRepoUrl)
            if (!local) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }
    }
    (publications) {
        "api"(MavenPublication::class) {
            version = ext["mavenProjectVersion"]!!.toString()
            artifactId = "cubicchunks-api"
            from(components["java"])
            artifacts.clear()
            artifact(apiSourcesJar) {
                classifier = "sources"
            }
            artifact(apiJar) {
                classifier = ""
            }
            artifact(apiJavadocJar) {
                classifier = "javadoc"
            }
            pom {
                name.set("Cubic Chunks API")
                description.set("API for the CubicChunks mod for Minecraft")
                packaging = "jar"
                url.set("https://github.com/OpenCubicChunks/CubicChunks")
                description.set("API for CubicChunks mod for Minecraft")
                scm {
                    connection.set("scm:git:git://github.com/OpenCubicChunks/CubicChunks.git")
                    developerConnection.set("scm:git:ssh://git@github.com:OpenCubicChunks/CubicChunks.git")
                    url.set("https://github.com/OpenCubicChunks/CubicChunks")
                }

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("http://www.tldrlegal.com/license/mit-license")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("Barteks2x")
                        name.set("Barteks2x")
                    }
                    // TODO: add more developers
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/OpenCubicChunks/CubicChunks/issues")
                }
            }
        }
        "mod"(MavenPublication::class) {
            version = ext["mavenProjectVersion"]!!.toString()
            artifactId = "cubicchunks"
            from(components["java"])
            artifacts.clear()
            artifact(sourcesJar) {
                classifier = "sources"
            }
            artifact(shadowJar) {
                classifier = ""
            }
            artifact(devShadowJar) {
                classifier = "dev"
            }
            pom {
                name.set(projectName)
                description.set("Unlimited world height mod for Minecraft")
                packaging = "jar"
                url.set("https://github.com/OpenCubicChunks/CubicChunks")
                scm {
                    connection.set("scm:git:git://github.com/OpenCubicChunks/CubicChunks.git")
                    developerConnection.set("scm:git:ssh://git@github.com:OpenCubicChunks/CubicChunks.git")
                    url.set("https://github.com/OpenCubicChunks/RegionLib")
                }

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("http://www.tldrlegal.com/license/mit-license")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("Barteks2x")
                        name.set("Barteks2x")
                    }
                    // TODO: add more developers
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/OpenCubicChunks/CubicChunks/issues")
                }
            }
        }
    }
}
afterEvaluate {
    tasks["publishApiPublicationToMavenRepository"].dependsOn("reobfApiJar")
    tasks["publishModPublicationToMavenRepository"].dependsOn("reobfShadowJar")
    tasks["publishModPublicationToMavenRepository"].dependsOn(devShadowJar)
}
// tasks must be before artifacts, don't change the order
artifacts {
    withGroovyBuilder {
        "archives"(apiJar, apiSourcesJar, apiJavadocJar, shadowJar, sourcesJar, devShadowJar)
    }
}

signing {
    isRequired = false
    // isRequired = gradle.taskGraph.hasTask("uploadArchives")
    sign(configurations.archives)
}

jar.apply {
    from(sourceSets["main"].output)
    from(sourceSets["api"].output)
    exclude("LICENSE.txt", "log4j2.xml")

    configureManifest(manifest)
}

fun configureManifest(manifest: Manifest) {
    manifest.attributes["FMLCorePlugin"] = "io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod"
    manifest.attributes["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
    manifest.attributes["TweakOrder"] = "0"
    manifest.attributes["ForceLoadAsMod"] = "true"
    manifest.attributes["FMLCorePluginContainsFMLMod"] = "true" // workaround for mixin double-loading the mod on new forge versions
}

fun configureShadowJar(task: ShadowJar, classifier: String) {
    task.configurations = listOf(coreShadow)
    task.exclude("META-INF/MUMFREY*")
    task.from(sourceSets["main"].output)
    task.from(sourceSets["api"].output)
    task.exclude("log4j2.xml")
    task.into("/") {
        from(embed)
    }

    task.classifier = classifier

    configureManifest(task.manifest)
}

shadowJar.apply { configureShadowJar(this, "all") }

test.apply {
    systemProperty("lwts.tweaker", "cubicchunks.tweaker.MixinTweakerServer")
    jvmArgs("-Dmixin.debug.verbose=true", //verbose mixin output for easier debugging of mixins
            "-Dmixin.checks.interfaces=true", //check if all interface methods are overriden in mixin
            "-Dmixin.env.remapRefMap=true")
    testLogging {
        showStandardStreams = true
    }
}

processResources.apply {
    // this will ensure that this task is redone when the versions change.
    inputs.property("version", project.version.toString())
    inputs.property("mcversion", minecraft.version)

    // replace stuff in mcmod.info, nothing else
    from(mainSourceSet.resources.srcDirs) {
        include("mcmod.info")

        // replace version and mcversion
        expand(mapOf("version" to project.version.toString(), "mcversion" to minecraft.version))
    }

    // copy everything else, thats not the mcmod.info
    from(mainSourceSet.resources.srcDirs) {
        exclude("mcmod.info")
    }
}

fun extractForgeMinorVersion(): String {
    // version format: MAJOR.MINOR.?.BUILD
    return minecraft.forgeVersion.split(Regex("\\.")).getOrNull(1) ?:
    throw RuntimeException("Invalid forge version format: ${minecraft.forgeVersion}")
}