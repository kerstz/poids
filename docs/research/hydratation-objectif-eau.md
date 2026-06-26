# Recherche — Objectif d'hydratation quotidien (app santé « Poids »)

Date du rapport : 2026-06-25
Périmètre : valider/ajuster la formule `objectif = poids(kg) × 33 ml/kg` et le défaut 2000 ml.

---

## TL;DR (verdict)

**La formule 33 ml/kg N'EST PAS fausse, mais elle est MAL APPLIQUÉE.** Le « 30-35 ml/kg » est
une **heuristique clinique** (origine pédiatrique Holliday-Segar 1957, généralisée aux adultes en
nutrition clinique), pas une référence de population. Surtout, ce chiffre estime l'**eau TOTALE**
(boissons + aliments + eau métabolique), alors que l'app ne trace que la **BOISSON**. Résultat :
l'objectif affiché surestime la cible-boisson de ~20-30 %.

**Recommandation : GARDER la logique par-kg mais appliquer un facteur boisson ~0,75**, soit une
constante effective **~25 ml/kg pour la cible-boisson** (équivalent : `poids × 33 × 0,75`). Borner
[1200 ml ; 3500 ml]. Conserver 2000 ml comme défaut (cohérent avec EFSA/ANSES). Abandonner toute
promesse « 8 verres / 2 L pour tout le monde » (mythe).

Confiance globale : **Haute** sur les références de population (EFSA/IOM/ANSES), **Moyenne** sur le
calibrage exact du facteur per-kg.

---

## Q1 — La formule 30-35 ml/kg a-t-elle une base solide ?

**Réponse : heuristique clinique, pas une référence scientifique de population. Confiance : Haute.**

- Origine : **Holliday & Segar, 1957** (*Pediatrics*) — règle « 100 ml / 100 kcal », c.-à-d.
  ~1 ml d'eau par kcal dépensée, dérivée du métabolisme de l'enfant. Le « 30-35 ml/kg/j » adulte
  en est une simplification linéaire pour les besoins de maintenance (nutrition clinique, ESPEN).
- Ce que disent les **références de population** (qui NE sont PAS par-kg) :
  - **EFSA 2010** : Apport Adéquat (AI) d'**eau totale** = **2,0 L/j femme, 2,5 L/j homme** (climat
    tempéré, activité modérée). Base : apports observés + osmolarité urinaire cible ~500 mosm/L.
  - **NASEM/IOM 2004** : eau totale **3,7 L/j homme, 2,7 L/j femme** (valeurs US, plus hautes ;
    ~80 % via boissons, ~20 % via aliments).
  - **ANSES** : reprend EFSA (2,0 L femme / 2,5 L homme eau totale ; ~1,5-2 L de boisson/j).
  - **ESPEN gériatrie 2022** : utilise **30 ml/kg** comme repère MAIS le plafonne et le traduit en
    « offrir ≥1,6 L de boisson/j (femme), ≥2,0 L/j (homme) » — soulignant l'évaluation individuelle.
- **Per-kg vs valeur fixe par sexe** : EFSA a délibérément choisi des **valeurs fixes par sexe/âge**
  car le lien besoin-en-eau ↔ poids corporel est faible chez l'adulte sain ; l'eau suit surtout la
  **dépense énergétique / le métabolisme** et l'environnement, pas la masse. Le per-kg **surestime**
  chez les personnes en surpoids/obésité (le tissu adipeux est pauvre en eau).
  → **Per-kg = plus personnalisant dans la zone IMC normale, mais base de preuve plus faible** que
  les valeurs fixes EFSA, et risque de sur-prescription aux poids élevés.

Sources : EFSA 2010 (1459) ; NASEM/IOM 2004 ; ESPEN gériatrie 2022 ; Holliday & Segar 1957.

---

## Q2 — Eau TOTALE vs BOISSON (le point critique pour l'app)

**Réponse : les recommandations officielles portent sur l'eau TOTALE. La boisson ≈ 70-80 % de
l'eau totale. L'app doit appliquer un facteur ~0,75-0,8. Confiance : Haute.**

- **IOM 2004** : ~**80 %** de l'eau totale vient des boissons, ~**20 %** des aliments.
- **EFSA/ANSES** : eau totale 2,0/2,5 L → part boisson ≈ **1,6 L femme / 2,0 L homme** (cohérent
  avec ANSES « 1,5-2 L de boisson/j »).
