# Audit jalon 2 — commits 5c9e0b0..a9afd23

Date : 2026-10-11 · Périmètre : `git diff 0dcd56f..HEAD` (5 commits, 26 fichiers) · Archive/ et build/ ignorés.

## (a) Verdict

- **NO-GO** : le correctif 5c9e0b0 est inopérant depuis l'UI — l'éditeur d'aliment reconstruit un `Food` neuf et efface code-barres, source OFF, nutriments optionnels et densité à chaque modification (F1).
- Le reste (migration 1→2, schéma 2.json, journal figé, unicité) est correct. GO dès que F1 est corrigé et F2 traité.

## (b) Constats

| ID | Sév. | Fichier:ligne | Description | Scénario d'échec | Correctif proposé | Statut |
|---|---|---|---|---|---|---|
| F1 | **Bloquant** | `nutrition/src/main/kotlin/org/tally/nutrition/food/FoodEditorViewModel.kt:110-120` | `save()` construit `Food(id, name, brand, baseUnit, per100)` : `barcode`, `source`, `per100Detail` et `densityGPerMl` prennent leurs valeurs par défaut (null / USER / UNKNOWN / null). `FoodRepositoryImpl.save` (l.47-58) écrit désormais ces champs (via `toEntity`, l.62-80) → NULL / USER en base. Le test `updatePreservesColumnsAbsentFromDomain` passe uniquement parce qu'il utilise `food.copy(...)`, pas le ViewModel | Aliment importé d'OFF → l'utilisateur corrige le nom → sucres, AG saturés, fibres, sel, densité et **code-barres** effacés, `source` passe à USER (l'attribution ODbL promise par NOTICE.md l.17 disparaît). Au scan suivant, `findByBarcodeAny` ne trouve plus rien → **2e ligne** créée pour le même produit | Charger le `Food` dans le VM (`loaded`), puis `saveFood(loaded.copy(name=…, brand=…, baseUnit=…, per100=…), portions)`. Ajouter un test VM ou repo « éditer un aliment OFF conserve barcode/source/detail/density » | Fait vérifié (lecture du code ; non exécuté sur appareil) |
| F2 | Majeur | `domain/.../RemoteUseCases.kt:45-71` + `nutrition/.../add/AddEntryViewModel.kt:140-143` + `data/.../dao/FoodDao.kt:37` | Course entre `findByBarcodeAny` et `insert` : il n'y a ni transaction, ni Mutex, ni `try/catch`, et l'insert est en `ABORT`. `chooseRemote` lance une coroutine à chaque appui | Double appui sur un résultat OFF → 2 imports concurrents → le 2e `insert` lève `SQLiteConstraintException` (index unique) dans `viewModelScope` → **crash de l'app** (avant v2 : doublon silencieux) | (1) Côté repo : `insertWithPortions` dans `withTransaction` + intercepter `SQLiteConstraintException` → relire par `findByBarcodeAny` et renvoyer l'id existant. (2) Côté VM : ignorer `chooseRemote` si un import est en cours | Code : fait vérifié ; timing réel : hypothèse |
| F3 | Mineur | `domain/.../RemoteUseCases.kt:55-56` | Branche archivée : `saveFood` puis `foods.unarchive` sont deux écritures non transactionnelles | Le processus meurt entre les deux → aliment rafraîchi mais toujours archivé. Rejouer l'import corrige l'état (idempotent) | Une méthode repo unique (`restore(food, portions)`) dans `withTransaction`, ou un `save` qui respecte `isArchived` | Fait vérifié |
| F4 | Mineur | `domain/.../RemoteUseCases.kt:47-53` | Le rafraîchissement remplace `per100` mais garde l'ancien `per100Detail` (et `densityGPerMl`). Il ignore `servingQuantityG` et ne met pas à jour `off_last_modified`. Les portions sont bien conservées, la source est forcée à OFF (les éditions utilisateur sont écrasées sans avertissement) | Les nouvelles macros OFF cohabitent avec d'anciens sucres et sels → les détails sont incohérents dans le journal | `per100Detail = NutrientDetail.UNKNOWN` (RemoteFood ne porte pas de détail) ou étendre `RemoteFood` ; décider s'il faut écraser les éditions utilisateur (voir D3) | Fait vérifié |
| F5 | Mineur | `data/.../nutrition/FoodRepositoryImpl.kt:62-80` | `toEntity` ne mappe pas `offLastModified` et `RemoteFood.lastModifiedEpochSec` n'est jamais propagé : la colonne vaut toujours NULL à l'insertion. La « préservation » de 5c9e0b0 préserve donc NULL | Aucune possibilité de rafraîchissement conditionnel (If-Modified-Since, AUDIT §3.4) | Porter `offLastModified` dans le domaine ou dans l'appel d'import | Fait vérifié |
| F6 | Mineur | `domain/.../Models.kt:44-52` | `NutrientDetail.plus` compte un null comme 0 dès qu'un des deux termes est connu : une somme partielle est présentée comme connue | Total du jour « sucres 10 g » alors que la moitié des entrées sont inconnues → sous-estimation présentée comme exacte (non affiché dans l'UI à ce jour) | Ajouter un indicateur `isPartial` ou un compteur d'inconnus, ou garder null si un terme est null (voir D4) | Fait vérifié (sémantique), impact UI : hypothèse |
| F7 | Mineur | `domain/.../LogUseCases.kt:58-70` | `LogRecipe` n'alimente pas `detail` : toujours UNKNOWN pour une recette, même si tous les ingrédients ont leurs sucres, sels, etc. | Les journaux de recettes n'ont jamais de nutriments optionnels | Calculer `detail` depuis les ingrédients (somme × portions / servings) ou documenter le choix | Fait vérifié |
| F8 | Mineur | `data/.../db/Migrations.kt:27-36` | Heuristique « énergie recalculée égale à 0,01 près ⇒ aliment inchangé » : faux positif si d'autres colonnes ont changé à kcal constante | Sucres v2 actuels attachés à une entrée v1 enregistrée avant une modification. Risque faible : l'éditeur efface le détail (F1) | Accepter et documenter, ou comparer aussi protein/carbs/fat | Hypothèse |
| F9 | Mineur (hors diff) | `nutrition/.../journal/JournalViewModel.kt:85-86` | L'annulation appelle `journal.add` directement, sans passer par un use case (contraire à AUDIT M7 et au KDoc de `SaveUseCases.kt`). Le snapshot conserve bien name, brand, detail et macros. En revanche, `portion_id` peut référencer une portion supprimée entre-temps : `updateWithPortions` supprime et recrée toutes les portions | Supprimer une entrée → modifier l'aliment → « Annuler » → violation de clé étrangère sur `portion_id` → crash | Use case `RestoreJournalEntry` qui remet `portionId` à null si la portion n'existe plus | Code : fait vérifié ; scénario : hypothèse (fenêtre courte) |
| F10 | Mineur | `domain/.../RemoteUseCases.kt:20-22`, `data/.../remote/OffApi.kt:144` | Aucune normalisation GTIN : UPC-A 12 chiffres et EAN-13 « 0 » + UPC sont deux chaînes distinctes. OFF renvoie `code` (préféré au code saisi) | Saisie 12 chiffres → aliment stocké en 13 chiffres → `findByBarcode(12)` rate → requête réseau à chaque fois ; hors ligne : « indisponible » alors que l'aliment est local. Pas de crash (`findByBarcodeAny` sur le code OFF) | Normaliser en GTIN-13 (ou 14) à l'écriture et à la lecture (`Gtin.normalize`) | Code : fait vérifié ; normalisation OFF : hypothèse |
| F11 | Mineur | `data/src/test/...`, `domain/src/test/...` | Tests manquants : renommer un aliment ne change pas le nom au journal ; l'annulation conserve name/brand/detail ; `LogFood` met `detail` à l'échelle ; `NutrientDetail.plus/times` ; aller-retour par l'éditeur (F1) ; migration avec 2 doublons **actifs**, unité ML et `PRAGMA foreign_keys=ON` | Les régressions F1 et F6 ne sont pas détectées | Ajouter ces tests | Fait vérifié |
| F12 | Mineur (doc) | `docs/AUDIT.md:669` (C4), `NOTICE.md:28` | « Version LO 1.0 ou 2.0 à confirmer » : la question peut être tranchée. L'API data.gouv distingue `fr-lo` (« Licence Ouverte / Open Licence », URL `etalab.gouv.fr/wp-content/uploads/2014/05/Licence_Ouverte.pdf`) de `lov2` (« … version 2.0 ») | Mention de licence imprécise | Écrire « Licence Ouverte 1.0 (Etalab, 2011) » et citer `api/1/datasets/licenses/` | Fait vérifié (API, 2026-10-11) ; équivalence fr-lo = LO 1.0 : déduction forte |
| F13 | Mineur (doc) | `NOTICE.md:17-18` | (1) « Les aliments copiés depuis OFF portent `source = OFF` » est faux après une édition (F1). (2) Selon le texte de l'ODbL, le partage à l'identique s'applique à une base dérivée **utilisée publiquement** ; « sauvegarde partagée » va plus loin que le texte | Attribution perdue ; obligation mal décrite | Corriger après F1 ; reformuler : « base dérivée utilisée publiquement (§4.4 ODbL) » | (1) Fait vérifié ; (2) Hypothèse (texte ODbL non relu ici) |
| F14 | Mineur (doc) | `docs/AUDIT.md:383,387` | `CHECK(>=0)` et `food_fts` (FTS4) sont annoncés mais absents de `2.json` (0 occurrence ; la recherche utilise `LIKE`, `FoodDao.kt:15-20`). Écart antérieur au diff, voisin de la ligne 386 modifiée | Documentation de schéma fausse | Marquer [non implémenté] ou supprimer | Fait vérifié |

