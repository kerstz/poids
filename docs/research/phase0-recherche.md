# Phase 0 — Recherche & Plan technique
**App Android de suivi santé / perte de poids — 100% local, hors-ligne, privacy-first**

Date de recherche : 2026-06-23 · Dev intermédiaire · Native Android · Cible Google Play 2026
Méthode : 4 axes de recherche parallèles, sources primaires privilégiées, claims recoupés ≥2 sources, niveaux de confiance indiqués.

---

## Décisions v1 verrouillées (validées par l'utilisateur, 2026-06-23)

- **Posture privacy : full dégooglisé.** ZXing (scan), AOSP `LocationManager`+`GPS_PROVIDER` (GPS), `TYPE_STEP_COUNTER` brut (pas). **Pas de Health Connect, pas de ML Kit, pas de FusedLocation.** Cible F-Droid + Play, zéro dépendance proprio.
- **Chiffrement : non.** Room standard (clair). Pas de SQLCipher → pas de souci 16 KB. Réversible plus tard.
- **Nutrition : cache à la volée.** Aucune DB bundlée. Scan → API OFF online → écrit cache Room → rescan offline. APK léger.
- Conservés : Compose M3, **Hilt** (OK sans GMS), Coroutines/Flow, Room FTS4 + auto-migrations, Vico, Coil + Photo Picker, WorkManager, baromètre pour dénivelé, sommeil/habitudes en saisie manuelle. targetSdk 36, minSdk 26.

---

## Résumé exécutif (décisions clés)

1. **Stack confirmé** : Kotlin + Jetpack Compose + Room = bon choix natif 2026, validé par la doc Google. Compléments ajoutés (Hilt, Coroutines/Flow, Vico, Coil, WorkManager). **Confiance : Haute.**
2. **Google Fit est mort** : sign-ups API fermés depuis 2024-05-01, fin de support fin 2026. Remplaçant officiel = **Health Connect** (on-device, chiffré, sans cloud) → s'aligne avec privacy-first. À intégrer en **bridge optionnel**, pas comme stockage principal.
3. **Nutrition** : **Open Food Facts (primaire, ODbL) + USDA FoodData Central (fallback, CC0)** = les 2 seules sources dont la licence autorise un **cache local persistant** et un **dump offline**. Les APIs commerciales (Nutritionix/FatSecret/Edamam/Spoonacular) interdisent le cache persistant → inutilisables pour l'offline.
4. **Scan code-barres** : **ML Kit** (on-device, gratuit) pour build Play Store ; ZXing en option si cible F-Droid / dégooglisé.
5. **Différenciateur produit** : aucun concurrent ne marche vraiment hors-ligne. Offline réel + zéro compte + zéro pub + scan gratuit = attaque directe des pires reproches de MyFitnessPal/Cronometer/FatSecret.

---

## 1. Analyse concurrentielle

| Dimension | MyFitnessPal | Google Fit | Cronometer | FatSecret |
|---|---|---|---|---|
| Modèle | PE-owned (Francisco Partners) | Google/pub | Bootstrappé, privacy | B2B data + app |
| Food DB | ~20M, **crowdsourcé** (erreurs) | aucune | ~970k, **curé/vérifié** | ~4.2M crowdsourcé |
| Scan code-barres | **Premium-only (oct 2022)** | N/A | Gratuit | Gratuit |
| Micronutriments | Calories + macros | N/A | **~82 micros** | ~13 |
| BMR/TDEE | Mifflin-St Jeor | objectifs activité only | Mifflin-St Jeor | calc calorique |
| Précision data | Moyenne | Moyenne (activité) | **Haute (gold std)** | **La plus basse** |
| Privacy | **Mauvaise** (breach 150M 2018) | Mitigée | **Forte** | Correcte |
| Friction monét. | **Haute** (pub+paywall) | Free | Moyenne (cap 7j histo) | **Basse** |
| **Hors-ligne** | **Mauvais** | partiel | **Aucun** | **Aucun** |

