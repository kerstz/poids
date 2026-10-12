# AUDIT — App Android FOSS nutrition / poids / sport

| Champ | Valeur |
|---|---|
| Date | 2026-10-11 |
| Phase | Audit + plan uniquement. 0 code, 0 modif de projet existant |
| Dépôts audités | `audit/waistline`, `audit/OpenTracks`, `audit/openScale`, `audit/OpenNutriTracker` (`git clone --depth 1`, lecture seule) |
| Exclu | `Archive/` (ancienne implémentation, ignorée sur demande) |
| Méthode | 1 sous-agent Haiku par dépôt (exploration) ; 1 sous-agent Opus (sections 3-5) |
| Limite générale | Clones superficiels (1 commit) : historique et fréquence de maintenance non mesurables localement |
| Légende | **F** = fait vérifié dans le dépôt · **H** = hypothèse non vérifiée · **D** = décision |

---

## 1. Audit des projets (constats vérifiés)

### 1.1 Vue d'ensemble

| Dépôt | Techno | Version | Dernier commit (clone) | Licence | Rôle pour nous |
|---|---|---|---|---|---|
| Waistline | Cordova 12 + JS/jQuery + Framework7, IndexedDB v34 | 3.11.1 (`config.xml`) | 2026-09-19 | GPLv3 (`www/LICENSE.txt`) | Référence fonctionnelle nutrition, OFF, barcode |
| OpenTracks | Java (233 `.java`, 0 `.kt`), vues XML, SQLite brut, mono-module | v4.29.0 (`build.gradle`) | 2026-10-08 | Apache-2.0 (`LICENSE`) | Référence GPS, service, GPX |
| openScale | Kotlin + Compose + Room + Hilt, mono-module `:app` | 3.1.3 (code 77) | 2026-09-27 | GPLv3 (`LICENSE`) | Référence poids, mesures, BLE, backup |
| OpenNutriTracker | Flutter/Dart, Hive, flutter_bloc, get_it | 2.4.0+66 | 2026-09-26 | GPLv3 (`LICENSE`) | Référence archi nutrition, calculs, export |

### 1.2 Waistline — `audit/waistline`

| Domaine | Constat | Source | Statut |
|---|---|---|---|
| Architecture | Cordova, `www/activities/{diary,foodlist,foods-meals-recipes,meals,recipes,goals,statistics,settings,setup-wizard}` | arborescence, `package.json` | F |
| SDK | minSdk 21 / target 36 | `config.xml` | F |
| Stockage | IndexedDB `waistlineDb` v34 ; stores `foodList`, `diary`, `meals`, `recipes` ; réglages en `localStorage` | `www/assets/js/db-handler.js:23-27` | F |
| Migrations | `upgradeDatabase`, blocs `oldVersion < 30/31/34` | `db-handler.js:170` | F |
| Barcode | `phonegap-plugin-barcodescanner` (patché) ; recherche locale par index `barcode` avant réseau | `www/activities/foodlist/js/foodlist.js:442-448` | F |
| OFF | `world.openfoodfacts.org/api/v0/product/{code}.json`, `cgi/search.pl?search_simple=1&page_size=50&json=1` | `www/activities/foodlist/js/open-food-facts.js:29-31` | F |
| User-Agent OFF | `"Waistline - Android - Version <v> - https://github.com/davidhealey/waistline"` | `open-food-facts.js:60,402` | F |
| Cache OFF | Pas de cache dédié (grep négatif) ; un aliment scanné est enregistré dans `foodList` | `open-food-facts.js` | F |
| USDA | `api.nal.usda.gov/fdc/v1/foods/search`, `DEMO_KEY` par défaut, clé utilisateur sinon | `foodlist/js/usda.js:68-73` | F |
| Attribution ODbL | Aucune dans l'app (seulement lien de don OFF) | `README.md:66` | F |
| Journal | Entrée = référence + portion + nombre de portions ; aliment modifié = archivage (cohérence historique) | `FAQ.md:27,52-56` | F |
| Export | JSON `waistline_export.json` versionné (`DB.version`) + réglages ; secrets vidés avant export | `db-handler.js:730-760`, `settings.js:431-438` | F |
| Poids / mesures | `body-stats.js` : stats corporelles personnalisables (ordre, unités) | `www/activities/settings/js/body-stats.js:22-30` | F |
| Calcul énergie | Formule type Mifflin-St Jeor + facteur activité, objectif ±20 % | `setup-wizard/js/setup-wizard.js:90-100,147-151` | F (Mifflin = H) |
| Graphiques | Chart.js 2.9.4 | `statistics/js/statistics.js` | F |
| Tests / CI | Pas de `.github`, pas de CI, pas de suite automatisée (`tinytest.js` manuel) | arborescence | F |
| Anti-patterns | Mot de passe OFF en query string, `usesCleartextTraffic=true`, CSP `unsafe-eval`, clés en localStorage, version `2.9.2` en dur | `open-food-facts.js:397`, `config.xml`, `www/index.html:19`, `www/index.js:139` | F |
| Licences libs embarquées | jQuery, Framework7, Chart.js, he : MIT ; Font Awesome : CC BY 4.0 / OFL / MIT | en-têtes | F |
| Licences plugins Cordova | Non vérifiées (`node_modules` absent) | — | H |

### 1.3 OpenTracks — `audit/OpenTracks`

| Domaine | Constat | Source | Statut |
|---|---|---|---|
| Langage / UI | Java uniquement, vues XML + ViewBinding (pas Compose) | `build.gradle` | F |
| Build | AGP 9.4.0, Gradle 9.7.1, minSdk 26, target 37 | `build.gradle`, `gradle/wrapper/gradle-wrapper.properties` | F |
| Play Services | Aucune dépendance `gms`/Firebase | `build.gradle`, manifeste | F |
| Room | Absent ; `SQLiteOpenHelper` + ContentProvider maison | `data/CustomSQLiteOpenHelper.java`, `data/CustomContentProvider.java` | F |
| DB | `DATABASE_VERSION = 42`, migrations par pas `upgradeFrom23to24`…`41to42` | `CustomSQLiteOpenHelper.java` | F |
| Tables | `tracks`, `trackpoints` (FK `trackid` ON DELETE CASCADE, `elevation`, `elevation_gain/loss`, `type` CHECK), `markers` | `data/tables/*Columns.java` | F |
| Service | `TrackRecordingService` ; `startForeground` type `location|connectedDevice` ; `START_STICKY`, reprise via `TrackIdStorage` | `services/TrackRecordingService.java:146-159,221`, manifeste l.423 | F |
| GPS | `LocationManagerCompat.requestLocationUpdates`, `GPS_PROVIDER`, `QUALITY_HIGH_ACCURACY`, intervalle min configurable (défaut 0 s) | `sensors/driver/GpsInternal.java:40-110` | F |
| Filtres | Précision seuil 50 m (défaut) ; distance 10 m (idle/mouvement) ; perte signal 30 s | `sensors/GpsHandler.java:86-95`, `TrackStatisticsUpdater.java:171`, `GpsStatusManager.java:25` | F |
| Pause | Timer idle → point `IDLE` ; reprise sur distance (pas de seuil de vitesse) | `services/TrackRecordingManager.java:156-158,240-241` | F |
| Wakelock | `PARTIAL_WAKE_LOCK` | `sensors/SensorManager.java:63,80`, `util/SystemUtils.java:85` | F |
| Altitude | EGM2008 (`TrackPointWithEGM2008`), `AltitudeChangeHandler` | `services/TrackRecordingManager`, `sensors/AltitudeChangeHandler.java` | F (détail = H) |
| Export | GPX (`<ele>`, `gpxtpx:speed`), KML, KMZ, CSV ; pas de GeoJSON | `io/file/exporter/*.java` | F |
| Import | GPX (extensions Garmin TPX v1/v2, PowerExtension) | `io/file/importer/GPXTrackImporter.java` | F |
| Permissions | FINE/COARSE_LOCATION, FOREGROUND_SERVICE(+LOCATION,+CONNECTED_DEVICE), WAKE_LOCK, VIBRATE, POST_NOTIFICATIONS, BLUETOOTH_*, Health Connect WRITE_* ; **pas d'INTERNET** ; `ACCESS_BACKGROUND_LOCATION` commentée | `src/main/AndroidManifest.xml:27-76` | F |
| Tests | 0 test unitaire ; 67 `androidTest` | `src/androidTest` | F |
| CI | Forgejo `.forgejo/workflows/test.yaml` ; Renovate | racine | F |
| Deps à vérifier | `core-location-altitude`, `desugar_jdk_libs` (tirent-elles Play Services ? licences) | — | H |

### 1.4 openScale — `audit/openScale`

