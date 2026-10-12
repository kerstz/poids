# SPORT-ARCHI — Suivi marche / course (GPS natif)

Décision d'architecture (Opus, 2026-10-12). Complète `docs/AUDIT.md` §3.4, §3.6, §4.2, §4.6, §5.1. En cas d'écart, **ce fichier prime** pour le sport.
Légende : [F] fait vérifié dans le code/doc · [H] hypothèse à vérifier · [D] décision.

## 0. Sources consultées

| Fichier | Utilisé pour |
|---|---|
| `docs/AUDIT.md` §3.1–3.6, §4.1–4.6, §5.1, §6, §7 (M7, M13, M16) | modèle prévu, permissions, critères Sport |
| `docs/UI.md` (nav, états vides, tokens) | règles écrans |
| `activity/build.gradle.kts`, `activity/src/main/kotlin/org/tally/activity/ActivityGraph.kt` | état placeholder |
| `build-logic/src/main/kotlin/Catalog.kt`, `tally.android.feature.gradle.kts` | minSdk 26, target/compile 37, deps feature |
| `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts` (`checkPermissions`), `config/permissions-allowlist.txt` | garde-fou permissions |
| `data/src/main/kotlin/org/tally/data/backup/BackupRepositoryImpl.kt`, `backup/v1/Dtos.kt` | format sauvegarde |
| `data/src/main/kotlin/org/tally/data/db/{TallyDatabase,Migrations}.kt`, `data/schemas/` | Room v3, style migrations |
| `domain/src/main/kotlin/org/tally/domain/body/GetWeightAt.kt` | poids pour kcal |
| `core/src/main/kotlin/org/tally/core/unit/Units.kt`, `core/time/AppClock.kt` | `DistanceUnit`, horloge |
| `nutrition/src/main/kotlin/org/tally/nutrition/scan/ScanScreen.kt` | modèle demande de permission (permanent → réglages) |
| `build.gradle.kts` (racine) | `checkModuleGraph`, `checkNoDestructiveMigration`, `checkDesignTokens`, `checkStrings`, `checkArchitecture` |

## 1. Faits vérifiés (base de la décision)

| # | Fait |
|---|---|
| F1 | `:activity` = placeholder (1 fichier Kotlin + strings), plugin `tally.android.feature` (Hilt, Compose, nav, serialization), pas de manifeste |
| F2 | minSdk 26, targetSdk 37 → règles FGS Android 14+ s'appliquent |
| F3 | Manifeste app : `INTERNET` seul ; allowlist : INTERNET, CAMERA, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |
| F4 | `checkPermissions` vérifie aussi `uses-feature camera required=false` (regex sur manifeste fusionné release) |
| F5 | Room v3, migrations SQL manuelles dans `Migrations.kt`, schéma v3 = 1re release (M16) mais **aucun tag publié** ; aucune contrainte `CHECK` en base (validation dans le domaine) |
| F6 | Backup : `FORMAT_VERSION=1`, `TABLES` figé 11 tables ; `readAndValidate` exige ensemble exact des entrées ZIP **et** `manifest.tables.keys == TABLES` → ajouter une table sans adaptation = rejet des sauvegardes v1 |
| F7 | `Dtos.kt` annonce : « toute évolution passe par `formatVersion` 2 et un importeur v1 → v2 » |
| F8 | `GetWeightAt(instant)` existe : dernière pesée ≤ instant, sinon première après, sinon null |
| F9 | `DistanceUnit` existe dans `:core` |
| F10 | Aucune référence sport dans `:domain`/`:data` (hors `ActivityLevel` du profil) |

## 2. Modèle de données [D]

### 2.1 Choix structurants

| Question | Décision | Pourquoi |
|---|---|---|
| Points : table vs polyline compressée | **Table `track_point`** (1 ligne/point gardé) | atomicité flush+résumé, CASCADE, backup JSONL natif, GPX exact (time/ele) ; polyline perd temps/altitude |
| Échantillonnage | 1 Hz demandé, **seuls les points filtrés** stockés (§3.3) | ≈ 1 000 pts/h → 0,1 Mo/h ; distance recalculable depuis la base = distance affichée |
| Points bruts rejetés | **non stockés** | YAGNI ; seuils ajustés par rejeu de traces de test |
| Types | `WALK`, `RUN` seulement | périmètre demandé ; TEXT sans CHECK → `BIKE/HIKE` ajoutables sans migration |
| Altitude | `Location.altitude` brute (ellipsoïde WGS84), **pas** de `core-location-altitude`, pas de baromètre en v1 | 0 dépendance, R2/U3 évités ; dénivelé = différences → non affecté |
| Segments | `segment` +1 **uniquement** à la reprise après pause | perte signal / kill : ligne droite comptée (approximation acceptable, distance non sous-estimée) |
| Auto-pause | **non** en v1 | seuils non calibrés (U6) ; pause manuelle |

