# Politique de confidentialité — Tally

*Version 0.1 — dernière mise à jour : 2026-10-16. English version below.*

Tally est un journal alimentaire et de poids libre (GPLv3), sans compte, sans publicité et sans traçage.

## Données traitées

| Donnée | Où | Envoyée ailleurs ? |
|---|---|---|
| Journal alimentaire, poids et mesures, aliments, recettes, objectifs, réglages | Base locale de l'appareil | Non |
| Code-barres scanné ou mots recherchés | Envoyés à Open Food Facts **seulement si l'option « Consulter Open Food Facts » est activée** | Oui, à Open Food Facts |
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
L'option peut être désactivée à tout moment dans Réglages.

## Ce que l'application ne fait pas

- Aucun compte, aucun serveur du développeur, aucune statistique d'usage ni rapport de plantage.
- Aucune bibliothèque publicitaire ou de mesure d'audience, aucun service Google Play.
- La sauvegarde cloud Android (Auto Backup) est désactivée.
- Permissions : `INTERNET` et `CAMERA` (demandée seulement au premier scan).

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

<!-- TODO (décision juridique non tranchée, voir docs/research audit 2026-10-16) :
     - identité et coordonnées du responsable de traitement / éditeur à indiquer avant publication sur un store ;
     - base légale à confirmer (traitement local sur l'appareil vs consentement explicite art. 9.2.a pour la recherche OFF) ;
     - politique de conservation éventuelle du cache Open Food Facts ;
     - réseau Open Food Facts actif par défaut (opt-out) : décider entre désactivé par défaut ou demande au premier usage. -->

---

# Privacy policy — Tally (English)

Tally is a free (GPLv3) food and weight journal with no account, no ads and no tracking.

- **Stored on the device only**: food journal, weight and measurements, foods, recipes, goals, settings.
  Food journal and weight are health data; the app never sends them anywhere.
- **Open Food Facts** (only when "Look up products on Open Food Facts" is enabled): the scanned barcode or
  search words are sent over HTTPS to `world.openfoodfacts.org` / `search.openfoodfacts.org`. As with any
  internet request, Open Food Facts also sees your **IP address**, the **time** and the app identifier
  (`Tally/<version> (Android; +https://github.com/kerstz/poids)`). See <https://world.openfoodfacts.org/privacy>.
  Received products are cached locally. You can turn this option off in Settings.
- **Camera** frames are analysed in memory to read barcodes and never saved.
- No developer server, analytics, crash reporting, ad SDK or Google Play services. Android cloud backup is disabled.
- **Backups (ZIP) and exports (CSV)** are created on request, **unencrypted**, where you choose. If you put
  them in a cloud service, they leave the device.
- **Your rights**: access/portability via Backup and CSV export; rectification in the app; erasure via
  Settings → Data → "Erase all my data" or by uninstalling the app.
- Contact: <https://github.com/kerstz/poids/issues>.
