# Recherche — Énergie d'activité : MET, équations ACSM, pas

Date du rapport : 2026-06-25
Périmètre : valider MetCalculator (MET × poids × h, net, ACSM vitesse) et StepDistanceCalculator (taille×0.41, 0.5 kcal/kg/km), + la stratégie anti double-comptage.
Standards Bureau : ≥8 sources primaires, double vérification, niveaux de confiance, dates, COI.

## TL;DR (verdicts)

| Sujet | Verdict |
|---|---|
| MET × poids × h | GARDER ; **net (MET−1) par défaut** ; correction RMR = amélioration forte |
| Valeurs MET défaut | **AJUSTER 4/7** (marche rapide, vélo, natation, course) |
| Équations ACSM | GARDER dans domaine de validité ; brancher sur l'allure en zone grise 5–8 km/h |
| Pas→distance ×0.41 / coût 0.5 | Facteur pas GARDER + cadence ; coût marche 0.5 / course 1.0 net VALIDÉ |
| **Geler les pas en session GPS** | **REMPLACER** : dédupliquer les CALORIES, pas les PAS (indéfendable) |

## P1 — Formule MET, net vs brut, correction RMR

- `kcal = MET × poids × h` = forme simplifiée d'ACSM. 1 MET = **1,05** kcal/kg/h, pas 1,0 → le `×1.0` surestime déjà ~5 %.
- Les MET du Compendium sont **bruts** (incluent le repos). Pour un bilan énergétique, le **net ≈ (MET−1)×kg×h** est physiologiquement correct (à 3 MET, le brut surévalue le net de 50 %). → **net par défaut**.
- Le « 1 MET = 3,5 mL/kg/min » dérive d'UN individu (Balke ~1960) et surestime le repos réel : Byrne 2005 (769 adultes) VO2 repos réel ≈ 2,6 → surestimation ~35 % du VO2, ~20 % des kcal, **pire chez surpoids/femmes/seniors**. Kozey 2010 : MET corrigé = MET × (3,5 / RMR_estimé en mL/kg/min) ; via RMR estimé (Mifflin/Harris-Benedict) ≈ aussi bon que RMR mesuré.
- → **Correction RMR personnelle recommandée** pour usage individuel.

Sources : Byrne (J Appl Physiol 2005) ; Kozey (J Phys Act Health 2010) ; Wilson (MSSE 2014) ; pacompendium.com corrected-METs 2024.

## P2 — Valeurs MET défaut vs Compendium 2024

Source : Herrmann/Willis/Ainsworth, « 2024 Adult Compendium of Physical Activities », J Sport Health Sci 2024;13(1):6-12.

| Activité | App | MET 2024 | Action |
|---|---|---|---|
| Marche (général) | 3.5 | 3.5 (17160) | OK |
| Marche rapide | 4.3 | 4.8 (17200) | **→ 4.8** |
| Course | 9.8 | 9.3 (12050, 6 mph) | → 9.3 (mineur) |
| Vélo | 8.0 | 7.0 général ; 8.0 = 12-14 mph | **→ 7.0** (sauf vitesse mesurée) |
| Rando | 6.0 | 6.0 (17080) | OK |
| Natation | 7.0 | 6.0 loisir ; ~9.8 laps | **→ 6.0** (ou scinder) |
| Autre | 4.0 | ~4.0 | OK |

Piège : agrégateurs commerciaux divergent → se fier au PDF primaire.

## P3 — Équations ACSM marche/course

Coefficients vérifiés (9e–11e éd.) : marche `0,1·v + 3,5` (plat) ; course `0,2·v + 3,5` ; 1 MET = 3,5.
Domaines : marche ~50–100 m/min (3–6 km/h) ; course ≥134 m/min (≥8 km/h). **Zone grise 5–8 km/h non fiable** (marche ou trot selon démarche). SEE ±3–6 mL/kg/min.
Alternatives : Pandolf 1977 (charges/pentes/descente) ; Ludlow-Weyand « Minimum Mechanics » 2016/2017 (r²=0,99, SEE≈1,06, ~4× mieux ; ACSM surprédit +13 % en extérieur). Pour marche/course à plat dans les bandes valides, ACSM acceptable. Pour pentes/charges, préférer Minimum Mechanics/Pandolf.
→ Brancher sur la cadence (timestamps des pas) en zone grise.

