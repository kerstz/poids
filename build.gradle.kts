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

// Chaînes : aucune chaîne littérale dans l'UI et parité des clés FR/EN par module (jalon 8).
val checkStrings = tasks.register("checkStrings") {
    group = "verification"
    description = "Aucun texte en dur dans les composables ; values/ et values-fr/ ont les mêmes clés."
    val sources = fileTree(rootDir) {
        include("*/src/main/**/*.kt")
        exclude("Archive/**", "audit/**", "**/build/**")
    }
    val resources = fileTree(rootDir) {
        include("*/src/main/res/values*/strings.xml")
        exclude("Archive/**", "audit/**", "**/build/**")
    }
    inputs.files(sources, resources)
    val rootPath = rootDir
    doLast {
        val hardcoded = listOf(
            Regex("""\bText\(\s*"[^"$]"""),
            Regex("""\bText\(\s*text\s*=\s*"[^"$]"""),
            Regex("""contentDescription\s*=\s*"[^"$]"""),
            Regex("""\b(label|title|placeholder|supportingText)\s*=\s*\{\s*Text\(\s*"[^"$]"""),
        )
        val allowed = Regex("""//\s*i18n-ok:\s*\S+""")
        val violations = sources.files.sortedBy { it.path }.flatMap { file ->
            val rel = file.relativeTo(rootPath).invariantSeparatorsPath
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.trim()
                if (code.startsWith("//") || allowed.containsMatchIn(line)) return@mapIndexedNotNull null
                if (hardcoded.any { it.containsMatchIn(line) }) "$rel:${i + 1}: texte en dur — ${code.take(80)}" else null
            }
        }
        val keyRegex = Regex("""<(string|plurals|string-array)\s+name="([^"]+)"""")
        val parity = resources.files.groupBy { it.parentFile.parentFile.parentFile.parentFile.name }.flatMap { (module, files) ->
            val byLocale = files.associate { it.parentFile.name to keyRegex.findAll(it.readText()).map { m -> m.groupValues[2] }.toSet() }
            val en = byLocale["values"] ?: emptySet()
            val fr = byLocale["values-fr"] ?: emptySet()
            (en - fr).map { "$module: clé absente en FR : $it" } + (fr - en).map { "$module: clé absente en EN : $it" }
        }
        val all = violations + parity
        if (all.isNotEmpty()) {
            throw GradleException("Chaînes :\n" + all.joinToString("\n") { " - $it" })
        }
        logger.lifecycle("Chaînes OK (${resources.files.size} fichiers, parité FR/EN)")
    }
}

// Licences : toute bibliothèque du classpath release doit figurer dans app/src/main/assets/licenses.json.
val checkLicenses = tasks.register("checkLicenses") {
    group = "verification"
    description = "Chaque groupe Maven de releaseRuntimeClasspath est déclaré dans licenses.json (écran À propos)."
    notCompatibleWithConfigurationCache("résout releaseRuntimeClasspath à l'exécution")
    val json = layout.projectDirectory.file("app/src/main/assets/licenses.json")
    doLast {
        val declared = Regex(""""group"\s*:\s*"([^"]+)"""").findAll(json.asFile.readText()).map { it.groupValues[1] }.toList()
        val groups = project(":app").configurations.getByName("releaseRuntimeClasspath")
            .incoming.resolutionResult.allComponents
            .mapNotNull { (it.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier)?.group }
            .toSortedSet()
        val missing = groups.filter { g -> declared.none { d -> g == d || (d.endsWith(".") && g.startsWith(d)) } }
        if (missing.isNotEmpty()) throw GradleException("Licences non déclarées dans licenses.json : $missing")
        logger.lifecycle("Licences OK (${groups.size} groupes)")
    }
}

// En-tête SPDX dans tous les fichiers Kotlin du projet.
val checkLicenseHeaders = tasks.register("checkLicenseHeaders") {
    group = "verification"
    description = "Chaque fichier .kt/.kts source commence par SPDX-License-Identifier: GPL-3.0-or-later."
    val files = fileTree(rootDir) {
        include("**/src/**/*.kt", "build-logic/src/**/*.kts")
        exclude("Archive/**", "audit/**", "**/build/**")
    }
    inputs.files(files)
    doLast {
        val bad = files.files.filter { !it.useLines { l -> l.firstOrNull().orEmpty().startsWith("// SPDX-License-Identifier: GPL-3.0-or-later") } }
        if (bad.isNotEmpty()) throw GradleException("En-tête SPDX absent :\n" + bad.take(20).joinToString("\n") { " - $it" })
    }
}

tasks.register("checkArchitecture") {
    group = "verification"
    dependsOn(checkModuleGraph, checkNoDestructiveMigration, checkDesignTokens, checkStrings, checkLicenses, checkLicenseHeaders)
}