### 2.2 `activity_session`

| Colonne | Type | Null | Note |
|---|---|---|---|
| `id` | INTEGER PK autoinc | non | — |
| `uuid` | TEXT | non | UNIQUE ; nom fichier GPX, dédup futur import |
| `type` | TEXT | non | `WALK` / `RUN` |
| `status` | TEXT | non | `RECORDING` / `PAUSED` / `FINISHED` |
| `source` | TEXT | non | `RECORDED` / `MANUAL` (`GPX_IMPORT` réservé) |
| `started_at` | INTEGER | non | epoch ms UTC |
| `ended_at` | INTEGER | oui | null tant que non FINISHED |
| `zone_id` | TEXT | non | IANA au démarrage |
| `local_date` | INTEGER | non | epochDay de `started_at` dans `zone_id` (même convention que `journal_entry`) |
| `active_since` | INTEGER | oui | instant du dernier démarrage/reprise si RECORDING, sinon null |
| `moving_ms` | INTEGER | non | somme des intervalles fermés ; temps en mouvement courant = `moving_ms + (now − active_since)` |
| `distance_m` | REAL | non | ≥ 0, somme haversine intra-segment |
| `elevation_gain_m`, `elevation_loss_m` | REAL | non | hystérésis §3.4 |
| `energy_kcal` | REAL | oui | null = poids inconnu ou session non finie |
| `energy_method` | TEXT | oui | `ACSM_NET_v1` |
| `note` | TEXT | oui | — |
| `created_at`, `updated_at` | INTEGER | non | — |

- Pas d'`elapsed_ms` : dérivé (`ended_at − started_at`). Pas de `max_speed`, `name` : non affichés (UI épurée).
- Index : `UNIQUE(uuid)`, `(started_at)`, `(status)`, `(local_date)`.
- Pas de colonne poids (source unique `measurement`, AUDIT §4.3).

### 2.3 `track_point`

| Colonne | Type | Null | Note |
|---|---|---|---|
| `session_id` | INTEGER | non | FK → `activity_session.id` ON DELETE CASCADE |
| `seq` | INTEGER | non | PK `(session_id, seq)`, croissant |
| `segment` | INTEGER | non | 0, +1 par reprise |
| `time_ms` | INTEGER | non | `Location.time` (UTC) |
| `lat`, `lon` | REAL | non | WGS84 degrés |
| `alt_m` | REAL | oui | ellipsoïde ; null si `!hasAltitude()` |
| `h_acc_m` | REAL | non | précision horizontale |
| `speed_mps` | REAL | oui | `Location.speed` si présent |

