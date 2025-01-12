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
        val baseDir = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            file(path = "C:\\Users\\${System.getProperty("user.name")}\\AppData\\Roaming\\JetBrains")
        } else {
            file(path = "/home/${System.getProperty("user.name")}/.config/JetBrains")
        }

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
        val baseDir = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            file(path = "C:\\Users\\${System.getProperty("user.name")}\\AppData\\Roaming\\JetBrains")
        } else {
            file(path = "/home/${System.getProperty("user.name")}/.config/JetBrains")
        }

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
    println("Copied to tools")
}


tasks.assemble {
    dependsOn("copyToLivePlugins")
}