Sources : ACSM Guidelines ; Pandolf 1977 ; Ludlow-Weyand 2016/2017 ; Weyand 2021 ; Drain & Aisbett 2017 (indépendant).

## P4 — Longueur de pas et coût énergétique

- Facteur taille×0.415/0.413 : très répandu mais **origine primaire non vérifiée**. Corrélation taille↔foulée r≈0,68–0,71 (explique ~50 % variance). Erreur ±10–15 %/individu, ±20–30 % intra (dépendance vitesse ignorée). Mieux sans GPS : modèle **cadence-aware** `SL = a·cadence + b` (cadence dérivable de TYPE_STEP_COUNTER).
- Coût **marche net ≈ 0,5 kcal/kg/km** : VALIDÉ (courbe en U, min ~4–4,7 km/h). Coût **course net ≈ 1,0 kcal/kg/km, indépendant de la vitesse** (loi de Margaria 1963 ; Minetti 2002). → la distinction marche/course de l'app est justifiée (course ≈ 2× marche/km).

Sources : Margaria 1963 ; di Prampero 1986 ; Minetti 2002 ; PMC6473831 ; Sensors PMC5038701.

## P5 — Anti double-comptage : geler les pas (CONTREDIT la feature livrée)

- **Dédupliquer les CALORIES = correct** (ne pas additionner kcal-session MET + kcal-podomètre pour le même mouvement). C'est ce que fait Apple Health (déduplique l'énergie, JAMAIS les pas).
- **Geler le compteur de pas = indéfendable.** Toute la preuve « pas/jour » repose sur les **pas journaliers TOTAUX, exercice inclus** :
  - Saint-Maurice (JAMA 2020) : total pas/jour, course incluse ; 8000 vs 4000 → mortalité bien plus basse.
  - Paluch (Lancet Public Health 2022, méta 15 cohortes ~47 000) : pas totaux mesurés, jamais soustraits.
  - Tudor-Locke (10 000 pas ; cadence ≥100 pas/min = modéré) : la guidance veut justement les pas d'une marche rapide/course.
- Contre-exemple Garmin : compteur qui retombe à la sauvegarde = traité comme **bug** par les utilisateurs.
- **Risque confirmé** : exclure une sortie de 10 000 pas → la plus grosse séance affiche MOINS de pas qu'un jour sédentaire. Inversion de la dose-réponse, nombre non comparable aux repères (8000/10 000 pas).

**Correctif** : NE PAS geler. Les pas de la session entrent dans le total journalier. Dédupliquer au niveau **calories** : sur la fenêtre de session, calories MET de la session = autoritaire, retirer la contribution calorique podomètre de cet intervalle. Hors session, garder l'estimation podomètre.

Confiance **Haute**.

Sources : Saint-Maurice JAMA 2020 ; Paluch Lancet PH 2022 ; Tudor-Locke IJBNPA 2011 / BJSM 2018 ; Apple Support.

## Changements priorisés

- **P0** Arrêter de geler les pas. Déplacer la déduplication vers les calories (fenêtre de session : kcal MET session, retirer kcal podomètre).
- **P0** Ajuster 4 MET : marche rapide 4.3→4.8, vélo 8.0→7.0, natation 7.0→6.0, course 9.8→9.3.
- **P1** Calories en NET (MET−1) par défaut ; corriger le facteur 1,05 (ou via correction RMR).
- **P1** Correction RMR personnelle (× RMR_user/3,5).
- **P1** Zone grise 5–8 km/h : choisir marche/course selon cadence.
- **P2** Modèle de pas cadence-aware + calibration GPS unique. Scinder natation loisir/laps.
- **P3** Afficher distance/calories comme estimations ±10–15 %. Pandolf/Minimum Mechanics si pentes/charges deviennent un usage.

## Sources (datées)

Byrne 2005 · Kozey 2010 · Herrmann/Ainsworth 2024 · Wilson 2014 · Pandolf 1977 · Ludlow-Weyand 2016/2017 · Weyand 2021 · Margaria 1963 · di Prampero 1986 · Minetti 2002 · Saint-Maurice JAMA 2020 · Paluch Lancet PH 2022 · Tudor-Locke 2011/2018. COI : auteurs Compendium (réputationnel), Ludlow/Weyand (défendent leur modèle) — signalés.
