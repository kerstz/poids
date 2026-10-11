# Projet Android FOSS — Nutrition, poids et sport

## Stack imposée

Kotlin, Jetpack Compose + Material 3, MVVM + repositories + use cases,
Room/SQLite, Coroutines/Flow, CameraX + zxing-cpp (Apache-2.0).
ML Kit interdit. GPS via LocationManager natif, pas de dépendance Google Play Services.
Licence cible : GPLv3. Compatible F-Droid.

## Règles de délégation

Tu tournes en Sonnet. Tu ne fais PAS tout toi-même.
Délègue avec l'outil Agent en précisant le modèle :

### Exploration / lecture de code
- Sous-agent Haiku (model: haiku)
- Pour : lire des fichiers, chercher des patterns, résumer du code, explorer un dépôt
- Un sous-agent PAR dépôt ou par module
- Retourne un résumé structuré, pas le code brut

### Architecture / décisions critiques
- Sous-agent Opus (model: opus)
- Pour : choix d'architecture, modèle de données, résolution de bugs
  complexes après 2 tentatives échouées, revue de code avant merge/release
- Retourne une décision tranchée avec justification courte

### Code standard
- Tu le fais toi-même (Sonnet)
- Pour : écrans Compose, DAO Room, repositories, ViewModels,
  tests unitaires, renommages, strings, boilerplate

### Tâches mécaniques
- Sous-agent Haiku (model: haiku)
- Pour : renommages en masse, génération de tests répétitifs,
  traductions de strings, messages de commit

### Règles strictes
- Ne lis jamais un dépôt entier toi-même : spawne un Haiku par dépôt
- Ne décide jamais d'une architecture seul : spawne un Opus
- Si tu échoues 2 fois sur un bug : spawne un Opus
- Quand tu délègues, donne au sous-agent tout le contexte nécessaire
  (chemins, objectif, contraintes) — il ne voit pas cette conversation

## Style de sortie

- Tableaux et listes, pas de prose
- Terminal : 5 lignes max entre les appels d'outils
- Rapports écrits dans des fichiers (docs/), pas dans le terminal
- Cite les chemins des fichiers consultés
- Sépare faits vérifiés et hypothèses

## Règles de travail

- Travaille dans le répertoire du projet, jamais ailleurs
- Ne modifie aucun dépôt tiers cloné pour audit
- Ne présente pas une fonctionnalité comme opérationnelle sans vérification
- Signale incertitudes, limites et décisions à prendre
- Aucune suppression ni modification irréversible sans validation
- git clone --depth 1 pour les audits
