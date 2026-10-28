# UsagiKit 🐇

A lightweight, standalone Android library and manga bridge SDK to dynamically load and run **Usagi extension plugins** (`.jar` containing Dex sources) directly inside any Android application without requiring the Usagi app to be installed.

---

## 🚀 Features

- 🔌 **Dynamic Plugin Loading**: Loads compiled `.jar` plugin extensions (e.g. UMA or custom Usagi extensions) at runtime using `UsagiPluginClassLoader`.
- 🌉 **Uniform Manga Bridge (`UsagiMangaBridge`)**:
  - Popular & Latest manga pagination (`getPopularManga`, `getLatestManga`)
  - Filter & Keyword search (`searchManga`)
  - Full manga details & chapter listing (`getDetails`)
  - Chapter pages & image resolution (`getPages`, `getPageUrl`, `getPageRequest`, `getPageResponse`)
  - Custom HTTP header handling (`getHeaders`)
- 📦 **Independent & Decoupled**: Standalone library ready for inclusion via JitPack or Maven Local.
- 🧪 **Companion Tester App**: Contains an integrated `:tester-app` module demonstrating complete end-to-end functionality.

---

## 📥 Installation (No Authentication Required)

### 1. Add Maven Repository
In your project's `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/verizonkert/usagi-kit/gh-pages/maven")
        }
    }
}
```

### 2. Add Dependency
In your app module's `build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.draken.usagi:usagi-kit:1.0.0")
}
```

*(You can replace `1.0.0` with whichever release tag you wish to use).*

---

### Alternative: Direct `.aar` Download
You can also download the standalone `.aar` binary directly from the **[Releases](https://github.com/verizonkert/usagi-kit/releases)** page, put it into your project's `app/libs/` directory, and add:
```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
}
```

> **Note:** If you are testing locally from this workspace, you can publish to maven local via `./gradlew :usagi-kit:publishToMavenLocal` and use `implementation("org.draken.usagi:usagi-kit:1.0.0")`.

---

## ⚡ Quick Start

```kotlin
// 1. Initialize UsagiKit in Application.onCreate()
UsagiKit.initialize(applicationContext)

// 2. Load an extension plugin (.jar file)
val pluginJar = File(filesDir, "plugins/source.jar")
val extension = UsagiKit.loadExtension(pluginJar)

// 3. Select a source and fetch manga
val source = extension.sources.first()
val popularManga = UsagiKit.getPopularManga(source.name, page = 1)
```

For complete integration instructions, advanced configuration, and sample code, see **[USAGE.md](USAGE.md)**.

---

## 🛠️ Building & Contributing

### Build Library
```bash
./gradlew :usagi-kit:assembleRelease
```

### Run Tests
```bash
./gradlew testDebugUnitTest
```

### Publish to Maven Local
```bash
./gradlew :usagi-kit:publishToMavenLocal
```

---

## 📄 License

This project is open-source. Please check individual licenses of dependencies (`Tsuki`, `OkHttp`, etc.) when distributing.
