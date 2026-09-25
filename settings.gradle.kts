pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		gradlePluginPortal()
	}
}

plugins {
	// Auto-provisions the JDK 25 toolchain the unobfuscated 26.x targets need.
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lanport-default"