**Ce que les users adorent/détestent :**
- MFP : DB énorme + log rapide ❤️ / **paywall scan + pubs** 💢 (grief n°1)
- Cronometer : précision + 80 nutriments ❤️ / **courbe d'apprentissage, dense, pas d'offline, cap histo 7j** 💢
- FatSecret : free tier généreux ❤️ / **UI datée, DB imprécise** 💢
- Google Fit : simplicité ❤️ / **abandonné** ; le successeur "Google Health" (2026) prend une volée de critiques 💢

**État Google Fit 2026 (vérifié) :**
| Date | Événement |
|---|---|
| 2024-05-01 | Fermeture sign-ups API Fit (REST + Android) |
| ~mai 2026 | App Fit → remplacée par "Google Health" (Fitbit rebrandé) |
| sept 2026 | Fitbit Web API décommissionnée |
| fin 2026 | APIs Google Fit : fin de support |

→ **Ne PAS construire sur Google Fit. Utiliser Health Connect.** (Confiance : Haute)

**Gaps exploitables par une app local-first :**
1. **Offline réel** = faiblesse universelle des 4 → bundler une DB on-device (OFF+USDA, licences ouvertes) contourne même la contrainte de licence qui empêche Cronometer de passer offline.
2. **Zéro compte / zéro cloud** — "tes données ne quittent jamais ton téléphone".
3. **Pas de paywall scan / histo** — attaque directe du grief n°1 MFP.
4. **Précision sans complexité** — viser la fiabilité Cronometer avec un log rapide.
5. **Health Connect** au lieu de Fit mort.

**Décisions design retenues :** offline en feature phare · DB curée > crowdsourcée · jamais paywaller scan/histo · Mifflin-St Jeor pour BMR/TDEE (standard industrie) · éviter le piège de la densité UI.

---

## 2. Stack technique (recommandé)

| Composant | Choix | Conf. | Pourquoi |
|---|---|---|---|
| Langage | **Kotlin 2.4.x** | Haute | Seul langage natif first-class |
| UI | **Jetpack Compose** (BOM 2026.06.00) | Haute | Google "Compose-first" (2026-05-19) ; Views en maintenance |
| Design | **Material 3** (1.4.0) | Haute | Stable. M3 Expressive = pas encore GA, éviter en prod-critique |
| DB locale | **Room 2.8.x** | Haute | Meilleur fit solo/single-platform. SQLDelight = avantage KMP non pertinent ici |
| → Recherche aliments | **Room FTS4** (`@Fts4`) | Haute | `MATCH`/préfixe rapide vs `LIKE '%...%'` |
| → Time-series (poids/pas) | **Room + Flow** | Haute | `Flow<List<T>>` réémet → charts live |
| → Migrations | **auto-migrations** (`exportSchema=true`) | Haute | Évite ALTER TABLE manuels |
| Archi | **MVVM + repository + UDF** | Haute | C'EST la reco officielle Google |
| → Domain layer | **Skip** jusqu'à logique réutilisée | Haute | Optionnel, inutile à cette taille |
| → MVI | Pas de lib ; `UiState` immuable (data class/sealed) | Haute | UDF déjà MVI-flavoured |
| DI | **Hilt** | Haute | Reco officielle ; moins de friction. Koin = alt valide si KMP plus tard |
| Async | **Coroutines + Flow** (StateFlow + `collectAsStateWithLifecycle`) | Haute | Reco verbatim Google |
| Health store | **Room = source de vérité ; Health Connect = bridge opt-in** | Haute | Voir §détail |
| GPS | **Foreground service type `location`** | Haute | `FOREGROUND_SERVICE_LOCATION` + `ACCESS_FINE_LOCATION` |
| Travail périodique | **WorkManager** | Haute | Doze/retries/reboot |
| targetSdk | **36 / compileSdk 36** | Haute | Nouvelles apps doivent cibler 36 avant 2026-08-31 |
| minSdk | **26 (Android 8.0)** | Haute | `java.time`, channels, crypto moderne, large portée |
| 16 KB pages | OK si pas de `.so` | Haute | Room pur = pas de natif. SQLCipher ≥4.6.1 si chiffrement |
| Photos repas | **Fichier dans `filesDir` + chemin relatif en Room** | Haute | Jamais de blob image en SQLite |
| → Sélection photo | **Photo Picker** (`PickVisualMedia`) | Haute | Zéro permission stockage |
| → Chargement image | **Coil** (`coil-compose`) | Haute | Kotlin-first, Compose-native |
| Charts | **Vico** (Apache-2.0, v3.2.3) | Haute | Compose-native, maintenu. Éviter MPAndroidChart (View, stalled) |