### Points vérifiés sans défaut

| Point | Constat | Statut |
|---|---|---|
| Migration : sous-requêtes corrélées | Correctes. La dédup est indépendante de l'ordre de traitement : on ne met à NULL que des lignes ≠ tête, la tête reste stable | Fait vérifié (raisonnement + test) |
| `ORDER BY (archived_at IS NULL) DESC` | 1 = non archivé en premier, puis `updated_at DESC`, puis `id DESC` | Fait vérifié |
| Perte de données | Aucune ligne supprimée ; seuls les doublons perdent leur code (y compris un doublon **actif**, ce qui est voulu) ; macros v1 intactes | Fait vérifié (test) |
| Conformité 2.json | `name TEXT NOT NULL DEFAULT ''` = entité `@ColumnInfo(defaultValue="")` ; `runMigrationsAndValidate(…, true)` passe | Fait vérifié (test exécuté) |
| FK / index de `journal_entry` | Identiques entre 1.json et 2.json (4 FK, 5 index) ; seul `index_food_barcode` devient unique | Fait vérifié (comparaison JSON) |
| Clés étrangères pendant la migration | `ADD COLUMN` et `UPDATE food.barcode` ne touchent aucune colonne FK | Fait vérifié |
| Jointure vivante | Supprimée de `JournalDao.observeDay` ; aucune autre lecture du journal ne joint `food`/`recipe` | Fait vérifié |
| NOT NULL `name` | Tous les constructeurs de `NewJournalEntry` (LogFood, LogRecipe, snapshot) fournissent `name` | Fait vérifié |
| Code-barres `''` | Inatteignable : l'éditeur ne gère pas de code, OFF exige `isNotBlank` ou le code saisi (8-14 chiffres) | Fait vérifié |
| Update avec code modifié | Seul chemin : `SaveFood` (contrôle `owner.id != food.id`) ; aucun appel direct à `repository.save` hors use case | Fait vérifié (grep) |
| `Food.isArchived` ignoré à l'écriture | `toEntity` ne le mappe pas ; l'update reprend `existing.archivedAt` | Fait vérifié |
| UI inchangée | `NutrientDetail` / `detail` ne sont utilisés par aucun module UI | Fait vérifié (grep) |
| Dépendances Google | Seul ajout : `androidx.room:room-testing` (test) | Fait vérifié |
| User-Agent | `Tally/<v> (Android; +https://github.com/kerstz/poids)` ; dépôt **public** (`gh repo view`) ; pas d'e-mail | Fait vérifié ; conformité au format attendu par OFF : hypothèse |
| AUDIT §8 C1 | data.gouv : `license=fr-lo`, `last_modified=2025-11-20T10:35`, ressources 2020-07-03 | Fait vérifié (API) |

