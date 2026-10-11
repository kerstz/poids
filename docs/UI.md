# Tally — Système UI

Statut : implémenté dans `:core-ui` ; seul l'écran Journal est migré (pilote, jalon UI). Autres écrans listés dans `config/design-tokens-baseline.txt`. Remplace `core-ui/.../theme/Color.kt` (verts M3 en dur) et `Theme.kt` (dynamic color activé par défaut).
Fichiers consultés : `core-ui/src/main/kotlin/org/tally/coreui/theme/{Color,Theme}.kt`, `build.gradle.kts` (racine), `gradle/libs.versions.toml`, `nutrition/.../journal/JournalScreen.kt`.

## 1. Couleurs

Ratios WCAG 2.x calculés par script (luminance relative sRGB) le 2026-10-11. Seuils : texte ≥ 4,5, UI/graphique ≥ 3.

| Token | OLED | Clair | Ratio OLED (vs bg / vs s3) | Ratio clair (vs bg / vs s3) |
|---|---|---|---|---|
| `background` | `#000000` | `#F7F7F8` | — | — |
| `surface1` (cartes) | `#0F0F10` | `#FFFFFF` | — | — |
| `surface2` (sheets, champs) | `#18181A` | `#EFEFF1` | — | — |
| `surface3` (pressé, piste de jauge) | `#222225` | `#E6E6E9` | — | — |
| `divider` (séparateur décoratif) | `#2A2A2E` | `#D9D9DD` | 1,47 / 1,11 (décoratif, exempté) | 1,31 / 1,13 (exempté) |
| `outline` (bord champ, contrôle) | `#6B6B72` | `#7A7A82` | 3,97 / 3,00 | 3,98 / 3,42 |
| `onSurface` (texte principal) | `#E6E6E8` | `#1A1A1C` | 16,85 / 12,73 | 16,23 / 13,95 |
| `onSurfaceVariant` (secondaire) | `#9E9EA6` | `#56565D` | 7,90 / 5,97 | 6,80 / 5,84 |
| `accent` | `#5BC48E` | `#1D7349` | 9,73 / 7,35 | 5,45 / 4,68 |
| `onAccent` | `#00180C` | `#FFFFFF` | 8,57 sur accent | 5,83 sur accent |
| `error` | `#FF7A70` | `#B3261E` | 8,27 / 6,25 | 6,11 / 5,25 |
| `onError` | `#330A06` | `#FFFFFF` | 6,97 sur error | 6,54 sur error |
| `protein` | `#7FA3D6` | `#3D6496` | 8,11 / 6,13 | 5,67 / 4,87 |
| `carbs` | `#CDB074` | `#7E6219` | 10,06 / 7,60 | 5,38 / 4,62 |
| `fat` | `#C88FA3` | `#8F4D66` | 7,91 / 5,98 | 5,73 / 4,93 |

Règles :
- Pas d'ombre, pas de tonal elevation : `surfaceTint = Color.Transparent`, `tonalElevation = 0.dp`, élévation exprimée par palier `surface1→3`.
- Mapping M3 (pour que les composants M3 restent cohérents) : `background/surface/surfaceContainerLowest = background`, `surfaceContainerLow/surfaceContainer = surface1`, `surfaceContainerHigh = surface2`, `surfaceContainerHighest = surface3`, `outline = outline`, `outlineVariant = divider`, `primary = accent`, `onPrimary = onAccent`, `secondary/tertiary = accent` (un seul accent), `error/onError`.
- Macros hors M3 : `TallyColors(protein, carbs, fat, divider)` via `LocalTallyColors`. Accent et macros se distinguent par la teinte, pas la luminance (accent/glucides 1,03:1) → toujours un libellé texte ou une initiale à côté (jamais la couleur seule).
- Thème : réglage `OLED | Clair | Système`, défaut **OLED**. Dynamic color : option réglages, défaut **off** ; si on, elle remplace uniquement `accent/onAccent` (fonds OLED conservés) ; contraste non garanti → affiché comme tel dans les réglages.
- Thème XML (`:app`) : passer de `android:Theme.Material.Light.NoActionBar` à un thème `NoActionBar` sombre avec `windowBackground = #000000` (évite le flash blanc au démarrage).

## 2. Typographie

| Décision | Justification |
|---|---|
| Famille unique `FontFamily.Default` (Roboto / Roboto Flex selon OS) | 0 Ko d'APK, rendu natif, mise à l'échelle non linéaire Android 14+ gratuite. Police embarquée (Inter, OFL) rejetée : +300 Ko, pas de gain fonctionnel. |
| `fontFeatureSettings = "tnum"` sur tous les styles `number*` | Roboto a des chiffres tabulaires par défaut ; `tnum` protège contre les polices OEM (Samsung, etc.). Hypothèse : une police OEM sans `tnum` l'ignore silencieusement (pas de crash). |