**Health Connect — décision nuancée :** Room = source de vérité unique (app marche partout sans dépendance externe). Health Connect = couche interop **opt-in** : *lire* pas/sommeil depuis wearables, *écrire* poids/nutrition. Permissions `health.*` minimales, gated par rationale, activité privacy-policy obligatoire. **NE PAS** en faire le store principal (HC absent sur Android <14, requiert Play Services → casse le dégooglisé).

**Caveats build :** M3 Expressive pas GA · confirmer deadline API 36 dans Play Console · si chiffrement → SQLCipher ≥4.6.1 (16 KB) · Android 16 : pas de FGS location lancé depuis BOOT_COMPLETED · récupérer % distribution minSdk live depuis Android Studio.

**Stack finale :** Compose (M3) → ViewModel expose `UiState` immuable en StateFlow → repository (source unique) sur Room 2.8.x (FTS4, Flow, auto-migrations) → Coroutines/Flow + `collectAsStateWithLifecycle()` → Hilt → Vico → Coil + Photo Picker → WorkManager → FGS type location pour GPS → Room + Health Connect opt-in → targetSdk 36, minSdk 26.

---

## 3. Sources de données nutrition

| | **Open Food Facts** (primaire) | **USDA FDC** (fallback) | Nutritionix | FatSecret | Edamam | Spoonacular |
|---|---|---|---|---|---|---|
| Coût | Gratuit, sans clé | Gratuit, clé gratuite | ≥499$/mo | Free / contact sales | 14-299$+/mo | 29-300$+/mo |
| Code-barres | Oui (cœur) | Oui (`gtinUpc`) | Oui US+UK | Oui 58+ pays | Oui 700k+ | Oui ~86k US |
| Région | Global ; FR/DE/UK fort, US moyen | US | US+UK | 58+ pays | Global US-weighted | US only |
| **Cache local persistant ?** | **OUI (ODbL)** | **OUI (CC0)** | Non | **Non (purge 24h)** | **Non** | **Non (1h)** |
| Dump offline | **OUI nightly** (Parquet/CSV/Mongo) | **OUI** (CSV/JSON, bi-annuel) | Payant | Non | Non | Non |
| Licence | ODbL | CC0 domaine public | Proprio | Proprio | Proprio | Proprio |
| Fit offline | **Excellent** | **Excellent** | Faible | Faible | Faible | Pire |

**Faits clés :** OFF ~4M produits, API v3.6, lecture `GET /api/v2/product/{barcode}.json`, **rate limits 15 req/min produit + User-Agent custom obligatoire**, complétude nutritionnelle ~67% macros (Medium). USDA FDC 1000 req/h, Branded Foods avec `gtinUpc`, dump CSV/JSON refresh avril/octobre.

**Scanner code-barres :** **ML Kit** (`play-services-mlkit-barcode-scanning:18.3.1` + CameraX overlay Compose) ou GMS Code Scanner (zéro permission caméra). ZXing (`zxing-android-embedded`) uniquement pour build F-Droid/dégooglisé via product flavor derrière interface commune.