| Domaine | Constat | Source | Statut |
|---|---|---|---|
| Structure | Projet dans `android_app/`, module unique `:app`, `com.health.openscale.core/{bluetooth,database,data,model,usecase,facade,worker,service}` + `ui/` | `android_app/settings.gradle.kts` | F |
| Stack | Kotlin 2.4.20, AGP 9.2.1, Compose BOM 2026.05.01, Room 2.8.4 (KSP), Hilt 2.60.1, DataStore, WorkManager, Glance | `android_app/gradle/libs.versions.toml` | F |
| SDK | minSdk 31, target 37 | `app/build.gradle.kts` | F |
| Room | Schéma **v18**, `exportSchema=true`, schémas JSON v7-18, migrations `1_2`…`17_18`, tests `MigrationTest.kt` | `core/database/AppDatabase.kt`, `app/schemas/` | F |
| Entités | `User`, `Measurement(id,userId,timestamp)`, `MeasurementValue` (EAV : float/int/text/date), `MeasurementType`, `UserGoals` | `core/data/*.kt` | F |
| Types | Clés `builtin.*` : poids, IMC, masse grasse, eau, muscle, LBM, os, taille, hanches, plis, BMR, TDEE, FC, calories, protéines, photo… | `core/data/MeasurementType.kt` | F |
| Unités | KG, LB, ST, PERCENT, CM, INCH, KCAL, BPM, OHM | `core/data/Enums.kt:374` | F |
| Nutrition / sport | Aucune entité (seulement types CALORIES, PROTEIN) | — | F |
| BLE | `core/bluetooth/scales/` (72 fichiers) + `libs/` (24) ; `ScaleDeviceHandler` (abstraite), `GattScaleAdapter`, `SppScaleAdapter`, `BroadcastScaleAdapter`, `ScaleFactory` | `scales/ScaleDeviceHandler.kt:109` | F |
| BLE libs | `blessed-kotlin` 3.1.0 (MIT), `bouncycastle` 1.85.2 | `libs.versions.toml`, `CREDITS` | F (licence BC = H) |
| BLE perms | `BLUETOOTH_SCAN` (`neverForLocation`), `BLUETOOTH_CONNECT` ; pas de localisation | `AndroidManifest.xml`, `ui/screen/components/BluetoothActionButton.kt:165-175` | F |
| Graphiques | Vico 3.3.1 (Apache-2.0) | `ui/screen/components/MeasurementChart.kt` | F |
| Backup | ZIP : DB principale + `-shm`/`-wal` + images ; restore ZIP ou `.db` ; validation `sqlite_master` ; `AutoBackup` via WorkManager | `core/usecase/BackupRestoreUseCases.kt`, `AutoBackupUseCases.kt`, `core/worker/BackupWorker.kt` | F |
| Export | CSV (kotlin-csv, Apache-2.0), ZIP si photos ; pas de JSON | `core/usecase/ImportExportUseCases.kt` | F |
| Permissions | BLUETOOTH_CONNECT/SCAN, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS, VIBRATE ; `INTERNET` **retiré** (`tools:node="remove"`) ; `allowBackup=false` | `AndroidManifest.xml` | F |
| Point d'attention | `DatabaseProvider` exporté protégé par permission custom `dangerous` | `AndroidManifest.xml` | F |
| Tests | 97 fichiers unitaires (JUnit4, Truth, Robolectric) ; CI GitHub Actions | `app/src/test/`, `.github/workflows/ci_master.yml` | F |
| Risque | EAV : requêtes de graphes/tableaux plus coûteuses | — | H |

### 1.5 OpenNutriTracker — `audit/OpenNutriTracker`

| Domaine | Constat | Source | Statut |
|---|---|---|---|
| Architecture | `lib/core/{data,domain,presentation}` + `lib/features/<f>/{data,domain,presentation}` ; flutter_bloc, provider, get_it | `lib/`, README l.254 | F |
| Persistance | Hive CE (chiffrement AES annoncé, clé via secure storage) | `lib/core/utils/secure_app_storage_provider.dart` | F (présence) |
| Backend tiers | Supabase + Sentry (présents) | `pubspec.yaml`, `sentry_config.dart` | F |
| Entités | Intake, Meal, WeightLog, MealNutriments, IntakeType, User, TrackedDay, UserActivity, PhysicalActivity, Recipe, RecipeIngredient, WaterIntake, FastingSession… | `lib/core/data/dbo/*.dart` | F |
| Repas | 4 types : breakfast / lunch / dinner / snack | `lib/core/domain/entity/intake_type_entity.dart` | F |
| Énergie | TEE IOM 2005 direct (pas de BMR séparé) : H `864 − 9.72·âge + PA·(14.2·kg + 503·m)` ; F `387 − 7.31·âge + PA·(10.9·kg + 660.7·m)` | `lib/core/utils/calc/tdee_calc.dart` | F |
| PAL | 1.25 / 1.5 / 1.75 / 2.2 — le code signale des valeurs non sourcées | `lib/core/utils/calc/pal_calc.dart` | F |
| Macros | 4 / 4 / 9 kcal/g ; défaut 60 % glucides, 15 % protéines | `lib/core/utils/calc/macro_calc.dart` | F (défauts = H) |
| Objectif | ~7700 kcal/kg | `lib/core/utils/calc/calorie_goal_calc.dart` | F |
| OFF | `search.openfoodfacts.org/search`, fallback `world…/cgi/search.pl`, `…/api/v2/product/{barcode}` | `lib/core/utils/off_const.dart` | F |
| User-Agent | `OpenNutriTracker - <platform> - Version <v> - <url>` via `ONTHttpClient` | `lib/core/utils/app_const.dart`, `ont_http_client.dart` | F |
| Cache | Recherche : Hive, purge 90 j ; images 30 j | `remote_search_cache_data_source.dart`, `lib/main.dart:87`, `ont_image_cache_manager.dart` | F |
| Barcode | `mobile_scanner` ; validation 8-14 chiffres + check-digit EAN-13 | `lib/core/utils/barcode_validator.dart` | F |
| FDC | Via backend Supabase (dépôt séparé), pas d'appel direct | `sp_food_data_source.dart` | F |
| Export | ZIP JSON **ou** CSV, versionné, `export_write_verifier`, importers JSON/CSV | `docs/export-format.md`, `lib/features/settings/domain/usecase/` | F |
| Backup chiffré | Non trouvé | — | H |
| Tests / CI | 224 `*_test.dart` ; GitHub Actions | `test/`, `.github/workflows/` | F |

### 1.6 Synthèse : quoi reprendre en idée

| Besoin | Meilleure référence | Idée retenue |
|---|---|---|
| Journal + portions + archivage aliments | Waistline | Entrée = référence aliment + portion ; aliment modifié → archivé, pas écrasé |
| Recherche OFF / cache | OpenNutriTracker | Search-a-licious + fallback + barcode v2 ; cache TTL ; validation EAN |
| Calcul énergie | OpenNutriTracker (IOM 2005) | À recouper avec la littérature (code non sourcé pour PAL) |
| Poids / mesures / objectifs | openScale | Types de mesure + unités + objectifs ; migrations testées |
| Backup / restore | openScale | ZIP DB + WAL + images, validation à la restauration |
| Export versionné | OpenNutriTracker + Waistline | JSON versionné + CSV ; secrets exclus |
| Service GPS | OpenTracks | Foreground `location`, `START_STICKY`, reprise par id, filtre précision, wakelock partiel |
| GPX | OpenTracks | `<ele>`, extension vitesse, import extensions Garmin |
| BLE | openScale | `ScaleDeviceHandler` + adapters GATT ; blessed-kotlin |

---

## 2. Licences et obligations

### 2.1 Par dépôt

| Dépôt | Licence | Copyright (F) | Réutiliser du code | Réimplémenter | Obligations si code repris |
|---|---|---|---|---|---|
| Waistline | GPLv3 | David Healey 2018-2021 (en-têtes) | Possible : projet cible GPLv3 ; ici JS/Cordova donc **sans valeur pratique** | Oui (comportement, flux) | Conserver en-têtes + licence, fournir source, mention des modifications |
| OpenTracks | Apache-2.0 | Dennis Guse et contributeurs | Possible (Apache → GPLv3 compatible, sens unique) | Possible | Conserver copyright + `NOTICE` si présent, joindre licence, signaler fichiers modifiés ; œuvre combinée sous GPLv3 |
| openScale | GPLv3 | olie.xdev 2025 | Possible si cible GPLv3 | Possible | Idem GPLv3 : source, licence, attribution |
| OpenNutriTracker | GPLv3 | auteurs du dépôt | Code Dart : **sans valeur pratique** | Oui (formules = littérature) | Idem GPLv3 |

### 2.2 Dépendances candidates

| Dépendance | Source | Licence | Statut | Compatibilité GPLv3 |
|---|---|---|---|---|
| Vico (graphes) | openScale `CREDITS` | Apache-2.0 | F | Oui |
| kotlin-csv | openScale | Apache-2.0 | F | Oui |
| blessed-kotlin | openScale | MIT | F | Oui |
| Bouncy Castle | openScale | MIT-like (à confirmer) | H | Probable |
| zxing-cpp | CLAUDE.md | Apache-2.0 | F (imposé) | Oui |
| AndroidX / Compose / Room | — | Apache-2.0 | H (non relu dans les dépôts) | Oui |
| desugar_jdk_libs | OpenTracks | GPL-2.0 + Classpath Exception | H | À confirmer |
| Plugins Cordova | Waistline | non vérifiées | H | Sans objet (non repris) |
| Supabase / Sentry | OpenNutriTracker | — | F (présents) | **Exclus** : télémétrie / cloud interdits |

### 2.3 Données

| Source | Licence | Obligations | Statut |
|---|---|---|---|
| Open Food Facts (base) | ODbL | Attribution ; partage à l'identique des bases dérivées ; mention de la licence | H (non écrit dans les dépôts) |
| Open Food Facts (contenu / images) | DbCL (contenu) / CC BY-SA (images) | Attribution ; partage identique pour images | H |
| USDA FDC | CC0 | Aucune | F (README ONT l.87) |
| BLS (via ONT) | CC BY 4.0 | Attribution | F (README ONT l.281) |
| Waistline | — | Aucune attribution ODbL implémentée → **ne pas imiter** | F |

### 2.4 Réutilisation vs réimplémentation

