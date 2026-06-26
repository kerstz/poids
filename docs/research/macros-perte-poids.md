# Recherche — Cibles de macronutriments en perte de poids

Date du rapport : 2026-06-25
Périmètre : l'app affiche les macros INGÉRÉES mais ne propose AUCUNE cible. Définir des cibles défendables (protéine prioritaire) pour la phase de déficit.
Standards Bureau : ≥8 sources primaires, double vérification, niveaux de confiance, dates, COI.

## TL;DR (à implémenter)

1. **Protéine = à fixer en priorité.** **1,6–2,2 g/kg/j** sur un **poids de référence** (poids actuel si IMC<25 ; poids cible si IMC≥25). Défaut **1,8 g/kg**. Confiance **Haute**.
2. **Lipides = plancher** : `max(0,8 g/kg ; 20 % des kcal)` (fonction hormonale). Confiance **Moyenne-Haute**.
3. **Glucides = le reste.** À calories + protéines égales, le ratio glucides/lipides n'a pas d'effet indépendant prouvé sur la perte de gras → laisser libre (bornes AMDR). Confiance **Haute**.
4. **Fibres = 14 g/1000 kcal** (plancher ~25 g/j). Confiance **Haute**.
5. **Oui, afficher des cibles en grammes** (pas juste l'ingéré), protéine en accent. Confiance **Haute**.

## Q1 — Protéine en déficit

1,6–2,2 g/kg de poids de référence ; défaut 1,8 ; plancher 1,6.
- ISSN Position Stand (Jäger 2017) : 1,4–2,0 g/kg actifs ; plus en restriction (jusqu'à 2,3–3,1).
- Morton (BJSM 2018, méta 49 essais) : plateau des gains de masse maigre ~1,6 g/kg (IC jusqu'à ~2,2).
- Helms (IJSNEM 2014) : 2,3–3,1 g/kg de MASSE MAIGRE chez athlètes maigres en déficit sévère.
- **Base de poids (point pratique)** : multiplier par le poids actuel d'une personne obèse gonfle la cible (la masse grasse n'a pas besoin de protéine). → IMC<25 : poids actuel ; IMC≥25 : poids cible (ou poids ajusté = cible + 0,25·(actuel−cible)). g/kg de masse maigre = plus juste si % gras connu.

## Q2 — Glucides/lipides

AMDR (IOM/NASEM 2005) : glucides 45–65 %E, lipides 20–35 %E, protéines 10–35 %E (EFSA 2010 confirme lipides 20–35 %E). En hyperprotéiné de perte de poids, dépasser 35 %E protéine sur de courtes périodes est acceptable (sain ; cf Q5).
Plancher lipidique hormonal : `max(20 % kcal ; 0,6–0,8 g/kg)`. Whittaker 2021 / Soltani 2025 : régimes très pauvres en lipides baissent la testostérone (effet réel mais modeste, surtout hommes).
**Ne pas imposer de répartition** : Aragon (ISSN 2017) — à calories + protéines égales, aucune répartition glucides/lipides n'est supérieure. Glucides = variable libre.

## Q3 — Fibres

14 g/1000 kcal (IOM AI 2005, basé sur baisse du risque coronarien) ; ~25 g/j F, ~38 g/j H. Academy of Nutrition and Dietetics (Dahl & Stewart, JAND 2015) confirme. En déficit, ancrer sur les kcal cibles avec plancher ~25 g (satiété, faible densité énergétique).

## Q4 — Afficher des cibles ?

Oui. Leidy (AJCN 2015) : protéine → satiété/adhésion. Refalo (méta-régression 2025) : relation dose-réponse protéine↔rétention masse maigre → l'utilisateur a besoin d'une **cible chiffrée**, pas juste l'ingéré a posteriori. Glucides/lipides = cibles « souples ».

## Q5 — Avertissements

- Non-avis médical (repères populationnels).
- **Insuffisance rénale (MRC)** : protéine élevée validée chez l'adulte SAIN (Devries, J Nutr 2018 — pas d'effet sur le DFG) ; contre-indiquée en MRC → avertissement + consulter.
- Exclus : grossesse/allaitement, adolescents, TCA (une app de cibles chiffrées peut être contre-indiquée), diabète traité, IMC très bas.
- Détecter le cas « déficit trop sévère » (protéine + lipides plancher > kcal) et alerter au lieu d'afficher des glucides négatifs.

## Méthode de calcul proposée (protéine → lipides → glucides)

```
# entrées : C = kcal cibles ; W = poids ; Wt = poids cible ; IMC
W_ref = (IMC < 25) ? W : Wt           # ou poids ajusté = Wt + 0,25*(W-Wt)
P_g   = 1.8 * W_ref                    # réglable 1.6–2.2 ; 4 kcal/g
F_g   = max(0.8 * W_ref, 0.20 * C / 9) # plancher ; 9 kcal/g
if (4*P_g + 9*F_g > 0.95*C): alerte "déficit trop sévère"; réduire P vers 1.6 ou remonter C
G_g   = (C - 4*P_g - 9*F_g) / 4        # le reste
Fib_g = max(14 * C/1000, 25)
```

Exemple — femme 80 kg, cible 70 kg, IMC 29, C=1600 : W_ref=70 → P=126 g (504 kcal), F=56 g (504), G=148 g (592), fibres ~25 g.
Affichage : barre ingéré/cible par macro, **protéine en couleur d'accent** (prioritaire), glucides/lipides en cible souple.

## Changements priorisés

- **P0** Cible protéine en g (1,8 g/kg réglable × poids de référence), barre ingéré/cible en accent.
- **P0** Avertissement médical + flag insuffisance rénale.
- **P1** Plancher lipidique max(0,8 g/kg ; 20 % kcal) + garde-fou « déficit trop sévère ».
- **P1** Glucides = reste (cible souple) + bornes AMDR informatives.
- **P2** Cible fibres 14 g/1000 kcal, plancher 25 g.
- **P3** Curseur lipides/glucides optionnel ; base masse maigre si % gras saisi (2,3–3,1 g/kg FFM).

## Sources (datées)

IOM/NASEM DRI 2005 · EFSA 2010 · ISSN Jäger 2017 · ISSN Aragon 2017 · Morton BJSM 2018 · Helms IJSNEM 2014 · Devries J Nutr 2018 · Leidy AJCN 2015 · Dahl & Stewart JAND 2015 · Whittaker 2021 / Soltani 2025 · Refalo 2025.
COI signalés : ISSN (revue interne/liens industrie), Leidy (financements filières) — pondérés ; Morton/Devries/IOM/EFSA indépendants (poids de preuve le plus élevé).