**Architecture recommandée (scan → lookup) :**
1. Scanner ML Kit décode EAN-13/UPC-A on-device (offline)
2. Ordre lookup : **(a)** cache Room d'abord (offline, rapide) → **(b)** DB bundlée (sous-ensemble OFF filtré + USDA Branded sur `gtinUpc`) → **(c)** API OFF live sur cache-miss (respecter 15 req/min + User-Agent), **écrire le produit en cache Room** → **(d)** fallback commercial online-only optionnel (NE PAS persister au-delà du TTL ToS)
3. Recherche manuelle → tables locales OFF/USDA, fallback Search-a-licious online
4. Refresh → re-pull dump OFF hebdo/mensuel, USDA bi-annuel

**Licences / attribution :**
- **OFF (ODbL)** : usage commercial OK, code app peut rester fermé. **Attribution obligatoire** ("Open Food Facts" + ODbL sur écran À propos). **Share-alike** = si tu crées une *Derivative Database* publiquement distribuée, elle doit être en ODbL. ⚠️ Piège : fusionner OFF + autre dataset en une seule DB force le tout en open data ODbL. **Mitigation :** garder OFF dans table séparée des données proprio. Images = CC-BY-SA séparé. *(Résumé technique, pas avis juridique — faire valider par juriste.)*
- **USDA FDC (CC0)** : aucune restriction, cache/bundle/redistrib libre.

---

## 4. Capteurs, localisation & permissions