| Élément | Décision proposée | Justification |
|---|---|---|
| Code des 4 dépôts | **Réimplémentation** (aucune copie) | Langages différents (JS, Dart, Java) ; Kotlin/Compose imposé ; évite l'obligation de suivi de copyright |
| Formules énergie | Réimplémenter depuis la littérature (IOM 2005, Mifflin-St Jeor), citer la source | Faits publiés ; code ONT non sourcé pour PAL |
| Schéma openScale | S'inspirer, pas de copie | Même licence cible mais EAV écarté (voir §4) |
| Protocoles BLE | Phase Avancé : réimplémenter par protocole, ou reprendre `blessed-kotlin` (MIT) | Éviter la copie de handlers GPLv3 sans nécessité |
| Format GPX | Réimplémenter depuis le schéma GPX 1.1 public | Standard ouvert |

### 2.5 Points nécessitant vérification juridique

| # | Point | Pourquoi |
|---|---|---|
| J1 | Portée exacte de l'ODbL sur un cache local d'OFF et sur l'export utilisateur | « Base dérivée » : seuil flou |
| J2 | Licence des images OFF (CC BY-SA) si affichées / mises en cache / exportées | Partage identique |
| J3 | Compatibilité effective Apache-2.0 → GPLv3 en cas de reprise de fragments OpenTracks (GPLv3 §7, version de la GPL) | Sens unique, à confirmer par juriste |
| J4 | Réimplémentation « à partir du comportement » de handlers BLE GPLv3 | Frontière œuvre dérivée |
| J5 | Licence réelle de `desugar_jdk_libs`, Bouncy Castle, `core-location-altitude` | Non lues dans les POM |
| J6 | Obligation de mentionner Waistline / OpenTracks / openScale / ONT comme « inspirations » | Courtoisie vs obligation (aucune si 0 code repris) |
| J7 | Conformité RGPD d'un journal nutrition/poids (donnée de santé) : stockage local seul, export utilisateur | Données de santé = catégorie particulière |

---

## 3. Architecture cible

### 3.1 Graphe de modules Gradle

| Module | Type | Contenu | Dépend de |
|---|---|---|---|
| `:build-logic` | included build | convention plugins (android-lib, compose, room, hilt, jvm-lib), règles lint, vérif. permissions | — |
| `:core` | Kotlin/JVM pur | unités + conversions (kg/lb/st, m/cm/in, km/mi, kcal/kJ), `Clock`/`ZoneProvider`, `DispatcherProvider`, `Result`/erreurs, validation EAN | — |
| `:domain` | Kotlin/JVM pur | modèles métier, **interfaces** de repositories, use cases transverses (calcul énergie sport, TDEE, objectifs du jour, `weightAt(instant)`) | `:core` |
| `:data` | Android lib | **une seule** base Room, DAO, implémentations repositories, client OFF (OkHttp), cache OFF, backup/restore, export CSV/GPX, import GPX | `:domain`, `:core` |
| `:core-ui` | Android lib Compose | thème M3, composants, formatage unités/dates, wrappers graphiques Vico, `Canvas` tracé GPS | `:core` |
| `:nutrition` | feature Android | écrans + ViewModels journal/aliments/recettes/recherche OFF, scan CameraX + zxing-cpp, use cases propres | `:domain`, `:core`, `:core-ui` |
| `:body` | feature Android | écrans + VM poids/mesures/graphes, (Avancé) BLE balances | `:domain`, `:core`, `:core-ui` |
| `:activity` | feature Android | écrans + VM sport, `TrackingService` (FGS), `GpsSource` (LocationManager), filtres GPS | `:domain`, `:core`, `:core-ui` |
| `:app` | application | `MainActivity`, NavHost, bottom bar, modules Hilt de binding (interface→impl), manifeste final | tous |