| Style | Taille / interligne (sp) | Graisse | Rôle |
|---|---|---|---|
| `numberHero` | 48 / 56 | Medium | Chiffre clé (kcal restantes, poids du jour) — tnum |
| `numberLarge` | 28 / 36 | Medium | Totaux carte — tnum |
| `numberBody` | 16 / 24 | Regular | Valeurs en ligne (g, kcal) — tnum |
| `titleLarge` | 22 / 28 | Medium | Titre d'écran |
| `titleMedium` | 16 / 24 | SemiBold | En-tête de section |
| `bodyLarge` | 16 / 24 | Regular | Nom d'aliment |
| `bodyMedium` | 14 / 20 | Regular | Texte secondaire |
| `labelLarge` | 14 / 20 | Medium | Boutons |
| `labelMedium` | 12 / 16 | Medium, +0,4 sp | Labels discrets (unités, « Restant ») |

Les styles M3 standards sont redéfinis avec ces valeurs ; les `number*` vivent dans `TallyTypography` (`LocalTallyTypography`). À 200 % : aucune hauteur fixe sur du texte, `Row` → retour à la ligne (`FlowRow`) pour libellé + valeur.

## 3. Dimensions

| Token | Valeur | Usage |
|---|---|---|
| `space.xs / s / m / l / xl / xxl` | 4 / 8 / 12 / 16 / 24 / 32 dp | Grille 4 dp exclusive |
| `gutter` | 16 dp | Marge écran horizontale |
| `radius.s / m / l / full` | 8 / 12 / 16 dp / 50 % | champs+chips / cartes / haut de bottom sheet / pastilles |
| `icon.s / m / l` | 20 / 24 / 48 dp | Inline / standard / état vide |
| `touchTarget` | 48 dp min | `Modifier.minimumInteractiveComponentSize()` |
| `divider` | 1 dp | `HorizontalDivider(color = divider)` |
| `macroBarHeight` / `gaugeHeight` | 8 dp | Barre de macros / jauge |
| `rowMinHeight` | 56 dp | Ligne d'aliment |
| `fabClearance` | 88 dp | Réserve de liste sous le FAB |

Exposés par `TallyTheme.spacing`, `.shapes` (M3 `Shapes` small=8, medium=12, large=16), `.dimens`.

## 4. Motion

| Token | Durée | Easing | Usage |
|---|---|---|---|
| `short` | 150 ms | Standard `CubicBezier(0.2,0,0,1)` | Fondu, changement d'état, couleur |
| `medium` | 200 ms | Emphasized decelerate `(0.05,0.7,0.1,1)` (entrée) / accelerate `(0.3,0,0.8,0.15)` (sortie) | Expand, contenu de sheet, swipe retour |
| `long` | 250 ms | Emphasized decelerate | Transitions de navigation |

Reduce-motion : lire `Settings.Global.ANIMATOR_DURATION_SCALE` (0 = « Supprimer les animations ») dans `MainActivity` + `ContentObserver`, fourni par `LocalReduceMotion: Boolean`. Helper unique `TallyMotion.spec<T>(token)` → `snap()` si réduit, sinon `tween(...)`. Interdit d'appeler `tween()` directement dans les features. Incertitude : Compose applique peut-être déjà l'échelle système via `MotionDurationScale` ; le helper garantit le résultat dans tous les cas (y compris graphes Vico : désactiver `animateIn`/animation si réduit).

## 5. Interaction

