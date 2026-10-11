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

// Tokens de design : aucune couleur/dimension/forme/animation en dur dans les features (docs/UI.md §8).
// Les écrans pas encore migrés sont listés dans config/design-tokens-baseline.txt (à vider au fil des jalons).
val checkDesignTokens = tasks.register("checkDesignTokens") {
    group = "verification"
    description = "Interdit couleurs, dimensions, formes et animations littérales dans les modules feature et :app."
    val sources = fileTree(rootDir) {
        include("nutrition/src/main/**/*.kt", "body/src/main/**/*.kt", "activity/src/main/**/*.kt", "app/src/main/**/*.kt")
        exclude("**/build/**")
    }
    val baselineFile = layout.projectDirectory.file("config/design-tokens-baseline.txt")
    inputs.files(sources)
    inputs.file(baselineFile)
    val rootPath = rootDir
    doLast {
        val rules = listOf(
            "couleur littérale" to Regex("""Color\(\s*0x|\bColor\.(Black|White|Red|Green|Blue|Gray|DarkGray|LightGray|Yellow|Cyan|Magenta)\b"""),
            "dimension littérale" to Regex("""\b(?!0\.dp\b)\d+(\.\d+)?\.(dp|sp)\b"""),
            "forme littérale" to Regex("""\b(RoundedCornerShape|CircleShape|CutCornerShape)\b"""),
            "style ad hoc" to Regex("""\b(fontSize|fontWeight|letterSpacing)\s*="""),
            "animation directe" to Regex("""\b(tween|spring)\s*\("""),
            "icônes Material" to Regex("""androidx\.compose\.material\.icons"""),
        )
        val allowed = Regex("""//\s*design-ok:\s*\S+""")
        val baseline = baselineFile.asFile.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
        val violations = sources.files.sortedBy { it.path }.flatMap { file ->
            val rel = file.relativeTo(rootPath).invariantSeparatorsPath
            if (rel in baseline) return@flatMap emptyList<String>()
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.trim()
                if (code.startsWith("import ") && !code.contains("material.icons")) return@mapIndexedNotNull null
                if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*") || allowed.containsMatchIn(line)) return@mapIndexedNotNull null
                rules.firstOrNull { (_, re) -> re.containsMatchIn(line) }?.let { (name, _) -> "$rel:${i + 1}: $name — ${code.take(80)}" }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Tokens de design violés (utiliser TallyTheme.*, ou // design-ok: raison):\n" + violations.joinToString("\n") { " - $it" })
        }
        logger.lifecycle("Tokens de design OK (${baseline.size} fichier(s) en baseline)")
    }
}

tasks.register("checkArchitecture") {
    group = "verification"
    dependsOn(checkModuleGraph, checkNoDestructiveMigration, checkDesignTokens)
}
