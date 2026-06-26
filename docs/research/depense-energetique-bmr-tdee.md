# Recherche — Dépense énergétique : BMR / TDEE / cible calorique

Date du rapport : 2026-06-25
Périmètre : valider les formules métaboliques codées dans l'app (Mifflin-St Jeor, multiplicateurs 1.2–1.9, règle 7700 kcal/kg, planchers 1200/1500).
Standards Bureau : ≥8 sources primaires, double vérification, niveaux de confiance, dates, conflits d'intérêt signalés.

## TL;DR (verdicts)

1. **BMR Mifflin-St Jeor → GARDER.** Meilleur choix par défaut population générale. Erreur ±10 % chez ~80 % des sujets. Confiance **Haute**.
2. **Multiplicateurs 1.2–1.9 → GARDER mais requalifier.** Dérivés des PAL FAO/WHO/UNU. Le **1.2 « sédentaire » est sous le plancher FAO (1.40)** → sous-estime le TDEE. Confiance **Moyenne**.
3. **Double comptage → BUG À CORRIGER (P0).** Ajouter l'exercice mesuré par-dessus un TDEE qui contient déjà un facteur d'activité = compter 2× l'activité. Confiance **Haute**.
4. **Règle 7700 kcal/kg → AJUSTER.** OK court terme, surestime la perte à moyen/long terme (ignore l'adaptation métabolique). Modèle adaptatif (NIH Body Weight Planner / maintenance inférée) supérieur. Confiance **Haute**.
5. **Planchers 1200/1500 → GARDER + garde-fou rythme.** Cohérents guidelines obésité. Ajouter limite de rythme ≤1 %/semaine. Confiance **Moyenne-Haute**.

## Q1 — Mifflin-St Jeor

GARDER comme défaut. Proposer Katch-McArdle (370 + 21,6·MM) UNIQUEMENT si masse maigre connue (sinon pas mieux que Mifflin). Pour app internationale, Henry/Oxford 2005 a un biais plus faible sur populations non-occidentales.
Erreur inter-individuelle ±10 % même pour la meilleure équation → afficher « estimation, ±10 % ».
Nuance : Mifflin moins précise chez l'obèse âgé (sous-groupe).

Sources : Frankenfield (J Am Diet Assoc, 2005, revue de référence) ; Mifflin (Am J Clin Nutr, 1990) ; Henry (Public Health Nutr, 2005) ; méta athlètes (PMC10687135, 2023).

## Q2 — Multiplicateurs d'activité

Conceptuellement des PAL (TDEE/BMR) du cadre FAO/WHO/UNU (1985, 2004, eau doublement marquée).
Fourchettes FAO 2004 : sédentaire/léger 1.40–1.69 ; modéré 1.70–1.99 ; vigoureux 2.00–2.40.
→ **Le 1.2 de l'app = alitement, pas vie sédentaire normale (réel ≈ 1.4)**. Sous-estime le TDEE.
Mieux : dériver le niveau d'activité des **pas/jour** déjà collectés (mais alors NE PAS aussi ajouter les kcal de marche — cf Q3). Incertitude TDEE cumulée ≈ ±15–20 %.

Sources : FAO/WHO/UNU Human Energy Requirements (2004) ; PMC3245484 (PAL↔pas).

## Q3 — DOUBLE COMPTAGE (point le plus important)

TDEE = BMR × multiplicateur **inclut déjà** l'activité (pour tout mult. > 1.2). Ajouter ensuite les kcal mesurées (podomètre + GPS) → activité comptée 2× → déficit sous-estimé → stagnation.

**Deux architectures cohérentes — choisir UNE :**
- **Option A (recommandée, l'app a déjà les capteurs)** : fixer le facteur de base au plancher (~1.2–1.3 = métabolisme + vie quotidienne hors sport), puis AJOUTER l'énergie mesurée (pas + GPS). Modèle additif (Apple/Fitbit séparent repos/actif). Dédupliquer pas vs sessions GPS et utiliser MET **net (MET−1)** pour ne pas recompter le repos.
- **Option B** : garder BMR × multiplicateur, NE RIEN ajouter.
- Jamais les deux.

Apps de référence : MacroFactor (maintenance adaptative inférée, pas de multiplicateur) ; MyFitnessPal (additif, mais surcrédite l'exercice → à ne pas copier naïvement) ; Cronometer (laisse choisir).

Confiance **Haute**.

## Q4 — Règle 7700 kcal/kg

Suppose dépense constante → faux (BMR, coût du mouvement et DIT baissent avec le poids : adaptation métabolique). Surestime la perte, écart croissant sur des mois.
Hall (Lancet 2011, base du NIH Body Weight Planner) : modèle dynamique ; ~10 kcal/j permanent ≈ 0,45 kg à terme, 50 % en ~1 an, 95 % en ~3 ans.
Améliorations :
1. Minimal : recalculer BMR/TDEE à chaque nouveau poids (Mifflin baisse mécaniquement). Garder 7700 pour le court terme.
2. Idéal (100 % local) : **maintenance adaptative inférée** = apport moyen loggé + (variation de poids lissée × 7700)/jours, sur 2–4 sem. Corrige adaptation + erreurs de formule + double comptage. Meilleur rapport précision/effort.

Sources : Hall (Lancet 2011) ; Thomas/Hall (Int J Obes 2013, « 3500 rule wrong ») ; Hall (Gastroenterology 2017).

## Q5 — Planchers 1200/1500

AHA/ACC/TOS 2013 (Circulation 2014) : restriction 1200–1500 F / 1500–1800 H. <800 kcal = VLCD médical supervisé. Donc 1200/1500 = planchers de prudence raisonnables (minima, pas cibles).
**Vrai garde-fou = le rythme** : ACSM Position Stand 2009 (Donnelly) déficit 500–1000 kcal/j → ~0,5–1 kg/sem ≈ **≤1 % du poids/semaine**.
Reco : garder planchers absolus + ajouter plancher relatif (déficit ≤~1000 kcal/j ET rythme ≤1 %·poids/sem) + avertissement si déficit agressif.

Sources : Jensen AHA/ACC/TOS (Circulation 2014) ; Donnelly ACSM (MSSE 2009) ; NICE VLCD.

## Changements priorisés

- **P0** Corriger le double comptage (Option A : base 1.2–1.3 + énergie mesurée nette ; OU Option B). Dédupliquer pas/GPS.
- **P1** Brider le rythme ≤1 %·poids/sem + garder planchers + avertissement.
- **P2** Maintenance adaptative inférée (remplacer la promesse statique 7700→X kg/sem). À défaut, recalcul à chaque poids + présenter le rythme comme estimation qui ralentit.
- **P3** Requalifier/relever le « 1.2 » (ou dériver des pas, sans redoubler le comptage). Afficher PAL ±15 %.
- **P4** Option Katch-McArdle si % masse grasse saisi. Mifflin par défaut.
- **P5** Transparence d'incertitude partout (±10 % BMR, ±15–20 % TDEE ; le rythme réel se vérifie sur la tendance de poids).

## Sources (datées)

Frankenfield 2005 · Mifflin 1990 · FAO/WHO/UNU 2004 · Hall Lancet 2011 · Thomas/Hall 2013 · ACSM Donnelly 2009 · AHA/ACC/TOS 2014 · Henry 2005 · méta athlètes 2023. Calculateurs commerciaux = tertiaires (COI commercial signalé), utilisés en recoupement uniquement.