| Écran / pattern | Décision |
|---|---|
| Navigation | `NavigationBar` **3 onglets** : Aujourd'hui, Corps, Activité. Réglages = icône engrenage dans la `TopAppBar` des écrans racine (place libre pour « Entraînement » en 4e onglet). Impacte `TopLevelDestination.SETTINGS`. |
| Aujourd'hui | Action unique : FAB « Ajouter » → recherche aliment (plein écran : champ + résultats + scan) → `QuantitySheet`. Objectif calorique : `ModalBottomSheet` (remplace `TargetDialog`/`AlertDialog`). |
| Corps | FAB « Peser » → sheet avec `NumberField` (kg/lb selon réglage). |
| Activité | `EmptyState` avec action tant que placeholder. |
| Réglages | Aucune action principale ; listes simples. |
| Saisie | Toujours `ModalBottomSheet` (`skipPartiallyExpanded = true`), `imePadding`, focus auto sur le champ. `KeyboardType.Decimal` ; `,` et `.` acceptés, parsing par le parseur de `:core`/domain, affichage au séparateur de la locale. |
| Suppression | `SwipeToDismissBox` fin→début seulement, fond `surface3` + icône `error`. Suppression immédiate, snackbar « Supprimé · Annuler » (`SnackbarDuration.Long`), annuler = réinsertion même id. TalkBack : `semantics { customActions = Supprimer }` obligatoire (le swipe n'est pas accessible). |
| État vide | `EmptyState(icône, titre, texte, action)` — toujours une action. |
| Chargement | Rien < 300 ms ; ensuite `LinearProgressIndicator` sous le champ de recherche (pas de spinner plein écran). Liste locale (Room) : pas d'état de chargement visible. |
| Erreur réseau | Bandeau inline `surface2` + texte + bouton « Réessayer ». Distinguer : hors ligne / erreur serveur / limite de requêtes / aucun résultat (ce dernier = état vide, pas erreur). |
| Edge-to-edge | `enableEdgeToEdge()` (déjà fait) ; `Scaffold` racine unique, `contentWindowInsets` propagés, pas de padding système en dur. |
| Predictive back | Navigation-compose + `ModalBottomSheet` gèrent l'animation. À vérifier : `targetSdk` ≥ 36 (actif par défaut) sinon `android:enableOnBackInvokedCallback="true"` dans le manifest (absent aujourd'hui). |
| TalkBack | Ligne d'aliment = un nœud fusionné (« Pomme, 150 g, 78 kcal ») ; nombres annoncés avec unité complète ; graphes avec résumé `contentDescription`. |

## 6. Graphes

**Décision : Vico (compose-m3, Apache-2.0, déjà en 3.3.1) pour toutes les séries temporelles. Canvas/`Box` maison uniquement pour les jauges simples (barre de macros, progression).**

| Critère | Vico | Canvas maison |
|---|---|---|
| Axes, échelle, scroll/zoom, marqueur au toucher | Fourni | ~1 000 lignes à écrire et tester |
| Licence / F-Droid | Apache-2.0, pur Kotlin, déjà présent | — |
| Thème | `rememberVicoTheme`/couleurs injectées depuis tokens | Natif |
| Risque | API changeante entre majeures (2→3) → confiné à `TimeSeriesLineChart` dans `:core-ui` | Maintenance |

Une barre de macros = 3 `Box` proportionnels : Vico serait disproportionné. Aucune feature n'importe Vico directement.

## 7. API `:core-ui`

```kotlin
object TallyTheme { val colors: TallyColors @Composable get; val type: TallyTypography @Composable get
  val spacing: TallySpacing @Composable get; val dimens: TallyDimens @Composable get; val reduceMotion: Boolean @Composable get }
@Composable fun TallyTheme(mode: ThemeMode = ThemeMode.Oled, dynamicAccent: Boolean = false, content: @Composable () -> Unit)
enum class ThemeMode { Oled, Light, System }

@Composable fun TallyCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)
@Composable fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null, action: (@Composable () -> Unit)? = null)
@Composable fun FoodRow(name: String, detail: String, kcal: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onDelete: (() -> Unit)? = null)
@Composable fun MacroBar(protein: Float, carbs: Float, fat: Float, modifier: Modifier = Modifier, showLegend: Boolean = true) // grammes, normalisés en interne
@Composable fun ProgressGauge(value: Float, target: Float, label: String, modifier: Modifier = Modifier)
@Composable fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, unit: String, modifier: Modifier = Modifier, error: String? = null, imeAction: ImeAction = ImeAction.Done, onImeAction: () -> Unit = {})
@Composable fun QuantitySheet(title: String, value: String, onValueChange: (String) -> Unit, unit: String, units: List<String>, onUnitChange: (String) -> Unit, error: String?, onConfirm: () -> Unit, onDismiss: () -> Unit, preview: (@Composable () -> Unit)? = null)
@Composable fun EmptyState(icon: Painter, title: String, body: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier)
@Composable fun ErrorBanner(message: String, retryLabel: String, onRetry: () -> Unit, modifier: Modifier = Modifier)
@Composable fun TimeSeriesLineChart(...) // existant, pas encore thémé (jalon Corps) ; ajouter summary: String pour TalkBack
object TallyIcons { @DrawableRes val add: Int; val delete: Int; val settings: Int; val scale: Int; /* … */ }
```

Tous stateless, textes déjà formatés par le ViewModel (pas de formatage d'unité dans `:core-ui`). Icônes : Material Symbols Outlined, poids 400, 24 dp, convertis en vector drawables dans `core-ui/src/main/res/drawable/ic_*.xml` (Apache-2.0, google/material-design-icons). Retirer `material-icons-core` (utilisé aujourd'hui par `:app`, `:nutrition`, `:body`).

