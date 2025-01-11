plugins {
    kotlin("jvm") version "2.0.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

tasks.register("copyToLivePlugins") {
    outputs.upToDateWhen { false }
    doLast {
        val srcDir = file(path = "live-plugins")
        val baseDir = file(path = "C:\\Users\\Rakete\\AppData\\Roaming\\JetBrains")

        baseDir.listFiles()?.forEach { dir ->
            val livePluginsDir = File(dir, "live-plugins")
            if (livePluginsDir.exists() && livePluginsDir.isDirectory) {
                copy {
                    from(srcDir)
                    into(livePluginsDir)
                }
            }
        }
    }
    println("Copied to live-plugins")
}

tasks.register("copyExternalTools") {
    outputs.upToDateWhen { false }
    doLast {
        val srcDir = file(path = "tools")
        val baseDir = file(path = "C:\\Users\\Rakete\\AppData\\Roaming\\JetBrains")

        baseDir.listFiles()?.forEach { dir ->
            val toolsDir = File(dir, "tools")
            if (toolsDir.exists() && toolsDir.isDirectory) {
                copy {
                    from(srcDir)
                    into(toolsDir)
                }
            }
        }
    }
}


tasks.assemble {
    dependsOn("copyToLivePlugins")
}
