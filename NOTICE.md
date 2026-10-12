# NOTICE — Données tierces

Le **code** de Tally est sous GPLv3 (voir `LICENSE`). Les **données** ci-dessous restent sous
leur licence d'origine ; elles ne sont pas relicenciées sous GPLv3.

| Source | Statut | Licence | Mention obligatoire |
|---|---|---|---|
| Open Food Facts | Utilisée (requêtes réseau, copie locale des produits importés) | ODbL 1.0 (base), DbCL (contenu), CC BY-SA (images, non utilisées) | Voir ci-dessous |
| Table CIQUAL 2020 (Anses) | **Embarquée** (asset CSV converti, 2 298 aliments) | Licence Ouverte 1.0 (Etalab) | Voir ci-dessous |
| USDA FoodData Central | **Non utilisée** (réservée à un usage futur) | CC0 1.0 (domaine public) | Aucune (courtoisie : citer USDA FDC) |

## Open Food Facts

- Données : © Open Food Facts contributors, sous Open Database License (ODbL) 1.0.
  Contenu des fiches : Database Contents License (DbCL) 1.0.
- Site : <https://world.openfoodfacts.org> — licence : <https://opendatacommons.org/licenses/odbl/1-0/>
- Les aliments copiés depuis OFF portent `source = OFF` dans la base locale ; toute base dérivée
  utilisée publiquement (ODbL §4.4, à confirmer par un juriste) doit conserver cette attribution
  et la licence ODbL.
- Les images OFF (CC BY-SA 3.0) ne sont ni affichées ni stockées à ce jour.
- Requêtes identifiées par l'User-Agent `Tally/<version> (Android; +https://github.com/kerstz/poids)`.

## Table CIQUAL (Anses)

- Source : Anses, *Table de composition nutritionnelle des aliments Ciqual 2020*, version 2020-07-07,
  <https://ciqual.anses.fr>, jeu de données data.gouv.fr publié sous Licence Ouverte.
- Mention à afficher : « Source : Anses – Table de composition nutritionnelle des aliments Ciqual 2020,
  licence ouverte Etalab. Données modifiées par Tally (conversion de format). » (+ date de dernière mise à jour).
- Fichier : `data/src/main/assets/ciqual/ciqual_2020_07_07.csv`, produit par `tools/ciqual/convert.py` depuis
  `XML_2020_07_07.zip` (sha256 `cab13941…aa59ca8`) : conversion de format, virgule → point, « traces » et
  « < x » → 0, 887 aliments sans énergie exclus. Affichée dans l'onglet « Ciqual » de l'écran Ajout.
- Licence Ouverte 1.0 (Etalab, identifiant data.gouv `fr-lo`). Le passage à CIQUAL 2025 reste à vérifier :
  voir `docs/AUDIT.md` §8.1.

## USDA FoodData Central (si utilisée plus tard)

- Source : U.S. Department of Agriculture, Agricultural Research Service, FoodData Central,
  <https://fdc.nal.usda.gov>. Données du domaine public (CC0 1.0). Aucune attribution requise ;
  citer la source par courtoisie et indiquer la date d'extraction.

---

## Sauvegardes et exports

Une sauvegarde ZIP (`manifest.json`) contient, si l'utilisateur a utilisé ces sources, les attributions
Open Food Facts (ODbL-1.0) et Ciqual (Licence Ouverte 1.0). Les exports CSV portent une colonne `source`.

Ce fichier n'est pas un avis juridique ; les points ouverts sont listés dans `docs/AUDIT.md` §2.5 et §8.