### GPS
Deux chemins selon posture privacy :
| | **FusedLocationProviderClient** | **AOSP LocationManager + GPS_PROVIDER** (privacy) |
|---|---|---|
| Dépendance | Play Services | Aucune (marche dégooglisé) |
| Fusion/batterie | Auto | Manuelle |
| Route/distance/vitesse | Oui | **Oui** (OpenTracks/FitoTrack l'utilisent) |

- **Vitesse :** `Location.getSpeed()` (Doppler GNSS, plus précis que dériver positions), garder `hasSpeed()`
- **Distance :** somme `distanceTo()` par fix ; filtrer fixes avec `getAccuracy()` > ~20-50 m (anti-jitter)
- **Altitude/dénivelé :** `getAltitude()` = ellipsoïde WGS84, mauvais verticalement. **Utiliser baromètre** `Sensor.TYPE_PRESSURE` + `SensorManager.getAltitude()`, cumuler deltas au-dessus seuil hystérésis ~1-3 m (anti-bug "20000 ft en 1 mile"). Fallback altitude GPS filtrée.
- **FGS obligatoire** depuis Android 10 (type `location`). Android 14 : déclaration manifest + `FOREGROUND_SERVICE_LOCATION`. Background écran-éteint : `ACCESS_BACKGROUND_LOCATION`. Android 15+ : pas de FGS location depuis BOOT_COMPLETED.
- **Batterie :** `PRIORITY_HIGH_ACCURACY` en session active, interval ~1000ms, stop updates quand inutile.
- **Privacy sans GMS :** `LocationManager + GPS_PROVIDER` (stock + GrapheneOS/LineageOS).

### Pas
- **`TYPE_STEP_COUNTER`** (cumulatif depuis boot, précis) pour totaux quotidiens ; `TYPE_STEP_DETECTOR` = events par pas. Les deux requièrent **`ACTIVITY_RECOGNITION`** (API 29+).
- **Pas du jour** = compteur actuel − baseline stocké à minuit. **Reboot** : si compteur < baseline → reset baseline (compteur revenu à 0). Persister baseline (DataStore/Room).
- **Distance via pas :** `step_length(cm) ≈ taille(cm) × 0.415` (H) / `× 0.413` (F). Erreur ±5-10% (longueur de pas varie avec vitesse).
- HC `StepsRecord` : utiliser `aggregate(COUNT_TOTAL)` (déduplique). Mais HC requiert Play Services → **raw STEP_COUNTER = défaut plus fort pour privacy**.

### Calories
```
Par minute :  kcal/min   = (MET × 3.5 × poids_kg) / 200
Total :       total_kcal = MET × poids_kg × durée_h
Net : utiliser (MET − 1)
```
Valeurs MET : **2024 Adult Compendium of Physical Activities** (pacompendium.com). Ex : marche 3 mph ~3.5 · course 6 mph ~9.8 · vélo 12-14 mph ~8.0. Plus précis avec vitesse+pente : équations ACSM.
**Caveat : étiqueter ±20-30% (estimation).** Les wearables sur-estiment quasi toujours.

### Permissions (table)
| Permission | Type | Pourquoi | Quand demander |
|---|---|---|---|
| `ACCESS_FINE_LOCATION` | runtime | Routes GPS, vitesse | **En contexte** — au démarrage workout GPS |
| `ACCESS_BACKGROUND_LOCATION` | runtime (29+) | Route écran éteint | **2e requête séparée** après FINE (combiné rejeté Android 11+) |
| `ACTIVITY_RECOGNITION` | runtime (29+) | Compteur de pas | À l'activation du comptage |
| `POST_NOTIFICATIONS` | runtime (33+) | Notif FGS | Avant démarrage FGS / onboarding |
| `FOREGROUND_SERVICE` + `_LOCATION` | manifest | FGS GPS | Déclaré only |
| `CAMERA` | runtime | Scan code-barres | À l'ouverture du scanner |
| `health.READ_STEPS` etc. | Health Connect | Interop opt-in | Via PermissionController HC, si proposé |

**Flow :** demander en contexte (pas au launch) → `checkSelfPermission` → `shouldShowRequestPermissionRationale` → UI éducative → `registerForActivityResult`. Background location = incrémental (FINE d'abord, puis BACKGROUND séparé). **Dégradation gracieuse** : Approximate only → warn routes grossières ; background refusé → tracking seulement actif ; notif refusée → FGS tourne quand même. Jamais hard-block.

**Astuce :** tracker pas/GPS pur peut **éviter le FGS type `health`** entièrement — FGS `location` pour GPS + lire compteur via WorkManager, garder seulement `ACTIVITY_RECOGNITION`.

### Sommeil / habitudes
- **Aucune API sans Play Services n'auto-détecte le sommeil.** Play Services Sleep API existe mais soft-deprecated (Google pousse vers Health Connect). HC `SleepSessionRecord` = stockage seulement, ne détecte pas.
- **Reco privacy :** **saisie manuelle** (heures coucher/réveil) = défaut, zéro dépendance, ce que font la plupart des habit trackers. Option DIY heuristique (accéléromètre+lumière). Habitudes (alcool/tabac/repas) = saisie manuelle pure.

---

## Sources (sélection, avec dates)

**Stack/Android (primaires Google, accédés 2026-06-23) :**
- android-developers.googleblog.com/2026/05 — "Compose-first" (2026-05-19)
- developer.android.com — architecture (maj 2026-04-26), Room releases (2.8.4, maj 2026-05-07), Hilt (maj 2026-06-18), target-sdk (maj 2026-06-18)
- github.com/android/nowinandroid (réf)

**Concurrents :** SEC 8-K Under Armour (breach 2018-03-29), 9to5Google (Fit→Health, mai 2026), cronometer.com / platform.fatsecret.com (accédés juin 2026), developer.android.com Health Connect Fit migration FAQ.

**Nutrition :** world.openfoodfacts.org/data + API docs, huggingface OFF dataset, fdc.nal.usda.gov (CC0), developers.google.com/ml-kit (maj 2026-06-19), platform.fatsecret.com / developer.edamam.com / spoonacular.com.

**Capteurs :** developer.android.com sensors_motion + location/permissions (maj 2026-06-18) + fgs/service-types + about/versions/14 & 16, developers.google.com FusedLocationProviderClient + sleep, 2024 Adult Compendium (Herrmann/Ainsworth, *J Sport Health Sci* jan 2024, PMC10818145), Shcherbina et al. Stanford 2017 (précision wearables), OpenTracks/FitoTrack (FOSS).

**COI notés :** pages vendeurs (biais promo précision/couverture), sites review affiliés (Medium), pages éditeurs/Google (first-party sur leur propre stack).