- **Conséquence directe** : 33 ml/kg × 70 kg = 2310 ml. Si c'est présenté comme cible-**boisson**,
  on dépasse même la part-boisson d'un homme (2,0 L) → **surestimation ~15-30 %**.
- **Correctif** : cible-boisson = `eau_totale_estimée × 0,75` (milieu de 70-80 %).
  Avec 33 ml/kg comme proxy d'eau totale → **boisson ≈ 25 ml/kg**.
  Exemples (cible-boisson) : 50 kg→1250, 60 kg→1500, 70 kg→1750, 80 kg→2000, 90 kg→2250 ml.

Sources : IOM/NASEM 2004 ; EFSA 2010 ; ANSES.

---

## Q3 — Ajustements légitimes

**À intégrer (preuve solide) :** activité physique + chaleur. **À intégrer simplement :** sexe/âge.
**À traiter avec prudence :** grossesse/allaitement. Confiance : Haute (principe), Moyenne (chiffres).

- **Activité / sueur** : pertes sudorales 0,5-2 L/h selon intensité/chaleur (très variable).
  Règle app simple et sûre : **+ ~350-500 ml par tranche de 30-60 min d'exercice** (ne pas viser le
  remplacement intégral via l'objectif quotidien ; laisser la soif guider l'aigu).
- **Climat/chaleur** : majoration légitime ; règle app : **+10 à +20 %** par temps chaud/effort en
  chaleur (EFSA/IOM notent que leurs AI valent en climat tempéré, activité modérée).
- **Âge** : sujet âgé = soif émoussée → ESPEN insiste sur un **plancher** (≥1,6 L F / ≥2,0 L H de
  boisson) plutôt qu'un calcul agressif.
- **Grossesse** : EFSA **+300 ml/j** (eau totale → 2,3 L). **Allaitement** : EFSA **+700 ml/j**
  (→ 2,7 L). À convertir en boisson via ×0,75 si activé.
- **Poids élevé/obésité** : per-kg surestime → **caps** (plafond) indispensable, ou poids ajusté.

Sources : EFSA 2010 (grossesse/allaitement) ; ESPEN 2022 (âge) ; littérature sueur/EAH.

---

## Q4 — Bornes (plancher/plafond) et risque

**Réponse : OUI, borner. Plancher ~1200 ml, plafond ~3000-3500 ml/j de cible. Confiance : Haute.**

- **Hyponatrémie d'effort (EAH)** : cause = boire **au-delà** des pertes ; rein sain excrète
  ~0,5-1,0 L/h max. Boire >1,0-1,5 L/h dépasse la capacité d'excrétion. (Wilderness Medical Society,
  guideline 2021 ; littérature EAH).
- **Implication app** : ne jamais afficher un objectif qui pousse à boire trop vite/trop. Un objectif
  quotidien borné à ~3-3,5 L est largement suffisant et sûr pour un adulte ; au-delà, drapeau.
- **Plancher** : éviter d'afficher des cibles trop basses pour petits poids (50 kg × 25 = 1250 ml OK ;
  plancher 1200 ml).

Sources : Wilderness Medical Society EAH guideline 2021 ; revue EAH (PMC).

---

## Q5 — Le « 8 verres / 2 L par jour »

**Réponse : mythe quant à la règle universelle ; ~2 L de boisson reste un ordre de grandeur correct
pour une femme adulte. Confiance : Haute.**

- **Valtin 2002** (*Am J Physiol*) : aucune preuve contrôlée pour le « 8×8 » ; origine probable = note
  de 1945 du Food & Nutrition Board (2,5 L) dont la phrase « la majeure partie vient des aliments » a
  été oubliée.
- **Honnêteté UX** : présenter l'objectif comme **estimation personnalisée ajustable**, pas comme une
  vérité médicale ; rappeler que la **soif** et la **couleur des urines** sont de bons repères ; ne pas
  culpabiliser. 2000 ml par défaut = ordre de grandeur EFSA/ANSES défendable, pas une « loi ».

Sources : Valtin 2002 ; EFSA 2010 ; ANSES.

---

## Verdict sur la constante 33 ml/kg de l'app

