plugins {
	// Minecraft 26.1 onwards ships unobfuscated, so this uses the non-remapping Loom plugin
	// (net.fabricmc.fabric-loom, NOT -remap): no mappings(), plain implementation(), and the
	// plain jar task is the final artifact (nothing to remap).
	id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
}

group = "net.lanportdefault"
version = project.property("mod_version") as String

base {
	archivesName = "lanport-default-fabric-${project.property("minecraft_version")}"
}

// Unobfuscated Minecraft (26.1+) requires Java 25.
java {
	toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
	// Mixin annotations are needed at compile time (Loom's bundled Mixin applies them at runtime).
	exclusiveContent {
		forRepository {
			maven {
				name = "Sponge"
				url = uri("https://repo.spongepowered.org/repository/maven-public")
			}
		}
		filter { includeGroupAndSubgroups("org.spongepowered") }
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
	implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
	compileOnly("org.spongepowered:mixin:0.8.5")
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

tasks.processResources {
	val expandProps = mapOf("version" to version)
	inputs.properties(expandProps)
	filesMatching("fabric.mod.json") {
		expand(expandProps)
	}
}
