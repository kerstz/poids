# Politique de confidentialité — Tally

*Version 0.2 — dernière mise à jour : 2026-11-11. English version below.*

Tally est un journal alimentaire et de poids libre (GPLv3), sans compte, sans publicité et sans traçage.

## Données traitées

| Donnée | Où | Envoyée ailleurs ? |
|---|---|---|
| Journal alimentaire, poids et mesures, aliments, recettes, objectifs, réglages | Base locale de l'appareil (SQLite, répertoire privé de l'app ; non chiffrée par l'app, protégée par le bac à sable Android et le chiffrement de l'appareil) | Non |
| Code-barres scanné ou mots recherchés | Envoyés à Open Food Facts **seulement si l'option « Chercher les produits sur Open Food Facts » est activée (désactivée par défaut)** | Oui, à Open Food Facts |
| Fiches produits reçues d'Open Food Facts | Cache local de l'appareil | Non |
| Images de la caméra | Analysées en mémoire pour lire le code-barres, jamais enregistrées | Non |

Le journal alimentaire et le poids sont des **données relatives à la santé** (RGPD art. 9). Ils ne quittent jamais l'appareil du fait de l'application.

## Open Food Facts

Lorsqu'une recherche en ligne ou un scan est effectué avec l'option activée, l'application contacte
`world.openfoodfacts.org` et `search.openfoodfacts.org` en HTTPS. Sont transmis : le code-barres ou les
mots recherchés et, comme pour toute requête internet, votre **adresse IP**, l'**heure** de la requête et
l'identifiant de l'application (`Tally/<version> (Android; +https://github.com/kerstz/poids)`).
Aucun identifiant personnel, aucune donnée de journal ou de poids n'est envoyé.
Open Food Facts est un tiers indépendant : voir sa politique, <https://world.openfoodfacts.org/privacy>.
**L'option est désactivée par défaut (opt-in)** : l'application vous propose de l'activer au premier besoin
(onglet « En ligne » ou scan d'un code inconnu), en rappelant ce qui est envoyé. Elle peut être désactivée à tout
moment dans Réglages. Les connexions se font en TLS uniquement (aucun trafic en clair, aucune redirection vers HTTP).

## Ce que l'application ne fait pas

- Aucun compte, aucun serveur du développeur, aucune statistique d'usage ni rapport de plantage.
- Aucune bibliothèque publicitaire ou de mesure d'audience, aucun service Google Play.
- La sauvegarde cloud Android (Auto Backup) est désactivée.
- Permissions : `INTERNET` et `CAMERA` (demandée seulement au premier scan). Aucune localisation : le suivi sportif GPS est en développement et absent de cette version.

## Sauvegardes et exports

Les sauvegardes (ZIP) et exports (CSV) sont créés **à votre demande**, **en clair** (non chiffrés), à
l'emplacement que vous choisissez. Si vous les placez dans un service cloud ou les partagez, ils quittent
l'appareil sous votre responsabilité.

## Vos droits

- **Accès et portabilité** : Réglages → Données → Sauvegarder ou Exporter (CSV).
- **Rectification** : modification directe dans l'application.
- **Effacement** : Réglages → Données → « Effacer toutes mes données », ou désinstallation de l'application.
- Pour les données reçues par Open Food Facts, adressez-vous à Open Food Facts.

## Contact

Questions : <https://github.com/kerstz/poids/issues>.

<!-- Décidé le 2026-11-11 (audit) : réseau Open Food Facts désactivé par défaut, activation explicite au premier
     usage (opt-in), cf. docs/AUDIT.md M18. Cache Open Food Facts : fiches 30 jours, absences 7 jours, purgé à chaque
     écriture, effacé par « Effacer toutes mes données ».
     TODO (décision juridique non tranchée) :
     - identité et coordonnées du responsable de traitement / éditeur à indiquer avant publication sur un store ;
     - base légale à confirmer (traitement local sur l'appareil vs consentement explicite art. 9.2.a pour la recherche OFF). -->

---

# Privacy policy — Tally (English)

Tally is a free (GPLv3) food and weight journal with no account, no ads and no tracking.

- **Stored on the device only**: food journal, weight and measurements, foods, recipes, goals, settings, in a
  local SQLite database in the app's private storage (not encrypted by the app; protected by the Android sandbox
  and device encryption). Food journal and weight are health data; the app never sends them anywhere.
- **Open Food Facts** (**off by default**; only after you turn on "Look up products on Open Food Facts", which the
  app offers when you first need it): the scanned barcode or
  search words are sent over TLS only to `world.openfoodfacts.org` / `search.openfoodfacts.org`. As with any
  internet request, Open Food Facts also sees your **IP address**, the **time** and the app identifier
  (`Tally/<version> (Android; +https://github.com/kerstz/poids)`). See <https://world.openfoodfacts.org/privacy>.
  Received products are cached locally. You can turn this option off in Settings.
- **Camera** frames are analysed in memory to read barcodes and never saved.
- No developer server, analytics, crash reporting, ad SDK or Google Play services. Android cloud backup is disabled.
  Permissions: `INTERNET` and `CAMERA` only; no location (GPS activity tracking is still in development and not in this version).
- Open Food Facts cache: products 30 days, misses 7 days, purged on every write and by "Erase all my data".
- **Backups (ZIP) and exports (CSV)** are created on request, **unencrypted**, where you choose. If you put
  them in a cloud service, they leave the device.
- **Your rights**: access/portability via Backup and CSV export; rectification in the app; erasure via
  Settings → Data → "Erase all my data" or by uninstalling the app.
- Contact: <https://github.com/kerstz/poids/issues>.
