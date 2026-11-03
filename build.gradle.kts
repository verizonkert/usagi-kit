plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.github.verizonkert"
version = System.getenv("LIB_VERSION") ?: "0.0.1"

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    exclude("android/**")
    exclude("androidx/annotation/**")
    exclude("androidx/preference/**")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=tsuki.InternalParsersApi",
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
publishing {
	publications {
		create<MavenPublication>("maven") {
			artifactId = "usagi-kit"
			from(components["java"])
		}
	}
	repositories {
		maven {
			name = "LocalRepo"
			url = uri(layout.buildDirectory.dir("repo"))
		}
	}
}

dependencies {
	implementation(libs.androidx.collection)
	api(libs.jsoup)
	implementation(libs.json)
	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.okhttp)
	implementation(libs.okio)

	testImplementation(libs.junit.api)
	testImplementation(libs.junit.engine)
	testImplementation(libs.junit.launcher)
	testImplementation(libs.junit.params)
	testImplementation(libs.kotlinx.coroutines.test)
}
