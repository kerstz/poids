# Synthèse — Adaptations de l'app fondées sur la recherche

Date : 2026-06-25
Sources : 4 rapports `docs/research/` (dépense énergétique, énergie d'activité, hydratation, macros), ≥40 sources primaires datées.
But : traduire les verdicts scientifiques en changements concrets, priorisés, pour l'app.

Légende confiance : 🟢 Haute · 🟡 Moyenne-Haute · 🟠 Moyenne.

---

## P0 — Justesse scientifique (vrais défauts, à corriger)

### 1. Double comptage de l'activité 🟢
`HomeViewModel.netKcal = intake − burned`, où `burned = sessions + pas`, MAIS le TDEE (`CalorieTargetCalculator`) contient déjà le multiplicateur d'activité (1.2–1.9). L'activité est comptée deux fois → déficit sous-estimé → stagnation.
**Fix** : choisir une architecture.
- Option A (recommandée, l'app a les capteurs) : facteur de base ≈ 1.2–1.3 « hors sport » + énergie mesurée nette ajoutée.
- Option B : garder le multiplicateur, ne plus ajouter les kcal mesurées au bilan.

### 2. Geler les pas pendant une session GPS — CONTREDIT feature ① livrée 🟢
Le commit `32ee9cc` gèle l'accumulation des pas pendant une session GPS. La recherche (Saint-Maurice JAMA 2020, Paluch Lancet PH 2022, Tudor-Locke) montre que les repères « pas/jour » comptent les pas **totaux, sport inclus** → geler inverse la dose-réponse (grosse séance = moins de pas qu'un jour sédentaire).
**Fix** : NE PAS geler. Garder les pas de session dans le total. Dédupliquer au niveau **calories** : sur la fenêtre de session, kcal MET de la session = autoritaire, retirer la contribution calorique podomètre de cet intervalle. Résout aussi le point 1 côté mesure.

### 3. Valeurs MET obsolètes (Compendium 2024) 🟢
`ActivityType` : marche rapide **4.3→4.8**, vélo **8.0→7.0**, natation **7.0→6.0**, course **9.8→9.3**.

### 4. Objectif d'hydratation surestimé ~25 % 🟢
`WaterGoalCalculator` : 33 ml/kg estime l'eau TOTALE, l'app ne trace que la BOISSON (~75 % du total).
**Fix** : `objectif = poids × 33 × 0,75` (≈25 ml/kg boisson). Borner `[1200 ; 3500]` ml. Défaut 2000 conservé.

---

## P1 — Améliorations à fort appui

### 5. Calories d'activité en NET (MET−1) par défaut 🟢
`MetCalculator` a déjà l'option `net` ; les sessions GPS l'utilisent (`net=true`). Vérifier que TOUT passe en net (y compris l'estimation effort manuel — déjà le cas) et que le coût des pas est net. Corriger le facteur 1 MET = 1,05 kcal/kg/h.

### 6. Garde-fou rythme de perte 🟡
`UserProfile.weeklyRateKg` non borné. Limiter à ≤1 %·poids/semaine (et déficit ≤~1000 kcal/j), garder planchers 1200/1500, avertir si agressif.

### 7. Cibles de macronutriments 🟢
Ajouter une cible protéine (1,8 g/kg réglable × poids de référence : actuel si IMC<25, cible si ≥25), plancher lipides `max(0,8 g/kg ; 20 % kcal)`, glucides = reste, fibres 14 g/1000 kcal (plancher 25). Barre ingéré/cible, protéine en accent. Avertissement médical + flag insuffisance rénale.

### 8. Correction RMR du MET 🟡
`MET_corrigé = MET × (3,5 / RMR_user en mL/kg/min)`, RMR via Mifflin déjà calculé. Réduit la surestimation pour surpoids/femmes/seniors.

---

## P2 — Précision / robustesse (optionnel)

### 9. Maintenance adaptative inférée 🟡
Remplacer la promesse statique 7700→X kg/sem par un TDEE inféré (tendance de poids lissée 2–4 sem + apport loggé). 100 % local. À défaut, recalculer BMR/TDEE à chaque nouveau poids et présenter le rythme comme estimation qui ralentit.

### 10. Niveau d'activité dérivé des pas 🟠
Relibeller/relever le « 1.2 » (sédentaire réel ≈ 1.4 FAO) ou dériver le PAL des pas/jour (cohérent avec Option A si pas de double comptage).

### 11. Allure en zone grise 5–8 km/h 🟡
Choisir équation ACSM marche vs course selon la cadence (timestamps des pas), pas la seule vitesse.

### 12. Modèle de pas cadence-aware 🟠
`SL = a·cadence + b` au lieu du facteur taille fixe ; idéalement une calibration GPS unique (±5 % vs ±15 %).

---

## P3 — Honnêteté / UX (transversal)

### 13. Transparence d'incertitude 🟢
Afficher partout « estimation » : BMR ±10 %, TDEE ±15–20 %, distance/calories ±10–15 %, objectif d'eau personnalisé/ajustable (retirer toute formule « il faut 2 L / 8 verres »).

### 14. Ajustements hydratation optionnels 🟡
Toggle activité (+500 ml/séance), chaleur (+15 %), grossesse/allaitement (+300/+700 ml ×0,75).

### 15. Option BMR masse maigre 🟢
Bascule Katch-McArdle si l'utilisateur saisit son % de masse grasse. Mifflin par défaut.

---

## Note de conflit explicite

La feature ① « geler les pas pendant le sport » (choisie par l'utilisateur via clarification, commit `32ee9cc`) est **contredite par la littérature**. L'intention (éviter le double comptage **calorique**) était juste ; le **moyen** (geler les pas) est faux. La correction (point 2) préserve l'intention en déplaçant la déduplication sur les calories — et résout du même coup le point 1.