| Aspect | Évaluation |
|---|---|
| Base scientifique du per-kg | Heuristique clinique (Holliday-Segar) — acceptable, base faible vs EFSA |
| Valeur 33 ml/kg comme eau TOTALE | Plausible (proxy raisonnable) |
| Valeur 33 ml/kg comme cible BOISSON | **Trop élevée (~+20-30 %)** — erreur de catégorie |
| Défaut 2000 ml | Bon (cohérent EFSA/ANSES femme) |
| Bornes | **Manquantes** — à ajouter |

**Décision : AJUSTER (ne pas remplacer).** Garder le per-kg pour la personnalisation, mais appliquer
le facteur boisson.

---

## Changements concrets priorisés (pour le calcul d'objectif)

1. **[P0] Appliquer un facteur boisson 0,75** : `objectifBoisson = poids × 33 × 0,75` (≈ 25 ml/kg).
   Ou exposer `ML_PER_KG_TOTAL = 33` et `BEVERAGE_FACTOR = 0.75`. (Corrige la surestimation.)
2. **[P0] Borner l'objectif** : `coerceIn(1200, 3500)` ml.
3. **[P1] Conserver 2000 ml par défaut** (poids inconnu) — déjà bon ; éventuellement 1600 F / 2000 H
   si le sexe est connu.
4. **[P1] Ajustement activité** : option « +500 ml » par séance de sport (additif, optionnel).
5. **[P2] Ajustement chaleur** : toggle « temps chaud » → +15 %.
6. **[P2] Grossesse/allaitement** : +300 / +700 ml (× facteur boisson) si renseigné.
7. **[P2] UX honnête** : libellé « estimation personnalisée, ajustable » + repère soif/urines ;
   retirer toute formulation « il faut 2 L / 8 verres ».
8. **[P3] Garde-fou poids élevé** : au-dessus d'un IMC élevé, plafonner ou utiliser poids ajusté.

---

## Matrice qualité des sources

| Source | Type | Date | Crédibilité | Conflit d'intérêt |
|---|---|---|---|---|
| EFSA, *Scientific Opinion on DRV for water*, EFSA Journal 8(3):1459 | Primaire officiel | 2010 | Très haute | Aucun |
| NASEM/IOM, *DRI for Water, Potassium, Sodium, Chloride, Sulfate* | Primaire officiel | 2004-2005 | Très haute | Aucun |
| ESPEN, *Practical guideline: Clinical nutrition & hydration in geriatrics* | Primaire (guideline) | 2022 | Haute | Aucun |
| ANSES, *Actualisation des références nutritionnelles* | Primaire officiel (FR) | 2016/2021 | Haute | Aucun |
| Valtin, *« Drink 8 glasses »? Really?*, Am J Physiol | Revue (primaire littérature) | 2002 | Haute | Aucun |
| Holliday & Segar, *Pediatrics* | Primaire historique | 1957 | Haute (mais pédiatrique) | Aucun |
| Wilderness Medical Society, *EAH guideline* (AAFP synthèse) | Primaire (guideline) | 2021 | Haute | Aucun |
| CDC NCHS, *Daily Water Intake US* | Données officielles | 2009-2012 | Haute | Aucun |

**Note conflits d'intérêt** : se méfier des contenus « hydratation » publiés par l'industrie de l'eau
embouteillée (ex. IBWA, associations d'eaux minérales) qui tendent à majorer les besoins ; non utilisés
comme source primaire ici.

---

## Bibliographie (liens)

- EFSA 2010 : https://efsa.onlinelibrary.wiley.com/doi/10.2903/j.efsa.2010.1459
- NASEM/IOM 2004 : https://www.nationalacademies.org/read/10925/chapter/6
- ESPEN gériatrie 2022 : https://www.clinicalnutritionjournal.com/article/S0261-5614(22)00034-6/fulltext
- ANSES (références nutritionnelles) : https://www.anses.fr/fr/system/files/NUT2012SA0103Ra-2.pdf
- ANSES (eau de boisson) : https://www.anses.fr/en/content/good-practices-drinking-water-consumption
- Valtin 2002 : https://journals.physiology.org/doi/full/10.1152/ajpregu.00365.2002
- Holliday-Segar 1957 (synthèse) : https://en.wikipedia.org/wiki/Holliday-Segar_formula
- EAH / Wilderness Medical Society 2021 : https://www.aafp.org/pubs/afp/issues/2021/0215/p252.html
- EAH revue : https://pmc.ncbi.nlm.nih.gov/articles/PMC6735969/
- CDC NCHS water intake : https://www.cdc.gov/nchs/products/databriefs/db242.htm