### Affirmations de licence (AUDIT.md §8, NOTICE.md)

| Affirmation | Statut |
|---|---|
| CIQUAL 2020 sous Licence Ouverte (data.gouv `fr-lo`) | Vérifié |
| `fr-lo` = LO 1.0 | Vérifié (API, URL du PDF de 2014) — déduction |
| OFF : ODbL 1.0 (base), DbCL (contenu), CC BY-SA (images) | Hypothèse (connaissance, non relu en ligne ici) |
| USDA FDC : CC0 1.0 | Hypothèse (non vérifié ici) |
| O1/O2/O5/O6 (compatibilité GPLv3↔ODbL, FSF, Etalab, F-Droid) | Hypothèse — correctement marqué H dans AUDIT.md |
| O3 « F (texte ODbL) » | Hypothèse ici (texte non relu) ; cohérent avec la connaissance de l'ODbL |
| C2 (sha256, taille du xls) | Non vérifié (fichier non versionné) |
| CIQUAL 2025 : licence | **Non vérifiée, non devinée** |

## (c) Résumé exécutif

- Migration 1→2 : SQL correct, aucune ligne perdue, schéma conforme (test Room OK), FK/index du journal inchangés.
- Journal figé : plus aucune jointure vivante ; nom, marque et détail conservés à l'annulation.
- **F1 (bloquant)** : l'éditeur d'aliment efface code-barres, source, nutriments optionnels et densité à chaque sauvegarde. 5c9e0b0 ne corrige que le chemin repo.
- Conséquence de F1 : produit OFF modifié → code perdu → doublon au prochain scan ; l'attribution OFF de NOTICE.md n'est plus vraie.
- **F2 (majeur)** : double appui sur l'import OFF → `SQLiteConstraintException` non interceptée → crash (régression liée à l'index unique).
- Import d'un code archivé : fonctionnel mais non atomique, et nutriments optionnels incohérents (F3, F4).
- `NutrientDetail.plus` présente une somme partielle comme connue (F6) ; recettes sans détail (F7).
- Docs : LO 1.0 vérifiable (F12) ; NOTICE.md à corriger après F1 (F13) ; schéma AUDIT.md non conforme (F14).
- Tests : 37 tests domain + data passent ; plusieurs chemins critiques ne sont pas testés (F11).
- Règles du projet : validation par use cases respectée dans le diff (sauf l'annulation, antérieure) ; UI inchangée ; aucune dépendance Google.

## (d) Décisions à prendre (utilisateur)

| # | Décision | Options |
|---|---|---|
| D1 | Correctif F1 | Éditer `loaded.copy(...)` (recommandé) ou ajouter les champs barcode/détail à l'éditeur |
| D2 | Course à l'import (F2) | Garde dans le repo (transaction + reprise sur conflit) et/ou verrou dans le VM |
| D3 | Ré-import d'un aliment archivé modifié par l'utilisateur | Écraser par OFF (actuel) ou garder les éditions et seulement désarchiver |
| D4 | Somme de nutriments partiellement inconnus | Somme partielle + indicateur, ou null dès qu'un terme manque |
| D5 | Détail des recettes | Le calculer depuis les ingrédients ou l'assumer UNKNOWN |
| D6 | Forme canonique du code-barres | GTIN-13 (UPC-A préfixé par 0) ou GTIN-14 ; migration v3 si changement |
| D7 | CIQUAL 2025 | Rester sur 2020 (LO 1.0 vérifiée) tant que la licence 2025 n'est pas lue sur le site Anses |
| D8 | Formulation ODbL dans NOTICE.md | Restreindre à « utilisation publique » (texte ODbL) ; avis juridique éventuel |

## (e) Non vérifié

- Mise à niveau réelle d'une installation v1 sur appareil (SQLite Android ≠ Robolectric ; comportement de `foreign_keys` pendant la migration non testé).
- Exécution de F1, F2 et F9 sur appareil (déduits du code ; aucun test d'interface lancé).
- Normalisation des codes par OFF (UPC-A → EAN-13).
- Licence CIQUAL 2025 ; sha256 et taille du xls (C2) ; absence d'un jeu 2025 sur data.gouv (C5 non refait).
- Textes ODbL, DbCL, CC BY-SA, CC0 (USDA) et liste FSF non relus en ligne.
- Acceptation F-Droid et format User-Agent attendu par OFF.
- Captures Roborazzi et `checkDesignTokens` non relancés (UI non modifiée par le diff).

## Fichiers consultés

- `data/src/main/kotlin/org/tally/data/db/Migrations.kt`, `TallyDatabase.kt`, `dao/FoodDao.kt`, `dao/JournalDao.kt`, `entity/FoodEntity.kt`, `entity/JournalEntryEntity.kt`, `di/DataModule.kt`
- `data/src/main/kotlin/org/tally/data/nutrition/FoodRepositoryImpl.kt`, `JournalRepositoryImpl.kt`, `data/src/main/kotlin/org/tally/data/remote/OffApi.kt`
- `data/schemas/org.tally.data.db.TallyDatabase/1.json`, `2.json`
- `domain/src/main/kotlin/org/tally/domain/nutrition/{Models,LogUseCases,RemoteUseCases,SaveUseCases,Repositories,Remote}.kt`, `core/src/main/kotlin/org/tally/core/barcode/Gtin.kt`
- `nutrition/src/main/kotlin/org/tally/nutrition/food/FoodEditorViewModel.kt`, `add/AddEntryViewModel.kt`, `journal/JournalViewModel.kt`
- `app/src/main/kotlin/org/tally/app/di/AppModule.kt`, `*/res/values-fr/strings.xml` (attribution OFF présente)
- Tests : `data/src/test/.../MigrationTest.kt`, `NutritionRepositoryTest.kt`, `OffRemoteTest.kt`, `domain/src/test/.../NutritionTest.kt`, `RemoteUseCasesTest.kt`
- `docs/AUDIT.md` §4 (l.383-387) et §8, `NOTICE.md`
- Externe : `data.gouv.fr/api/1/datasets/table-de-composition-nutritionnelle-des-aliments-ciqual/`, `data.gouv.fr/api/1/datasets/licenses/`, `gh repo view kerstz/poids`

## Tests exécutés

`./gradlew --offline :domain:test :data:testDebugUnitTest --tests MigrationTest --tests 'org.tally.data.nutrition.*' --tests 'org.tally.data.remote.*'` → 37 tests, 0 échec (MigrationTest 1, NutritionRepositoryTest 8, FoodSearchBenchmarkTest 1, OffRemoteTest 10, domain 17 ; les résultats domain proviennent peut-être du cache Gradle).