- [D] `:domain` séparé : justification = (1) les features ne voient jamais `:data`/Room, (2) use cases transverses (sport lit le poids, nutrition lit objectifs) sans dépendance latérale entre features, (3) tests JVM rapides sans Robolectric.
- [D] `:core-ui` séparé : justification = cohérence visuelle des 3 features (réimplémentation unifiée, pas fusion d'UI) + un seul point d'intégration Vico.
- [D] Pas de module `:scanner` séparé en MVP : scan reste dans `:nutrition` ; extraction si `:body` en a besoin (pas prévu).

Règles de dépendance (vérifiées en CI) :

| Règle | Mécanisme |
|---|---|
| feature ↛ feature (`:nutrition` ↛ `:body` ↛ `:activity`) | plugin `com.jraska.module.graph.assertion` (Apache-2.0) ou tâche custom `build-logic` |
| feature ↛ `:data` | idem |
| `:domain`, `:core` = 0 dépendance Android | modules `kotlin("jvm")` → impossible à la compilation |
| navigation cross-feature | lambdas `onOpenX()` injectées par `:app`, jamais d'import de route d'une autre feature |
| un seul `RoomDatabase` | uniquement dans `:data` |

Packages par module feature (ex. `:nutrition`) :
```
org.<app>.nutrition
 ├─ domain/     use cases spécifiques (ComputeDayTotals, ResolveBarcode…)
 ├─ ui/journal  Screen + ViewModel + UiState
 ├─ ui/food     ui/recipe  ui/search  ui/scan
 └─ navigation/ NutritionRoutes + NavGraphBuilder.nutritionGraph(...)
```
`:data` : `db/` (entities, dao, migrations, converters), `repository/` (impl), `remote/off/`, `backup/`, `export/` (csv, gpx), `di/`.

### 3.2 DI

| Option | Verdict | Raison |
|---|---|---|
| **Hilt** | **[D] RETENU** | [F] utilisé par openScale (GPLv3, distribué sur F-Droid) ; Apache-2.0 ; support natif `@AndroidEntryPoint Service`, `@HiltWorker`, `hiltViewModel()` ; erreurs à la compilation |
| Koin | rejeté | erreurs de graphe au runtime ; aucun gain FOSS |
| Manuel (AppContainer) | rejeté | viable mais boilerplate croissant avec Service + Worker + 3 features + BLE |

- [D] KSP uniquement (pas de kapt). Bindings interface→impl dans `:data/di` (`@Binds`), installés via `:app`.

### 3.3 Navigation

- [D] Navigation Compose 2.x **type-safe** (routes `@Serializable`) — [F] version 2.10.0 utilisée par openScale. Navigation 3 non retenue (moins de recul).
- [D] 4 destinations top-level : Journal (nutrition) · Corps · Sport · Réglages. Chaque feature expose `NavGraphBuilder.xxxGraph(navController, callbacks)`.
- [D] Deep link interne unique : notification de suivi GPS → écran session en cours.

### 3.4 Décisions techniques

| Sujet | Décision [D] | Détails | Statut |
|---|---|---|---|
| Scan code-barres | CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`) + **`io.github.zxing-cpp:android`** (wrapper Kotlin/JNI officiel du projet zxing-cpp, Maven Central) | `ImageAnalysis` STRATEGY_KEEP_ONLY_LATEST, résolution analyse ~1280×720, formats limités EAN-13/EAN-8/UPC-A/UPC-E, validation checksum (8–14 chiffres, cf. OpenNutriTracker), 2 lectures identiques consécutives avant validation | [H] version exacte (2.x vs 3.x) et API (`BarcodeReader`) à figer au jalon 6 |
| ↳ licence / taille | Apache-2.0 [F] ; .so natif par ABI | [H] ~1–2 Mo/ABI non vérifié → mitigation : splits ABI (arm64-v8a, armeabi-v7a, x86_64) ; vérifier alignement 16 Ko | à mesurer |
| ↳ alternatives | `com.google.zxing:core` 3.5.x (Java pur, Apache-2.0) = **plan B** derrière l'interface `BarcodeDecoder` | `zxing-android-embedded` rejeté (Camera1, UI imposée, maintenance minimale) | — |
| GPS | `LocationManager` natif via `LocationManagerCompat`, **GPS_PROVIDER seul** | `minTime=1000 ms`, `minDistance=0` (filtrage applicatif) ; filtres : précision ≤ 50 m [F OpenTracks], distance min entre points gardés 5 m marche/rando, 10 m course/vélo [F OpenTracks=10 m] ; rejet vitesse aberrante (> plafond par type) ; 1ers fixes ignorés tant que précision > seuil ; perte signal 30 s → marqueur de segment [F OpenTracks] ; auto-pause optionnelle (vitesse < seuil N s) | seuils = [H] à calibrer terrain |
| Altitude / dénivelé | altitude GPS + baromètre (`TYPE_PRESSURE`, sans permission) si présent ; gain/perte avec **hystérésis** (seuil à calibrer, [H] 3–5 m) | conversion ellipsoïde→MSL via `core-location-altitude` **seulement si** vérifié sans GMS ; sinon stocker ellipsoïde + flag (le dénivelé n'est pas affecté, seul l'`ele` absolu GPX l'est) | [H] dépendance GMS de `core-location-altitude` |
| Arrière-plan sport | **Foreground Service** `foregroundServiceType="location"` démarré par action utilisateur (app visible) | pas d'`ACCESS_BACKGROUND_LOCATION` [F OpenTracks l'a commenté] ; `PARTIAL_WAKE_LOCK` tenu seulement en RECORDING ; `START_STICKY` ; session active persistée en DB (`status=RECORDING`) ; au redémarrage du service ou de l'app : reprise ou clôture proposée ; points flushés en DB par lot (≤ 10 s ou 20 points) → perte max ≈ 10 s | [H] comportement redémarrage sticky FGS location sur Android 14+ |
| WorkManager | **Non** pour le GPS. **Oui** pour : backup auto (Avancé), purge cache OFF (MVP, périodique 1 j, contraintes `batteryNotLow`) | — | — |
| Optimisation batterie | lien vers `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (aucune permission) + aide OEM | pas de `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | — |
| Cache OFF | table Room `off_product_cache` (produit seulement, JSON filtré `fields=`) | TTL 30 j, *stale-while-revalidate* (affiche cache, rafraîchit si en ligne) ; négatif (NOT_FOUND) 7 j ; `If-None-Match`/`If-Modified-Since` si l'en-tête existe ; aliment ajouté au journal = **copie** dans `food` (le cache peut être purgé sans casser l'historique) ; recherche texte : **pas** de cache de résultats, hors ligne = FTS local sur `food` + cache produits | [H] OFF renvoie ETag/Last-Modified ? |
| Ordre de résolution code-barres | 1) `food.barcode` local (non archivé) → 2) `off_product_cache` valide → 3) réseau OFF → 4) formulaire aliment manuel pré-rempli du code | [F] Waistline : index local avant réseau | — |
| Endpoints OFF | produit : `world.openfoodfacts.org/api/v2/product/{code}?fields=…&lc=fr` ; recherche : Search-a-licious (`search.openfoodfacts.org/search`) + repli `cgi/search.pl` | [F] OpenNutriTracker fait ce double chemin ; derrière interface `FoodRemoteSource` | [H] stabilité Search-a-licious, quotas |
| User-Agent | obligatoire, intercepteur OkHttp : `NomApp/versionName (Android; +URL dépôt)` | jamais d'email perso ; pas d'identifiant OFF / mot de passe (anti-pattern Waistline) ; HTTPS only, `cleartextTrafficPermitted=false` | — |
| Rate limit | côté client : debounce recherche 500 ms, recherche déclenchée ≥ 3 caractères, file sérielle, backoff sur 429 | [H] quotas OFF (~100 req/min produit, ~10 req/min recherche) à vérifier | — |
| Client HTTP | **OkHttp** (Apache-2.0) sans Retrofit | 2 endpoints → Retrofit inutile ; MockWebServer pour tests ; timeouts explicites ; pas de cache disque OkHttp (cache = Room) | — |
| Sérialisation | **kotlinx.serialization** (JSON) | parsing OFF tolérant (`ignoreUnknownKeys`, `coerceInputValues`), backup, routes navigation ; pas de réflexion → R8 simple | — |
| Graphiques | **Vico** (`compose-m3`) pour séries temporelles (poids, mesures, kcal) ; **Canvas** maison pour tracé GPS et profils altitude/vitesse | [F] openScale Vico 3.3.1 | — |
| Carte | **Aucune carte tuilée en v1** : polyline projetée sur Canvas | osmdroid / MapLibre = option ultérieure (INTERNET + tuiles + attribution OSM) | à valider utilisateur |
| Paramètres | table Room `app_setting` (clé/valeur typée) | une seule source sauvegardée ; pas de DataStore | — |
| Backup / restore | **ZIP** : `manifest.json` (`formatVersion`, `appVersionCode`, `dbSchemaVersion`, `createdAt`, compteurs par table, SHA-256 par fichier) + `data/<table>.jsonl` (streaming, 1 ligne = 1 entité) | logique (pas de copie de fichier DB → pas de problème WAL ni de version SQLite) ; restore = valider manifeste → backup auto de sécurité → transaction unique wipe+insert → vérif compteurs ; importeurs `formatVersion N → courant` ; SAF (`CreateDocument`/`OpenDocument`) ; aucun secret à exclure (pas de compte) | — |
| Exports interop | CSV (poids/mesures, journal nutrition, sessions), GPX 1.1 par session (`ele`, `time`, extension vitesse `gpxtpx` [F OpenTracks]) | CSV RFC 4180, UTF-8, séparateur `,`, en-tête versionné, unités SI dans le nom de colonne (`weight_kg`) ; écrivain CSV maison (≈50 lignes) | — |
| Import | GPX (trk/trkseg/trkpt, extensions Garmin tolérées [F OpenTracks]) + backup ZIP ; CSV poids (Avancé) | parsing XmlPullParser (plateforme) | — |
| i18n | `values/` = **EN source**, `values-fr/` complet dès MVP ; strings par module ; `locales_config` (langue par app, API 33+) ; formats via `Locale` | Weblate ultérieurement | — |
| Build | version catalog, convention plugins, R8 + shrinkResources, builds reproductibles (pas de timestamp/VCS info), `gradle/verification-metadata.xml` | minSdk **26** (cf. décisions à valider), target = dernier stable | — |

### 3.5 FOSS / F-Droid par bibliothèque

| Lib | Licence | Anti-feature F-Droid | Verdict | Certitude |
|---|---|---|---|---|
| Kotlin stdlib, coroutines, kotlinx.serialization | Apache-2.0 | — | OK | [F] |
| AndroidX : Compose BOM, Material3, Activity, Lifecycle, Navigation, Room, WorkManager | Apache-2.0 | — | OK | [F] |
| CameraX (camera2/lifecycle/view) | Apache-2.0 | — | OK (aucune dépendance GMS) | [H] haute confiance, vérifier `dependencies` |
| zxing-cpp `android` | Apache-2.0 | — | OK | [H] acceptation F-Droid d'un AAR avec .so précompilé depuis Maven Central ; sinon compilation source (NDK) ou plan B zxing-core |
| zxing `core` (plan B) | Apache-2.0 | — | OK | [F] |
| Hilt / Dagger | Apache-2.0 | — | OK | [F] openScale sur F-Droid |
| OkHttp + Okio | Apache-2.0 | — | OK | [F] |
| Vico | Apache-2.0 | — | OK | [F] openScale |
| `androidx.core:core-location-altitude` | Apache-2.0 | ? | **Conditionnel** | [H] vérifier absence `play-services-*` dans l'arbre de dépendances (`./gradlew :activity:dependencies`) |
| blessed-kotlin (Avancé, BLE) | MIT | — | OK | [F] openScale |
| Tests : JUnit4, Turbine, Truth, Robolectric, MockWebServer, room-testing | Apache-2.0 / MIT | — (test only) | OK | [F] |
| module-graph-assertion plugin | Apache-2.0 | — (build only) | OK | [H] |
| **Exclus** : ML Kit, Play Services Location, Firebase, Sentry, Supabase, Health Connect (`connect-client`) | — | NonFreeDep / Tracking | INTERDIT | [F] contrainte |
| Service réseau OFF | données ODbL | `NonFreeNet` **non** applicable (service libre) | OK | [H] confirmer à la soumission |

### 3.6 Permissions minimales

| Permission | Phase | Module | Runtime | Justification |
|---|---|---|---|---|
| `INTERNET` | MVP | `:data` | non | OFF uniquement |
| `CAMERA` | MVP | `:nutrition` | oui (au 1er scan) | scan ; `uses-feature camera required=false` |
| `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` | Sport | `:activity` | oui (au 1er démarrage session) | Android 12+ exige de demander les deux |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` | Sport | `:activity` | non | FGS type location |
| `POST_NOTIFICATIONS` | Sport | `:activity` | oui (API 33+) | notification FGS visible |
| `WAKE_LOCK` | Sport | `:activity` | non | PARTIAL pendant RECORDING |
| `BLUETOOTH_SCAN` (`neverForLocation`) + `BLUETOOTH_CONNECT` | Avancé | `:body` | oui (API 31+) | [F] schéma openScale |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` `maxSdkVersion=30` | Avancé | `:body` | non | legacy ≤ API 30 (+ FINE_LOCATION déjà présente) |
| `RECEIVE_BOOT_COMPLETED` | Avancé | (fusion WorkManager) | non | reprogrammation backup auto ; à retirer (`tools:node="remove"`) tant que pas de backup auto |

- **Refusées** : `ACCESS_BACKGROUND_LOCATION`, `ACTIVITY_RECOGNITION`, `READ/WRITE_EXTERNAL_STORAGE` (SAF), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `ACCESS_NETWORK_STATE` (on tente la requête, échec = hors ligne).
- [D] Test CI : manifeste fusionné release comparé à une **liste blanche** versionnée ; toute permission nouvelle = build rouge.

### 3.7 Attribution ODbL / Open Food Facts

| Où | Quoi | Mécanique |
|---|---|---|
| Fiche aliment `source=OFF` | « Données : Open Food Facts — ODbL » + code-barres + lien produit | champ `food.source` + `off_code` → composant `OffAttribution` de `:core-ui` |
| Écran recherche OFF / résultat scan | même mention en pied de liste | composant partagé |
| Réglages › À propos › Sources de données | texte ODbL, lien licence, lien OFF, mention « contribuer sur OFF » | écran statique `:app` |
| Export JSON/CSV contenant des aliments OFF | `manifest.json` → `attributions: [{source:"Open Food Facts", license:"ODbL-1.0", url}]` ; CSV : colonne `source` | généré si ≥1 aliment `source=OFF` |
| Licences des libs | écran « Licences » généré au build | plugin de rapport de licences ([H] choix : AboutLibraries Apache-2.0) |
| Images produits (CC BY-SA) | **non téléchargées en v1** | évite licence images + bande passante ; à revoir |

- [F] Waistline n'affiche aucune attribution ODbL → anti-modèle à ne pas reproduire.

---

## 4. Modèle de données unifié

### 4.1 Conventions transverses

| Sujet | Décision [D] | Raison |
|---|---|---|
| Base | **une seule** `RoomDatabase` dans `:data` | FK inter-domaines, backup unique, transactions atomiques |
| IDs | `Long` auto-incrément (`autoGenerate`) partout ; `uuid TEXT UNIQUE` **uniquement** sur `activity_session` | mono-appareil sans sync ; restore = remplacement complet → pas besoin d'UUID globaux ; UUID session = dédoublonnage import GPX + nom de fichier stable |
| Instants | `*_at INTEGER` = epoch ms UTC + `zone_id TEXT` (ZoneId IANA à la saisie) | affichage fidèle en voyage ; DST |
| Jour nutrition | `local_date INTEGER` = `LocalDate.toEpochDay()` calculé avec la zone de saisie | groupage/index par jour sans calcul de zone en SQL |
| Unités stockées | masse corps kg · masse aliment g · volume ml · longueur m · distance m · vitesse m/s · durée ms · énergie **kcal** · % en 0–100 | SI sauf énergie : kcal = unité de référence nutrition et OFF ; conversion kJ en UI (×4,184) |
| Conversion | exclusivement `:core` (pure, testée) ; UI lit préférences `app_setting` | — |
| Nombres | `REAL` (Double) ; arrondi uniquement à l'affichage | — |
| Suppression | **archivage** (`archived_at`) pour entités référencées (food, recipe, measurement_type, meal_slot) ; **suppression physique** pour faits (journal, mesures, sessions) avec annulation UI | cohérence historique [F Waistline archive] |
| Audit | `created_at`, `updated_at` sur toutes les tables utilisateur | export/debug |
| Enums | `TEXT` (nom stable) + `CHECK(... IN (...))` | lisibles dans exports, robustes au réordonnancement |

### 4.2 Entités Room

**`profile`** (singleton)
| Champ | Type | Contraintes |
|---|---|---|
| id | INTEGER PK | `CHECK(id=1)` |
| birth_date | INTEGER (epochDay) | nullable |
| sex | TEXT | `FEMALE/MALE/UNSPECIFIED` (formules énergie) |
| height_m | REAL | nullable, `CHECK(height_m BETWEEN 0.5 AND 2.8)` |
| activity_level | TEXT | `SEDENTARY/LIGHT/MODERATE/ACTIVE/VERY_ACTIVE` |
| created_at, updated_at | INTEGER | — |
- [D] **Aucun champ poids** dans `profile`.

**`goal`** (historisé)
| Champ | Type | Contraintes |
|---|---|---|
| id | INTEGER PK | — |
| type | TEXT | `ENERGY_KCAL/PROTEIN_G/CARBS_G/FAT_G/BODY_WEIGHT_KG/…` |
| value | REAL | `CHECK(value >= 0)` |
| mode | TEXT | `FIXED/COMPUTED` (COMPUTED = recalcul par use case) |
| valid_from | INTEGER (epochDay) | `UNIQUE(type, valid_from)` |
| created_at | INTEGER | — |
- Index : `(type, valid_from DESC)`. Objectif d'un jour J = ligne `valid_from ≤ J` la plus récente.

**`meal_slot`**
| id PK | name TEXT nullable (null = libellé builtin i18n) | builtin_key TEXT UNIQUE nullable (`breakfast/lunch/dinner/snack`) | position INTEGER | archived_at INTEGER nullable |
- Seed : 4 créneaux [F OpenNutriTracker 4 types].

**`food`**
| Champ | Type | Contraintes |
|---|---|---|
| id | INTEGER PK | — |
| name, brand | TEXT | name NOT NULL |
| barcode | TEXT | nullable, `CHECK(length BETWEEN 8 AND 14)` |
| source | TEXT | `USER/OFF/IMPORT` |
| off_last_modified | INTEGER | nullable |
| base_unit | TEXT | `G/ML` |
| density_g_per_ml | REAL | nullable |
| energy_kcal, protein_g, carbs_g, sugars_g, fat_g, sat_fat_g, fiber_g, salt_g | REAL | **pour 100 g/ml** ; energy/protein/carbs/fat NOT NULL, autres nullable ; `CHECK(>=0)` **[non implémenté : validé par `FoodValidation`]** |
| archived_at | INTEGER | nullable |
| created_at, updated_at | INTEGER | — |
- Index : `barcode` **unique** (v2 ; NULL multiples permis, archivés inclus — un code = une ligne, le ré-import d'un archivé désarchive et rafraîchit la même ligne), `(archived_at, name)`.
- FTS : `food_fts` (`@Fts4`, contentEntity=food, `name, brand`, tokenizer `unicode61 remove_diacritics`) pour recherche hors ligne. **[non implémenté : recherche en `LIKE`, voir M4]**
- [D] Pas de colonnes micronutriments en v1.

**`food_portion`**
| id PK | food_id FK→food CASCADE | label TEXT | quantity_base REAL (`>0`, en g ou ml) | is_default INTEGER(0/1) |
- Index `food_id`.

**`recipe`**
| id PK | name TEXT | servings REAL (`>0`) | cooked_weight_g REAL nullable | note TEXT | archived_at | created_at, updated_at |

**`recipe_ingredient`**
| id PK | recipe_id FK→recipe CASCADE | food_id FK→food **RESTRICT** | quantity_base REAL (`>0`) | position INTEGER |
- Index `recipe_id`, `food_id`. [D] Pas de recette imbriquée en v1.

**`journal_entry`**
| Champ | Type | Contraintes |
|---|---|---|
| id | INTEGER PK | — |
| local_date | INTEGER (epochDay) | NOT NULL |
| meal_slot_id | INTEGER FK→meal_slot RESTRICT | — |
| food_id | INTEGER FK→food RESTRICT | nullable |
| recipe_id | INTEGER FK→recipe RESTRICT | nullable |
| portion_id | INTEGER FK→food_portion SET NULL | nullable |
| portion_count | REAL | nullable |
| quantity_base | REAL | `>0`, g/ml résolus (vérité) |
| energy_kcal, protein_g, carbs_g, fat_g | REAL | **snapshot** calculé à la saisie |
| logged_at, zone_id | INTEGER, TEXT | — |
| created_at, updated_at | INTEGER | — |
- `CHECK((food_id IS NULL) <> (recipe_id IS NULL))`.
- Index : `(local_date, meal_slot_id)`, `food_id`, `recipe_id`.
- [D] Snapshot nutriments + archivage : modifier un aliment ne réécrit jamais l'historique ; totaux du jour = `SUM` sur un index.
- [F] Waistline : journal = référence + portion + nb portions → repris + snapshot.

**`measurement_type`**
| id PK | key TEXT UNIQUE (`builtin.weight`, `builtin.waist`, `builtin.body_fat`, `custom.<uuid>`) | name TEXT nullable (custom) | dimension TEXT (`MASS/LENGTH/PERCENT/ENERGY/COUNT`) | display_unit TEXT nullable (override) | is_enabled INTEGER | position INTEGER | archived_at |

**`measurement`**
| Champ | Type | Contraintes |
|---|---|---|
| id | INTEGER PK | — |
| type_id | INTEGER FK→measurement_type RESTRICT | — |
| value_si | REAL | `CHECK(value_si >= 0)` (+ bornes par dimension dans use case) |
| measured_at, zone_id | INTEGER, TEXT | — |
| group_id | INTEGER | nullable (même pesée BLE multi-valeurs) |
| source | TEXT | `MANUAL/BLE/IMPORT` |
| note | TEXT | nullable |
| created_at, updated_at | INTEGER | — |
- Index : `(type_id, measured_at DESC)` (couvre courbe poids + `weightAt`), `group_id`.
- Unique import : `UNIQUE(type_id, measured_at, source)` → dédoublonnage CSV/BLE.

**`activity_session`**
| Champ | Type | Contraintes |
|---|---|---|
| id | INTEGER PK | — |
| uuid | TEXT UNIQUE | — |
| type | TEXT | `WALK/RUN/BIKE/HIKE` |
| status | TEXT | `RECORDING/PAUSED/FINISHED` |
| source | TEXT | `RECORDED/GPX_IMPORT/MANUAL` |
| started_at, ended_at, zone_id | INTEGER, INTEGER?, TEXT | — |
| elapsed_ms, moving_ms | INTEGER | — |
| distance_m, elevation_gain_m, elevation_loss_m, max_speed_mps | REAL | mis à jour à chaque flush |
| energy_kcal | REAL | nullable |
| energy_method | TEXT | ex. `MET_v1` (traçabilité formule) |
| name, note | TEXT | nullable |
| created_at, updated_at | INTEGER | — |
- Index : `started_at DESC`, `status` (détection session orpheline au démarrage).
- [D] Pas de colonne poids : `energy_kcal` calculé à la clôture via `weightAt(started_at)`.

**`track_point`**
| Champ | Type | Contraintes |
|---|---|---|
| session_id | INTEGER FK→activity_session CASCADE | PK composite |
| seq | INTEGER | PK composite `(session_id, seq)` |
| segment | INTEGER | pause / perte signal → +1 |
| time_ms | INTEGER | — |
| lat, lon | REAL | WGS84 degrés |
| alt_m | REAL | nullable |
| alt_ref | TEXT | `ELLIPSOID/MSL/BARO` |
| h_acc_m, v_acc_m | REAL | v_acc nullable |
| speed_mps, bearing_deg | REAL | nullable |
- Pas d'index supplémentaire (PK couvre lecture séquentielle).

**`app_setting`**
| key TEXT PK | value TEXT | updated_at INTEGER |
- Clés : `unit.mass`, `unit.length`, `unit.distance`, `unit.energy`, `gps.min_accuracy_m`, `off.enabled`, `off.lang`…

**`off_product_cache`**
| Champ | Type | Contraintes |
|---|---|---|
| barcode | TEXT PK | — |
| status | TEXT | `FOUND/NOT_FOUND` |
| payload_json | TEXT | nullable (champs filtrés) |
| etag, last_modified | TEXT | nullable |
| fetched_at, expires_at | INTEGER | — |
- Index `expires_at` (purge). [D] Exclu du backup (régénérable).

### 4.3 Relations

```
profile (1)                     goal (N, historisé)
meal_slot 1──N journal_entry N──1 food 1──N food_portion
                     │  └──N──1 recipe 1──N recipe_ingredient N──1 food
                     └── portion_id → food_portion (SET NULL)
measurement_type 1──N measurement
activity_session 1──N track_point
food ··(copie)·· off_product_cache        (aucune FK : cache jetable)
app_setting (k/v)
```

| Lecture transverse | Mécanisme [D] |
|---|---|
| Sport → poids (calories) | use case `:domain` `GetWeightAt(instant)` → `BodyRepository.latestValue(builtin.weight, ≤ instant)` ; fallback : 1re pesée postérieure, sinon kcal = null + invitation à saisir un poids |
| Nutrition → objectifs | use case `GetDayTargets(localDate)` → `GoalRepository` ; si `mode=COMPUTED` : TDEE depuis `profile` + `GetWeightAt(fin du jour)` |
| Nutrition → dépense sport (bilan du jour) | `ActivityRepository.energyFor(localDate)` via `:domain` |

**Source unique du poids [D]**
- Le poids n'existe **que** dans `measurement` (`type=builtin.weight`), propriété du module `:body`.
- `profile`, `activity_session`, `goal` n'en stockent aucune copie ; tout consommateur passe par `GetWeightAt`.
- Conséquence : corriger une pesée met à jour TDEE/objectifs calculés ; les kcal de sessions déjà clôturées restent figées (snapshot du résultat, pas du poids).

### 4.4 Mesures corporelles : EAV vs colonnes

| Option | Verdict |
|---|---|
| Colonnes fixes (`weight, waist, hips…`) | rejeté : chaque mesure perso = migration ; lignes creuses |
| EAV complet openScale (Measurement + MeasurementValue multi-colonnes typées) | rejeté : [F] risque requêtes coûteuses relevé à l'audit ; jointure systématique |
| **[D] Format long typé** : 1 ligne = 1 valeur numérique (`measurement(type_id, value_si, measured_at)`) + catalogue `measurement_type` + `group_id` optionnel | **retenu** : courbe d'un type = 1 range scan d'index ; mesures perso sans migration ; valeurs toutes numériques (texte/date exclus) |

### 4.5 Migrations Room

| Règle | Détail |
|---|---|
| Schéma exporté | plugin Gradle `androidx.room` (`schemaDirectory("$projectDir/schemas")`), JSON versionnés dans git [F openScale fait de même via KSP arg] |
| Version 1 | gelée à la release MVP ; avant release, la v1 peut être réécrite |
| Migrations | `AutoMigration` si possible (+ `AutoMigrationSpec`), sinon `Migration` manuelle SQL |
| Tests | `MigrationTestHelper` : chaque N→N+1 + 1→dernière ; test de données (insérer en vN, lire en vN+1) [F openScale : migrations 1→18 testées] |
| Fallback destructif | **interdit** : aucun `fallbackToDestructiveMigration*` ; check CI (grep/lint) |
| Échec migration | crash contrôlé → écran « exporter la base brute » (copie fichier SAF) pour ne rien perdre |
| Export/import | `formatVersion` du backup **indépendant** de la version Room ; importeurs `vN→courant` testés avec des fixtures ZIP figées dans `src/test/resources` |

### 4.6 Volumétrie GPS

| Paramètre | Valeur | Statut |
|---|---|---|
| Fréquence | 1 Hz max | [D] |
| Points/h course/vélo | ≤ 3 600 brut ; après filtre distance 10 m : course ~10 km/h ≈ 1 000, vélo 25 km/h ≈ 2 500 | [H] calcul théorique |
| Points/h marche (filtre 5 m, 5 km/h) | ≈ 1 000 | [H] |
| Taille ligne + PK | ~80–100 octets | [H] estimation SQLite |
| Stockage | ≈ 0,1–0,4 Mo/h → 260 h/an ≈ 25–100 Mo/an | [H] |
| Écriture | batch transactionnel (≤ 10 s / 20 points), jamais 1 transaction/point | [D] |
| Lecture | carte/graphes : chargement par session via PK ; décimation Douglas-Peucker en mémoire pour affichage | [D] |
| **Room vs fichier** | **[D] Room** : backup unique, atomicité avec le résumé de session, suppression CASCADE, pas de fichiers orphelins ; fichier GPX = format d'export seulement | — |

---

## 5. Plan de développement

### 5.1 Vue par phase

| | MVP | Sport | Avancé |
|---|---|---|---|
| **Objectif** | archi + journal nutrition + poids + recherche/scan OFF + backup/export/import | enregistrement GPS fiable écran éteint + historique + GPX | recettes enrichies + mesures corporelles complètes + BLE balances + backup auto |
| **Modules** | `:build-logic`, `:core`, `:domain`, `:data`, `:core-ui`, `:nutrition`, `:body` (poids), `:app` | `:activity` (+ `:data` entités sport, GPX) | `:body` (mesures, BLE), `:nutrition` (recettes), `:data` (WorkManager backup) |
| **Deps ajoutées** | Compose/M3, Room, Hilt, Navigation, OkHttp, kotlinx.serialization, CameraX, zxing-cpp, Vico, WorkManager | (aucune lib) ; `core-location-altitude` si validé | blessed-kotlin |
| **Critères d'acceptation** | (1) export ZIP → désinstall → import : **100 %** des lignes identiques (test round-trip égalité) ; (2) scan EAN-13 produit déjà vu **en mode avion** → aliment ajouté au journal ; (3) toute requête OFF porte l'UA (test MockWebServer) ; (4) manifeste release = liste blanche {INTERNET, CAMERA} ; (5) 0 crash sur parcours complet (saisie poids, ajout repas, recherche, scan, export) ; (6) attribution ODbL visible sur toute fiche OFF ; (7) FR+EN sans chaîne en dur (lint `HardcodedText` = 0) ; (8) build F-Droid-like (sans GMS) passe | (1) 60 min écran éteint sur 2 appareils test : aucun trou > 30 s hors perte signal réelle ; (2) distance sur parcours de référence mesuré : écart **≤ 2 %** ; (3) `adb shell am kill` / redémarrage pendant RECORDING : perte ≤ 10 s, session reprise ou clôturable ; (4) GPX exporté valide XSD GPX 1.1 et réimport → mêmes nb points/timestamps/distance (±0,1 %) ; (5) consommation batterie mesurée et consignée (seuil à fixer après 1re mesure) ; (6) aucune permission `BACKGROUND_LOCATION` | (1) pesée BLE sur ≥ 1 balance de test → mesures groupées (`group_id`) ; (2) kcal/portion recette = somme ingrédients (test unitaire) ; (3) backup auto exécuté selon planning (test WorkManager) ; (4) migrations vMVP→vAvancé testées |
| **Tests unitaires (JVM)** | conversions unités, EAN checksum, calcul totaux jour, mapping OFF (fixtures JSON réelles), TTL cache, importeurs backup, CSV | filtres GPS (traces rejouées), distance haversine, hystérésis dénivelé, auto-pause, calcul MET/kcal, parse/écriture GPX | parsers BLE (trames capturées), calcul recettes |
| **Tests instrumentés** | DAO + migrations (`MigrationTestHelper`), round-trip backup, navigation Compose (UI tests clés) | service FGS (start/stop/reprise), DAO track_point en volume (100 k points) | flux BLE simulé, Worker backup |
| **Tests manuels appareil** | scan en lumière faible/codes abîmés, hors ligne, rotation, TalkBack | marche/course/vélo réels, tunnel/forêt, OEM agressif (Xiaomi/Samsung), Doze | balances réelles |
| **Risques → mitigation** | zxing-cpp refusé F-Droid → plan B zxing-core derrière `BarcodeDecoder` ; API OFF instable → `FoodRemoteSource` + fixtures ; schéma v1 mal pensé → v1 gelée seulement à la release | OEM tuent le FGS → wake lock + guide batterie + reprise ; GPS bruité → filtres paramétrables + traces de référence rejouables ; restart FGS Android 14+ → détection session orpheline au lancement | protocoles BLE propriétaires → reprendre logique openScale (GPLv3 compatible) handler par handler ; perms BLE → `neverForLocation` |

### 5.2 Jalons MVP (ordonnés)

| # | Jalon | Livrable | Dépend de |
|---|---|---|---|
| 1 | Socle | multi-module + `:build-logic` + version catalog + CI (build, lint, tests, assertion graphe modules, liste blanche permissions, check anti-`fallbackToDestructive`) + Hilt + thème `:core-ui` + NavHost 4 onglets vides | — |
| 2 | Base de données v0 | entités MVP (profile, goal, meal_slot, food, food_portion, recipe, recipe_ingredient, journal_entry, measurement_type, measurement, app_setting, off_product_cache) + schéma exporté + `:core` unités + réglages unités | 1 |
| 3 | Poids | saisie/édition/suppression, historique, graphe Vico, `GetWeightAt`, unités kg/lb/st | 2 |
| 4 | Journal nutrition | aliments custom + portions, créneaux, journal jour, totaux kcal/macros vs `goal`, recettes simples (ingrédients → kcal/portion), archivage, FTS local | 2 (objectifs calculés : 3) |
| 5 | OFF en ligne | client OkHttp + UA + recherche + produit par code + cache Room TTL + copie vers `food` + attribution ODbL + purge WorkManager | 4 |
| 6 | Scan **[fait 2026-10-11, non testé sur appareil]** | CameraX + zxing-cpp + validation EAN + chaîne local→cache→réseau→manuel + permission CAMERA à la demande | 5 |
| 7 | Données **[fait 2026-10-11]** | backup/restore ZIP JSONL versionné + export CSV (poids, journal) + tests round-trip | 2–6 (schéma stable) |
| 8 | Durcissement release **[partiel 2026-10-11, voir M16]** | FR/EN complet, accessibilité, R8, build reproductible, métadonnées fastlane F-Droid, gel schéma Room v1, écran À propos/licences | 1–7 |

---


---

## 6. Risques, incertitudes, décisions restantes

### 6.1 Risques

| # | Risque | Phase | Impact | Probabilité | Mitigation |
|---|---|---|---|---|---|
| R1 | zxing-cpp (AAR + `.so` précompilé) refusé par F-Droid | MVP | Élevé | Moyenne | Interface `BarcodeDecoder` ; plan B `com.google.zxing:core` ; ou build NDK depuis source |
| R2 | `core-location-altitude` tire Play Services | Sport | Moyen | Moyenne | Vérifier arbre de deps ; sinon stocker altitude ellipsoïde + flag, conversion MSL maison |
| R3 | OEM tuent le service GPS écran éteint | Sport | Élevé | Haute | FGS `location`, wake lock partiel, session persistée, reprise/clôture au lancement, guide batterie |
| R4 | Données GPS bruitées (dénivelé, distance) | Sport | Moyen | Haute | Filtres paramétrables, hystérésis, traces de référence rejouables |
| R5 | Conformité ODbL insuffisante (attribution, cache, export) | MVP | Élevé (juridique) | Moyenne | Composant `OffAttribution`, mention dans exports, validation juridique J1/J2 |
| R6 | API OFF instable / quotas / Search-a-licious change | MVP | Moyen | Moyenne | `FoodRemoteSource`, debounce, backoff 429, fixtures JSON réelles, repli `cgi/search.pl` |
| R7 | Schéma Room v1 mal pensé | MVP | Moyen | Moyenne | v1 gelée seulement à la release ; migrations testées dès v2 ; pas de fallback destructif |
| R8 | Perte de données utilisateur (migration, restore) | Toutes | Élevé | Faible | Backup auto de sécurité avant restore ; tests round-trip ; écran export brut en cas d'échec |
| R9 | Formules énergie mal sourcées (PAL non cité dans ONT) | MVP | Moyen (santé) | Moyenne | Citer littérature (IOM 2005, Mifflin-St Jeor), afficher « estimation », valeurs modifiables |
| R10 | Données de santé (RGPD) | Toutes | Moyen | Faible | 0 réseau hors OFF (requête = code-barres seul), stockage local, pas de backup cloud Android (`allowBackup=false`) |
| R11 | Protocoles BLE propriétaires, peu documentés | Avancé | Moyen | Haute | Cibler 1-2 balances testables ; `blessed-kotlin` ; handlers un par un |
| R12 | Portée trop large pour un dev seul | Toutes | Élevé | Haute | Jalons ordonnés, MVP strict, une feature à la fois |
| R13 | FTS local fragile (contenu externe non auto-synchronisé) | MVP | Faible | Moyenne | Tester triggers FTS4 en jalon 4 ; repli `LIKE` + index (déjà rencontré sur l'ancien projet, note mémoire) |
| R14 | `INTERNET` rend l'app « avec réseau » : perception privacy | MVP | Faible | Faible | Réseau OFF désactivable (`off.enabled`), requêtes minimales, doc claire |

### 6.2 Incertitudes (non vérifiées)

| # | Point | Vérification |
|---|---|---|
| U1 | Version/API exacte `io.github.zxing-cpp:android`, taille `.so`/ABI, alignement 16 Ko | Maven Central, APK de test, `apkanalyzer` |
| U2 | Politique F-Droid pour AAR natif précompilé | Apps F-Droid existantes (scanners), revue de demande d'inclusion |
| U3 | Dépendances GMS de `core-location-altitude`, `desugar_jdk_libs` | `./gradlew :activity:dependencies`, POM |
| U4 | Redémarrage FGS `location` `START_STICKY` sur Android 14-16 | Test appareil (`am kill`, Doze) |
| U5 | OFF : ETag/Last-Modified, quotas, stabilité Search-a-licious | `curl -I` avec UA, documentation OFF |
| U6 | Seuils GPS (précision, distance, hystérésis, auto-pause) | Rejeu de traces réelles |
| U7 | Volumétrie `track_point` réelle | Test instrumenté 100 k points |
| U8 | Licences exactes des dépendances (BC, desugar, AndroidX) non relues dans les dépôts | POM / `CREDITS` |
| U9 | Activité réelle des projets (clones superficiels, 1 commit) | `git fetch --unshallow` ou plateforme d'hébergement |
| U10 | Nombre réel de balances BLE supportées par openScale | Générateur `ScaleCatalog` (wiki) |
| U11 | Détail `AltitudeChangeHandler` OpenTracks et exporteurs CSV/KML | Lecture ciblée avant implémentation GPS |
| U12 | Comportement du User-Agent OFF vs requêtes OkHttp (UA custom accepté) | Test réseau |
| U13 | Formules Mifflin-St Jeor exactes de Waistline (coefficients) | Non nécessaire si réimplémenté depuis la littérature |

### 6.3 Limites de l'audit

| Limite | Détail |
|---|---|
| Profondeur | Lecture ciblée par un sous-agent Haiku par dépôt ; pas de lecture exhaustive |
| Historique | `--depth 1` : maintenance estimée par date du dernier commit seulement |
| Exécution | Rien n'a été compilé ni exécuté |
| Hors périmètre | `Archive/` ignoré à la demande ; saisie IA, Supabase, Sentry, Health Connect d'ONT/OpenTracks non retenus |
| Juridique | Aucun avis juridique : section 2.5 à faire valider |

### 6.4 Décisions restantes (à valider par l'utilisateur)

| # | Décision | Recommandation | Impact si différent |
|---|---|---|---|
| D1 | Licence du projet | GPLv3-or-later | Apache/MIT : interdit de reprendre du code GPLv3 (openScale, Waistline, ONT) |
| D2 | minSdk | 26 | 29+ : simplifie stockage/permissions, réduit la couverture |
| D3 | Carte tuilée en v1 | Non (tracé Canvas) | Oui : `INTERNET` côté sport, attribution OSM, module carte |
| D4 | Énergie stockée en kcal | Oui (kJ à l'affichage) | Tout-SI : conversions OFF partout |
| D5 | Recettes simples en MVP, enrichies en Avancé | Oui | Reporter : MVP plus court |
| D6 | Images produits OFF | Non en v1 | Oui : CC BY-SA, cache disque, bande passante |
| D7 | Backup logique ZIP/JSONL | Oui | Copie brute DB : plus simple, couplé à la version Room/WAL |
| D8 | Hilt comme DI | Oui | Koin / manuel : moins de compile-time safety |
| D9 | User-Agent OFF : nom + URL du dépôt | Oui, jamais d'email | — |
| D10 | Langue source EN + FR complète | Oui | FR seul : plus rapide, moins ouvert |
| D11 | Nom de l'app / `applicationId` / URL dépôt public | À fournir | Bloque UA OFF, manifeste, F-Droid |
| D12 | Rôle de `Archive/` (ancienne implémentation) : repartir de zéro ou récupérer des parties | À trancher | Réutilisation = autre plan, hors périmètre de cet audit |
| D13 | Structure du dépôt : conserver `Poids/` + `audit/` + `docs/` ; ajouter `audit/` au `.gitignore` | Oui | Dépôts tiers dans l'historique git |

---

**STOP.** Audit terminé. 0 code, 0 modification de projet existant. En attente de validation explicite.

---

## 7. Mises à jour (après jalon 5)

| # | Sujet | Constat vérifié (2026-10-11) | Source | Impact |
|---|---|---|---|---|
| M1 | Limites OFF | 15 req/min/IP (produit), 10 req/min/IP (recherche) ; « ne pas utiliser pour la recherche au fil de la frappe » ; HTTP 503 si limites globales dépassées | openfoodfacts.github.io/openfoodfacts-server/api/ (Rate limits) | Corrige §3.4 (≈100 req/min produit, supposé). Recherche = soumission explicite + debounce 400 ms ; limiteur local 12/min (produit) et 8/min (recherche) |
| M2 | ETag / Last-Modified | Absents des en-têtes de `api/v2/product` | requête réelle avec User-Agent | Pas de requête conditionnelle : TTL seul (30 j trouvé, 7 j absent) |
| M3 | Forme des réponses | Produit inconnu = HTTP 200 + `status: 0` ; Search-a-licious : `hits[]`, `brands` en liste, nutriments partiels | requêtes réelles | Parsing tolérant ; produit sans nom ou sans énergie ignoré |
| M4 | FTS4 vs LIKE | `LIKE '%q%'` : 4 ms à 5 000 lignes, 20 ms à 50 000 (Robolectric, Room en mémoire) ; la table `food` ne contient que les aliments saisis ou copiés par l'utilisateur ; le cache OFF est lu par clé primaire | `FoodSearchBenchmarkTest` | **LIKE conservé** ; FTS4 à reconsidérer au-delà de ~100 000 aliments |
| M5 | Purge du cache | Purge des entrées expirées à chaque écriture du cache (pas de WorkManager) | `CachedOffRemoteSource` | Évite `RECEIVE_BOOT_COMPLETED` et une dépendance ; écart assumé avec §3.4 |
| M6 | `quantity_base` | `journal_entry.quantity_unit` (G / ML / SERVING) ajouté ; schéma v1 réécrit (non publié) | commit « Fix : unité explicite… » | R13 / ambiguïté levée |
| M7 | Écritures | `SaveWeight`, `SaveFood`, `SaveRecipe` : chemin unique validé ; les repositories ne valident pas. Import/restauration (jalon 7) devront les utiliser | `domain/` | Point à respecter au jalon 7 |
| M8 | Archivage / ré-import (F3, F4) | `FoodRepository.save` applique `Food.isArchived` ; `unarchive` supprimé. Le ré-import d'un archivé écrase ce qu'OFF fournit (nom, marque, macros, détail, date) et garde id, unité, densité, portions | `ImportRemoteFood` | Une seule écriture : atomique |
| M9 | Détail nutritionnel (F6, F7) | Somme stricte : inconnu si un terme est inconnu ; recettes : détail calculé depuis les ingrédients ; OFF : `sugars/saturated-fat/fiber/salt_100g` lus | `NutrientDetail`, `Recipe.perServingDetail`, `OffApi` | — |
| M10 | Migration v2 non publiée (F8, F10) | Pas de backfill déduit du détail (NULL = inconnu) ; code-barres normalisés GTIN-13 avant dédup. Un appareil de dev déjà en v2 doit être réinstallé | `Migrations.kt`, `Gtin.normalize` | Aucune release distribuée (`git tag` vide) |
| M11 | Annulation journal (F9) | Use case `RestoreJournalEntry` : portion disparue → `portion_id = NULL` | `LogUseCases.kt` | Plus de violation de FK |
| M12 | CIQUAL embarqué (décision Opus 5.5) | Onglet « Ciqual » (recherche locale, ≥ 2 lettres, sans réseau), table `ciqual_food` rechargée à chaque changement de `ciqual.version`, hors sauvegarde ; v3 = `MIGRATION_2_3` | `CiqualRepositoryImpl`, `ImportCiqualFood` | Jalon 7 : exclure `ciqual_food`, lister les attributions |
| M13 | Restauration (jalon 7, exception documentée à M7) | Chemin dédié `BackupRepositoryImpl` : conserve ids, archivés, timestamps, snapshot du journal ; mêmes règles de valeur (`FoodValidation`, `WeightValidation`, XOR journal, FK, GTIN-13, finis ≥ 0). Passe 1 lecture seule (sha256, compteurs, lignes), sauvegarde de sécurité `filesDir/backup/pre-restore.zip`, passe 2 : une transaction (`BackupDao.replaceAll`). Exclus : `off_product_cache`, `ciqual_food`, `ciqual.version` | `data/.../backup/` | Rejet = base inchangée (testé) |
| M14 | Export CSV v1 | RFC 4180, UTF-8 sans BOM, CRLF, version dans le nom de fichier, injection de formules neutralisée sur les colonnes texte, `source` (USER/OFF/CIQUAL/RECIPE) pour l'attribution. Excel FR peut demander l'import manuel (séparateur `,`) | `CsvExporterImpl` | Limite assumée |
| M15 | Scan (jalon 6, décision Opus 5.5) | `io.github.zxing-cpp:android:3.1.1` (Apache-2.0, .so arm64/v7a/x86/x86_64, segments alignés 16 Ko vérifiés `readelf`) + CameraX 1.6.2 ; 0 dépendance `gms/firebase/mlkit` dans `releaseRuntimeClasspath`. EAN-13, EAN-8, UPC-A ; UPC-E exclu. `ScanStabilizer` : 2 trames identiques + clé GTIN. Permission CAMERA demandée à l'ouverture de l'écran de scan, `uses-feature camera` en `required=false` (vérifié par `checkPermissions`). Issues sans quantité : saisie manuelle avec le code | `nutrition/.../scan/` | **Non vérifié sur appareil** (mise au point, torche, faible lumière) ; acceptation F-Droid du .so précompilé = hypothèse, plan B `com.google.zxing:core` derrière `BarcodeDecoder` |
| M16 | Jalon 8 (décision Opus 5.5) | **Fait** : lint strict `warningsAsErrors` sur `:app` (a révélé 6 défauts corrigés : ressources non configuration-aware, pluriels FR/EN, `dataExtractionRules`/`fullBackupContent` sans sauvegarde cloud, icône monochrome) ; `checkStrings` (0 texte en dur, parité des clés FR/EN) ; `checkLicenses` + `licenses.json` + section À propos ; `checkLicenseHeaders` (SPDX GPL-3.0-or-later, 132 fichiers) ; icônes Material retirées (`TallyIcons`) ; locale par app (`generateLocaleConfig`) ; test de chaîne des migrations 1→2→3 + schémas exportés (v3 = schéma de la 1re release) ; `assembleRelease` sans règles R8 manquantes, APK non signé **identique** après `clean` (même répertoire) ; `distributionSha256Sum` ; fastlane en-US/fr-FR (`tools/check-fastlane.sh`) | `build.gradle.kts`, `app/`, `fastlane/` | **Non fait / impossible ici** : `verification-metadata.xml` (dépendances), reproductibilité entre deux clones/chemins, icône 512×512 et captures finales fastlane (provisoires = captures de test), tests ATF d'accessibilité automatiques, signature, tag `v0.1.0`, validation F-Droid (.so zxing-cpp = hypothèse), choix « only/or-later » (posé : or-later) |

## 8. Données de référence et licences (jalon 2, 2026-10-11)

### 8.1 CIQUAL : version retenue

| # | Constat | Source | Statut |
|---|---|---|---|
| C1 | **CIQUAL 2020** retenu : jeu de données Anses sur data.gouv.fr, licence `fr-lo` (Licence Ouverte), métadonnées modifiées le 2025-11-20 | `data.gouv.fr/api/1/datasets/table-de-composition-nutritionnelle-des-aliments-ciqual/` | F (métadonnée API) |
| C2 | Fichier xls FR téléchargé depuis le site Anses (HTTP 200, 3 639 808 o, sha256 `a728c29d…5da75d`), **non embarqué**, **non versionné** | `ciqual.anses.fr/cms/sites/default/files/inline-files/Table Ciqual 2020_FR_2020 07 07.xls` | F |
| C3 | La page d'accueil `ciqual.anses.fr` est une SPA Angular : la mention de licence n'y a pas été lue en clair | curl | H |
| C4 | L'API data.gouv distingue `fr-lo` (« Licence Ouverte / Open Licence », PDF Etalab 2014 = **LO 1.0**) de `lov2` (« version 2.0 ») : CIQUAL 2020 = **LO 1.0** (déduction forte, API `datasets/licenses/`, audit Opus 2026-10-11) | `data.gouv.fr/api/1/datasets/licenses/` | F (déduction) |
| C5 | **CIQUAL 2025** : un seul jeu (2020) existe sur data.gouv ; **passage à 2025 conditionné à la vérification de sa licence sur le site Anses** (page de téléchargement ou mentions légales du fichier) | recherche data.gouv `q=ciqual` | À faire |
| C7 | **Embarqué** le 2026-10-11 : `tools/ciqual/convert.py` → CSV 203 Ko, 2 298 aliments sur 3 185 (887 exclus : énergie 328 et 333 à « - », rien n'est calculé). Table Room séparée `ciqual_food` (v3), copie dans `food` (`source=CIQUAL`, `ciqual_code` unique) à l'usage. Attribution dans l'onglet Ciqual et `NOTICE.md` | commit jalon CIQUAL | F |
| C6 | Obligation LO : mentionner la source (Anses, Table Ciqual 2020) et la date de dernière mise à jour des données → voir `NOTICE.md` | Licence Ouverte | F |

### 8.2 ODbL (OFF) ↔ GPLv3

| # | Analyse (lecture des textes, **pas un avis juridique**) | Statut |
|---|---|---|
| O1 | La GPLv3 régit le **code** ; l'ODbL régit la **base de données**. Ce sont deux objets distincts : le code GPLv3 n'est pas une « base dérivée », la base ODbL n'est pas un programme. L'app les distribue côte à côte (agrégat) sans relicencier l'un sous l'autre | Raisonnement, H |
| O2 | L'ODbL n'est pas une licence logicielle : la FSF ne la liste pas parmi les licences « compatibles GPL », ce qui est sans objet tant que la base n'est pas fusionnée au code | H |
| O3 | Obligations ODbL qui restent pour la **donnée** : attribution, mention de la licence, partage à l'identique d'une base dérivée rendue publique, maintien de l'ouverture | F (texte ODbL) |
| O4 | Reste à vérifier : la table `food` (aliments copiés d'OFF) + l'export utilisateur sont-ils une « base dérivée » ? (= J1 de §2.5) ; contenu = DbCL, images = CC BY-SA (J2, aucune image stockée à ce jour) | H |
| O5 | Reste à vérifier : mélange CIQUAL (LO) et OFF (ODbL) dans la même table `food` : la LO n'impose que l'attribution, donc compatible en principe avec l'ODbL, mais l'Etalab ne cite pas explicitement l'ODbL dans sa liste de licences compatibles | H |
| O6 | Reste à vérifier : acceptation F-Droid (données ODbL téléchargées à l'exécution, service libre → pas d'anti-feature `NonFreeNet` attendue) | H |

**Verdict :** compatibilité code GPLv3 ↔ données ODbL **plausible mais non confirmée** ; points ouverts O4, O5, O6 + J1, J2. Aucun blocage technique.