- Retirés vs AUDIT : `alt_ref` (toujours ellipsoïde), `v_acc_m`, `bearing_deg` (inutilisés). Ajout futur = `ALTER TABLE ADD COLUMN`.
- Index : PK seule. [H] Room peut avertir « FK sans index » malgré la PK composite → si warning, ajouter `index(session_id)` (pas d'ignore).

### 2.4 Migration 3 → 4

| Point | Décision |
|---|---|
| Forme | `MIGRATION_3_4` manuelle dans `Migrations.kt` (cohérent avec 1_2, 2_3), SQL copié de `data/schemas/.../4.json` généré |
| Contenu | 2 `CREATE TABLE` + index ; aucune donnée existante touchée |
| Tests | `MigrationTest` : 3→4 avec données v3 conservées + chaîne 1→4 ; `checkNoDestructiveMigration` inchangé |
| Version | `TallyDatabase` v4, `DB_SCHEMA_VERSION=4` dans backup |

### 2.5 Calories [D]

- Formule : **équations métaboliques ACSM, énergie nette** (au-dessus du repos → pas de double comptage avec TDEE).
- `kcal = poids_kg × (c_h × distance_m + c_v × gain_m) × 5 / 1000`
  - WALK : `c_h = 0,1`, `c_v = 1,8` ml O₂/kg/m ; RUN : `c_h = 0,2`, `c_v = 0,9` ; 5 kcal/L O₂ ; descente ignorée.
  - Ordre de grandeur : 70 kg, 5 km plat → marche 175 kcal, course 350 kcal (~1 kcal/kg/km course).
- Pas de MET : MET × durée dépend de la vitesse supposée ; ACSM utilise distance + dénivelé réellement mesurés.
- Poids : `GetWeightAt(started_at)`. Null → `energy_kcal = null`, détail affiche action « Ajouter un poids » (lambda `onOpenWeight` fournie par `:app`).
- Snapshot à la clôture ; `RecomputeMissingEnergy` remplit seulement les sessions FINISHED à `energy_kcal IS NULL` (à l'ouverture de la liste). Corriger un poids ne modifie pas les sessions déjà calculées (AUDIT §4.3).
- [H] Coefficients cités de mémoire (ACSM Guidelines, metabolic equations) → **fact-check source primaire avant S1** ; affichage « estimation ».

## 3. Enregistrement GPS [D]

### 3.1 Choix

| Option | Verdict |
|---|---|
| In-app seulement (écran allumé) | rejeté : écran éteint = arrêt GPS, critère AUDIT (60 min écran éteint) impossible |
| **Foreground service `location`** démarré par action utilisateur | **retenu** ; pas de `ACCESS_BACKGROUND_LOCATION` |
| Bound service | rejeté : cycle bind/unbind dans l'UI, inutile avec Hilt |
| **Singleton `TrackingController` + `StateFlow`** | **retenu** : VM testable avec faux, notif et UI passent par le même chemin |

### 3.2 Composants

| Composant | Module | Rôle |
|---|---|---|
| `ActivitySession`, `TrackPoint`, `ActivityType`, `SessionStatus` | `:domain/activity` | modèles |
| `ActivityRepository` (interface) | `:domain/activity` | `observeSessions()`, `observeSession(id)`, `activeSession()`, `points(id)`, `start(...)`, `appendBatch(id, points, aggregates)` (1 transaction), `pause/resume/finish`, `delete`, `restore` |
| `Geo` (haversine, R = 6 371 008,8 m), `TrackFilter`, `ElevationAccumulator`, `TrackAggregator`, `DouglasPeucker`, `EstimateActivityEnergy`, `Pace` | `:domain/activity` | logique pure JVM, testée |
| Use cases `StartSession`, `PauseSession`, `ResumeSession`, `FinishSession` (agrège + kcal), `DeleteSession`, `RestoreSession`, `RecomputeMissingEnergy` | `:domain/activity` | instanciés via `@Provides` dans `DataProvidersModule` (pattern existant) |
| `ActivitySessionEntity`, `TrackPointEntity`, `ActivityDao`, `ActivityRepositoryImpl` | `:data` | `@Binds` dans `DataBindingsModule` |
| `LocationSource` (interface) + `LocationManagerSource` | `:activity/tracking` | `callbackFlow` sur `LocationManagerCompat.requestLocationUpdates(GPS_PROVIDER, LocationRequestCompat(1000 ms, minDistance 0))` |
| `TrackingService` (`@AndroidEntryPoint`, `foregroundServiceType=location`) | `:activity/tracking` | seul écrivain de l'enregistrement |
| `TrackingController` (interface) + `ServiceTrackingController` `@Singleton` | `:activity/tracking` | `state: StateFlow<LiveTracking>` + commandes → `Intent` au service ; module Hilt dans `:activity` |
| `TrackingNotification` | `:activity/tracking` | canal `tracking` IMPORTANCE_LOW, ongoing |

### 3.3 Flux

1. VM → `controller.start(type)` → `StartSession` (ligne RECORDING, `active_since=now`) → `ContextCompat.startForegroundService`.
2. Service : `startForeground(...)` immédiat (type location) → lit `activeSession()` en DB (source de vérité) → collecte `LocationSource` dans **une** coroutine sérialisée (acteur : positions + commandes PAUSE/RESUME/FINISH).
3. Chaque fix → `TrackFilter` → point gardé → tampon + `StateFlow` mis à jour (distance live, précision, état GPS).
4. Flush : **≤ 10 s ou 20 points** → `appendBatch` (points + `distance_m`, gain/perte, `moving_ms`) en une transaction → perte max ≈ 10 s.
5. PAUSE : flush, `moving_ms += now − active_since`, `active_since=null`, status PAUSED, **désabonnement GPS + libération wake lock**, service reste au premier plan (notif « Reprendre »).
6. RESUME : `segment+1`, `active_since=now`, réabonnement.
7. FINISH (in-app seulement) : flush, `FinishSession` (agrégats recalculés depuis les points stockés, kcal), `stopForeground` + `stopSelf`.

### 3.4 Filtres (constantes `TrackFilterConfig` dans `:domain`, pas de réglage utilisateur v1)

| Règle | WALK | RUN | Statut |
|---|---|---|---|
| Fix accepté si `h_acc ≤` | 25 m | 25 m | [H] calibrer (AUDIT : 50 m OpenTracks, trop lâche à 5 m de pas) |
| Démarrage : ignorer jusqu'au 1er fix `h_acc ≤` | 15 m | 15 m | [H] ; UI « Recherche GPS » |
| Distance min depuis dernier point gardé | 5 m | 10 m | [F AUDIT/OpenTracks] |
| Vitesse implicite max (rejet) | 4 m/s | 9 m/s | [H] |
| Temps non croissant / doublon | rejet | rejet | — |
| Dénivelé : hystérésis | 10 m | 10 m | [H] bruit vertical GPS ±10–20 m, 3–5 m sur-compterait |

- Allure moyenne = `moving_ms / distance` ; allure courante = fenêtre glissante 30 s sur points gardés ; affichée en min/km ou min/mi selon `DistanceUnit` (les deux types).
- Durée affichée = temps en mouvement (pauses exclues).
- Horloge : `AppClock` (mur) pour tout ce qui est persisté (survit au kill) ; [H] changement d'heure système pendant session = durée faussée, accepté.

### 3.5 Reprise après kill [D]

| Cas | Comportement |
|---|---|
| Kill process, service `START_STICKY` relancé (`intent == null`) | lit session RECORDING en DB → `startForeground` dans `try/catch` (`ForegroundServiceStartNotAllowedException`/`SecurityException`) → succès : reprise même segment (trou = ligne droite) ; échec : `stopSelf`, session reste RECORDING en DB |
| Ouverture app avec session RECORDING/PAUSED et service absent | carte « Session interrompue » : **Reprendre** (app visible → FGS autorisé) / **Terminer** |
| Temps pendant le kill | compté en mouvement (l'utilisateur marchait) |
| [H] U4 | relance sticky FGS location Android 14–16 + accès « while-in-use » depuis l'arrière-plan : probablement refusé → le chemin « carte au lancement » est la garantie, testé |

- Wake lock `PARTIAL` tenu seulement en RECORDING, `acquire(timeout 6 h)` ré-armé à chaque flush.
- Notification : `setUsesChronometer` (durée live sans mise à jour 1 Hz), distance rafraîchie au flush, action Pause/Reprendre, tap → `getLaunchIntentForPackage` + action `org.tally.action.OPEN_TRACKING` → `:app` navigue vers l'écran d'enregistrement (`:activity` n'importe pas `MainActivity`).

## 4. Permissions [D]

### 4.1 Liste (manifeste `activity/src/main/AndroidManifest.xml`)

| Permission | Runtime | Raison |
|---|---|---|
| `ACCESS_FINE_LOCATION` | oui | GPS précis |
| `ACCESS_COARSE_LOCATION` | oui | obligatoire avec FINE (Android 12+) |
| `FOREGROUND_SERVICE` | non | FGS |
| `FOREGROUND_SERVICE_LOCATION` | non | type location (Android 14+) |
| `POST_NOTIFICATIONS` | oui API 33+ | notif FGS visible ; **refus non bloquant** |
| `WAKE_LOCK` | non | partiel en RECORDING |

- Refusées : `ACCESS_BACKGROUND_LOCATION`, `ACTIVITY_RECOGNITION`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- `<uses-feature android:name="android.hardware.location.gps" android:required="false"/>`.
- `<service android:name=".tracking.TrackingService" android:exported="false" android:foregroundServiceType="location"/>`.
- `config/permissions-allowlist.txt` : +6 lignes ; `checkPermissions` : étendre la règle `required=false` à `android.hardware.location*`.

### 4.2 Flux de demande (au tap « Démarrer », jamais au lancement)

1. FINE+COARSE ensemble (`RequestMultiplePermissions`).
2. FINE accordée → étape 3. COARSE seule (« approximative ») → **pas d'enregistrement GPS** : message « position précise requise » + bouton réglages (approximative = ~2 km, distance absurde).
3. API 33+ et notifications non accordées → demande une fois ; refus → continue.
4. Refus permanent localisation → état avec action « Ouvrir les réglages » (même schéma que `ScanScreen`).
5. GPS désactivé (`LocationManagerCompat.isLocationEnabled` faux) → action vers `ACTION_LOCATION_SOURCE_SETTINGS`.
- Aide batterie OEM : lien `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` dans Réglages (sans permission), S5.

## 5. Sauvegarde / exports [D]

| Sujet | Décision |
|---|---|
| Format | **`FORMAT_VERSION = 2`** = tables v1 + `activity_session` + `track_point` |
| Lecture | `tablesFor(formatVersion)` : v1 → 11 tables (sport vide), v2 → 13 ; tout autre ensemble = `NOT_A_BACKUP` ; > 2 = `FUTURE_FORMAT` |
| DTOs | `backup/v1/Dtos.kt` **inchangés** ; nouveaux `backup/v2/ActivityDtos.kt` (`ActivitySessionV2`, `TrackPointV2`) ; manifeste même forme |
| Rejet « tables optionnelles tolérées » | rejeté : un ZIP v2 sans tables sport serait accepté silencieusement (perte non détectée) |
| Restaurer un v1 | remplace tout, sport compris (→ 0 session) : cohérent « instantané complet » ; l'aperçu affiche le compteur sessions ; sauvegarde de sécurité + annulation existantes |
| Sessions en cours | export : `FINISHED` seulement ; restauration **refusée** si session RECORDING/PAUSED (`RestoreError.SESSION_ACTIVE`) — sinon le service écrit dans des tables vidées |
| Validation | type/status/source connus, status = FINISHED, lat ∈ [−90,90], lon ∈ [−180,180], finis, distances ≥ 0, `(session_id, seq)` unique, FK session présente, uuid unique |
| Volume | 100 h ≈ 360 k points ≈ 45 Mo JSONL < `MAX_BYTES` 256 Mo ; [H] mémoire de `rows()` (liste complète) à mesurer en test 100 k points, sinon insertion en flux |
| CSV | `activity_sessions_v1.csv` : `date, start_utc, zone_id, type, moving_s, distance_m, elevation_gain_m, energy_kcal` (RFC 4180, via `CsvWriter`) |
| GPX | export par session GPX 1.1 (`trk/trkseg` = `segment`, `ele`, `time`), `GpxWriter` maison dans `data/export` via `XmlSerializer` plateforme, SAF `CreateDocument`, nom `tally-<uuid>.gpx` ; `ele` = ellipsoïde (documenté) |
| Import GPX | **hors périmètre** (source `GPX_IMPORT` réservée) |

## 6. Carte [D]

- **Aucune tuile, aucune lib carto, aucun réseau**.
- `TrackShape` dans `:core-ui` : `Canvas`, projection équirectangulaire (x = lon·cos(lat₀), y = lat), cadrage sur bornes + marge token, trait couleur accent, point de départ ; décimation `DouglasPeucker` (`:domain`, ε en mètres, ≤ 500 points) ; un `Path` par segment.
- `contentDescription` = « Tracé, 5,2 km ». Pas de profil altitude/vitesse en v1 (Vico disponible plus tard via `:core-ui`).

## 7. UI (`:activity`) [D]

| Écran | Contenu | Action principale |
|---|---|---|
| `ActivityScreen`/`ActivityContent` | carte « En cours »/« Interrompue » si active ; liste sessions (date, type, durée, distance, kcal), swipe + snackbar « Annuler » ; état vide avec action | FAB Démarrer → bottom sheet type (Marche/Course, dernier type mémorisé `app_setting activity.last_type`) |
| `RecordingScreen`/`RecordingContent` | durée, distance, allure (`TallyTheme.type.number*`), état GPS | FAB Pause ↔ Reprendre ; **Terminer visible seulement en pause** (anti-erreur, sans dialogue) |
| `SessionDetailScreen`/`SessionDetailContent` | `TrackShape`, stats, kcal ou action « Ajouter un poids » | Exporter GPX (icône top bar) |

- Routes `@Serializable` : `ActivityRoute`, `RecordingRoute`, `SessionRoute(id)`. Navigation vers Corps par lambda injectée par `:app`.
- Icônes nouvelles via `TallyIcons` (vector drawables `:core-ui`) : démarrer, pause, stop, marche, course, GPS.
- Strings FR/EN parité (`checkStrings`), 0 valeur en dur (`checkDesignTokens`), cibles ≥ 48 dp.

## 8. Jalons [D]

| # | Livrable | Vérification (critère de sortie) |
|---|---|---|
| S1 Domaine + base | modèles, logique pure, use cases, entités/DAO/repo, `MIGRATION_3_4`, schéma 4.json, DI | JVM : haversine (distance connue ±0,1 %), filtres sur traces rejouées (fixtures `domain/src/test/resources`), hystérésis, kcal (valeurs §2.5), DP, allure ; Robolectric : DAO, CASCADE, `appendBatch` atomique, 100 k points (temps consigné), migration 3→4 + chaîne 1→4 ; `checkArchitecture` vert |
| S2 Sauvegarde v2 + CSV | `FORMAT_VERSION 2`, DTOs v2, `tablesFor`, `SESSION_ACTIVE`, CSV sessions | round-trip v2 égalité 100 % (sessions + points) ; **fixture ZIP v1 figée** restaurée OK ; rejets (lat hors borne, FK, status RECORDING) ; base inchangée sur rejet |
| S3 Enregistrement | manifeste, allowlist, `LocationSource`, `TrackingService`, controller, notif, flux permissions, `RecordingScreen` minimal, carte « interrompue » | `checkPermissions` vert ; Robolectric service (faux `LocationSource` : start → points flushés ; pause/reprise → segment+1 ; relance `intent=null` → reprise) ; VM + Turbine ; captures Roborazzi recording/pause/recherche GPS ; émulateur : route rejouée (`adb emu geo fix` / GPX étendu dans `tools/emulator_smoke.py`), `am kill` → carte « interrompue » |
| S4 Historique + détail | liste, swipe/annuler, détail, `TrackShape`, `RecomputeMissingEnergy`, export GPX | captures `_{oled,light,font200}` ; GPX valide XSD 1.1 (test JVM) ; 0 `AndroidRuntime:E` au smoke |
| S5 Terrain | aide batterie, calibrage seuils §3.4 | critères AUDIT §5.1 Sport : 60 min écran éteint sans trou > 30 s, écart distance ≤ 2 %, perte ≤ 10 s après kill, batterie consignée — **sur appareil réel** |
| S6 (option) Saisie manuelle + bilan | `MANUAL` (QuantitySheet type/durée/distance) ; `ActivityRepository.energyFor(localDate)` dans Aujourd'hui | décision utilisateur U-1 |

Ordre imposé : S2 avant S3 (aucune donnée sport hors sauvegarde).

## 9. Hypothèses à vérifier

| # | Hypothèse | Quand / comment |
|---|---|---|
| H1 | Coefficients ACSM (§2.5) | avant S1, fact-check source primaire |
| H2 | Relance sticky FGS location Android 14+ (U4) | S3 émulateur API 37 + S5 appareil |
| H3 | Seuils filtres / hystérésis | S5 traces réelles rejouées |
| H4 | Warning Room FK sans index sur PK composite | S1 compilation |
| H5 | Mémoire restauration 100 k+ points | S2 test volume |
| H6 | `LocationManagerCompat` (androidx.core) suffit, 0 dépendance GMS | S3 : `releaseRuntimeClasspath` sans `gms`/`firebase`/`mlkit` (même contrôle que M15) |
| H7 | GPS émulé émulateur suffisant pour le smoke | S3 |

## 10. Décisions réservées à l'utilisateur

| # | Décision | Recommandation |
|---|---|---|
| U-1 | Afficher la dépense sport dans « Aujourd'hui » et l'ajouter au budget kcal | Afficher en info (« +210 kcal sport »), **sans** l'ajouter au budget par défaut |
| U-2 | Tests terrain S5 (appareils réels, marche/course) | seul l'utilisateur peut les faire |