## 8. Enforcement et tests

**Scan de source : tâche `checkDesignTokens` ajoutée à `checkArchitecture`** (même modèle que `checkNoDestructiveMigration`, mais `inputs.files(...)` déclarés pour le cache). Portée : `{nutrition,body,activity,app}/src/main/**/*.kt`. `:core-ui` exclu (c'est la source des tokens).

| Interdit | Regex (par ligne) |
|---|---|
| Couleur littérale | `Color\(\s*0x` · `Color\.(Black\|White\|Red\|Green\|Blue\|Gray\|DarkGray\|LightGray\|Yellow\|Cyan\|Magenta)\b` |
| Dimension littérale | `\b(?!0\.dp)\d+(\.\d+)?\.(dp\|sp)\b` |
| Forme littérale | `(RoundedCornerShape\|CircleShape\|CutCornerShape)\(` |
| Style ad hoc | `\b(fontSize\|fontWeight\|letterSpacing)\s*=` |
| Animation directe | `\btween\s*\(` · `\bspring\s*\(` |
| Icônes Material | `androidx\.compose\.material\.icons` |

Exceptions : suffixe de ligne `// design-ok: <raison>` (raison obligatoire, la regex l'exige), `Color.Transparent`/`Color.Unspecified` autorisés, `src/test` et `src/debug` hors portée. Lint custom (module lint-api) et detekt rejetés pour l'instant : coût de setup > gain sur 4 modules. État actuel : `JournalScreen.kt` contient des `.dp` littéraux → la tâche échouera tant que la migration n'est pas faite (activer la tâche dans le même commit que la migration).

**Roborazzi** (dernière version vue : 1.76.0, publiée le 29 sept., année non affichée, probablement 2026 ; source : github.com/takahirom/roborazzi/releases).

| Point | Décision |
|---|---|
| Dépendances | `io.github.takahirom.roborazzi` (plugin) + `roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule` en `testImplementation` ; Robolectric 4.17 existant ; `ui-test-junit4` + `ui-test-manifest` (debug). |
| Config | `@RunWith(AndroidJUnit4)`, `@GraphicsMode(GraphicsMode.Mode.NATIVE)`, `@Config(sdk = [35], qualifiers = "w411dp-h914dp-xxhdpi")`, `testOptions.unitTests.isIncludeAndroidResources = true`. |
| Variantes | 3 par cas : OLED (`+night`), Clair, police 200 % (`RuntimeEnvironment.setFontScale(2f)`), plus reduce-motion forcé (captures stables). |
| Emplacement | `:core-ui/src/test` : galerie (1 test par composant, états vide/erreur/chargement). `:nutrition/src/test` : `JournalContent(state, onEvent)` — exige d'extraire un composable stateless de `JournalScreen` alimenté par un état fixe (pas de ViewModel/Hilt). |
| Référence | Images dans `src/test/screenshots/` versionnées ; `verifyRoborazziDebug` en CI Linux, `recordRoborazziDebug` en local. Convention plugin `tally.android.screenshot` dans `build-logic`. |

Incertitudes : (1) compatibilité du plugin Gradle Roborazzi avec AGP 9.4 (nouveau DSL, Kotlin intégré) non vérifiée → spike d'1 module d'abord ; repli : bibliothèque seule + propriété `-Droborazzi.test.verify=true` sans plugin. (2) Rendu natif Robolectric variable selon OS hôte → ne vérifier qu'en CI Linux. (3) Comportement exact de Compose vis-à-vis de `ANIMATOR_DURATION_SCALE`.

## 9. État d'implémentation (jalon UI)

Fait : thème (`TallyTheme(mode, dynamicAccent)`), tokens, `TallyIcons` (5 icônes vector), composants §7, `checkDesignTokens` (baseline), captures Roborazzi (bibliothèque seule, sans plugin Gradle), écran Journal migré.
Écarts vs spec :
- FAB sans ombre (`elevation = 0`) ; icône d'état vide = restaurant.
- `FoodRow` a un paramètre `secondary` (macros) et `deleteLabel` ; `QuantitySheet` a `label`/`confirmLabel`/`confirmEnabled`.
- Annuler une suppression réinsère une copie (nouvel id) via `JournalRepository.snapshot` ; pas de soft-delete.
- Réglage thème (OLED/Clair/Système) et dynamic accent : API prête, pas encore câblée dans Réglages. L'app tourne en OLED fixe.
- Réglages en icône + 3 onglets : non fait (touche `:app`, hors pilote). `TopLevelDestination` garde 4 onglets.
- Le test de captures Journal fixe `fr` ; sheet de saisie visible dans `journal_target_sheet_*`.
