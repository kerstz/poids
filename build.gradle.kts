// Les plugins sont appliqués par les convention plugins de build-logic.
// Racine : tâches de contrôle d'architecture (voir docs/AUDIT.md §3.1, §3.6).

val featureModules = setOf(":nutrition", ":body", ":activity")
val pureModules = setOf(":core", ":domain")

val checkModuleGraph = tasks.register("checkModuleGraph") {
    group = "verification"
    description = "Vérifie les règles de dépendance entre modules (feature ↛ feature, feature ↛ :data, :core/:domain sans Android)."
    notCompatibleWithConfigurationCache("inspecte le modèle de projets à l'exécution")
    val edges: Provider<List<String>> = provider {
        subprojects.flatMap { sub ->
            sub.configurations
                .filter { it.name.endsWith("Implementation") || it.name.endsWith("Api") || it.name == "implementation" || it.name == "api" }
                .flatMap { conf ->
                    conf.dependencies.withType<ProjectDependency>().map { dep -> "${sub.path}->${dep.path}" }
                }
        }.distinct()
    }
    doLast {
        val violations = edges.get().filter { edge ->
            val (from, to) = edge.split("->")
            when {
                from in featureModules && to in featureModules -> true
                from in featureModules && to == ":data" -> true
                from in pureModules && to != ":core" -> true
                from == ":core" -> true
                from == ":domain" && to == ":data" -> true
                else -> false
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Règles de modules violées:\n" + violations.joinToString("\n") { " - $it" })
        }
        logger.lifecycle("Graphe de modules OK (${edges.get().size} arêtes)")
    }
}

val checkNoDestructiveMigration = tasks.register("checkNoDestructiveMigration") {
    group = "verification"
    description = "Interdit fallbackToDestructiveMigration (voir docs/AUDIT.md §4.5)."
    val sources = fileTree(rootDir) {
        include("**/src/**/*.kt")
        exclude("Archive/**", "audit/**", "**/build/**")
    }
    doLast {
        val hits = sources.files.filter { it.readText().contains("fallbackToDestructiveMigration") }
        if (hits.isNotEmpty()) {
            throw GradleException("fallbackToDestructiveMigration interdit:\n" + hits.joinToString("\n") { " - $it" })
        }
    }
}

tasks.register("checkArchitecture") {
    group = "verification"
    dependsOn(checkModuleGraph, checkNoDestructiveMigration)
}
