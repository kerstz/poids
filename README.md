# Poids

App Android de suivi santé / perte de poids. **100% local, hors-ligne, dégooglisé** (aucune dépendance Google Play Services).

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · Hilt · Coroutines/Flow · MVVM + repository.
targetSdk 36 · minSdk 26. Détail et justification : [`docs/research/phase0-recherche.md`](docs/research/phase0-recherche.md).

Choix dégooglisés : ZXing (scan), AOSP `LocationManager` (GPS), `TYPE_STEP_COUNTER` (pas), baromètre (dénivelé). Pas de Health Connect, pas de ML Kit, pas de chiffrement DB (v1). Nutrition : API Open Food Facts + cache Room à la volée.

## Modules

| Module | Rôle |
|---|---|
| `:domain` | Kotlin pur. Modèles + calculs BMR/TDEE/MET/distance (testables, sans Android). |
| `:data` | Room (entités, DAO, base, converters), repositories, DI Hilt. |
| `:app` | UI Compose, navigation, thème M3, `Application` + `MainActivity`. |

## Build

```bash
./gradlew :app:assembleDebug   # APK debug
./gradlew :domain:test         # tests des calculs métier
./gradlew installDebug         # installe sur appareil/émulateur connecté
```

SDK Android requis (chemin dans `local.properties`, non versionné).

## État

Phase 0 (recherche + plan) ✓ · Scaffold ✓ · Couche de données ✓ (build vert, tests verts).

### Schéma de données (v1)

`user_profile` · `weight_entry` · `food_item` (+ FTS4) · `meal_log` · `activity_session` · `route_point` · `step_day` · `sleep_log` · `substance_log`.

### Prochaines features (ordre)

1. Profil + BMR/TDEE (écran de configuration)
2. Poids + bilan calorique du jour
3. Nutrition : recherche → scan ZXing → photo repas
4. Activité : pas → GPS → calories MET
5. Habitudes : sommeil, substances
6. Historique / tendances (Vico)
