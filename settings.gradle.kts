plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "LivePlugins"

gradle.addListener(object : BuildListener {
    override fun settingsEvaluated(settings: Settings) {
        println("Settings evaluated")
    }
    override fun projectsLoaded(gradle: Gradle) {
        println("Projects loaded")
    }
    override fun projectsEvaluated(gradle: Gradle) {
        println("Projects evaluated")
    }
    override fun buildFinished(result: BuildResult) {
        println("Build finished")
    }
})

