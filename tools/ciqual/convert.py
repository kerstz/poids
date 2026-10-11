#!/usr/bin/env python3
"""Convertit la table CIQUAL 2020 (XML Anses) en CSV UTF-8 pour l'app.

Usage : python3 -I tools/ciqual/convert.py XML_2020_07_07.zip data/src/main/assets/ciqual/ciqual_2020_07_07.csv

Source : https://ciqual.anses.fr/cms/sites/default/files/inline-files/XML_2020_07_07.zip
sha256 attendu de l'archive : cab13941ca693b7007c2f0fed2288fd5b94073fe6d598c9773297cf37aa59ca8
Licence des données : Licence Ouverte 1.0 (Etalab) — voir NOTICE.md. Stdlib seulement.

Règles : virgule décimale -> point ; « traces » et « < x » -> 0 ; « - » ou vide -> inconnu (vide).
Énergie : const 328 (UE 1169/2011) ; ~870 aliments n'ont aucune énergie (328 et 333 à « - ») : exclus, pas de calcul inventé.
Une ligne dont énergie, protéines, glucides ou lipides est inconnue est exclue (comptée sur stderr).
"""
import csv
import hashlib
import re
import sys
import zipfile

EXPECTED_SHA256 = "cab13941ca693b7007c2f0fed2288fd5b94073fe6d598c9773297cf37aa59ca8"
# const_code CIQUAL -> colonne
CONSTS = {
    328: "energy_kcal",    # Energie, Règlement UE N° 1169/2011 (kcal/100 g)
    25000: "protein_g",    # Protéines, N x facteur de Jones
    31000: "carbs_g",      # Glucides
    40000: "fat_g",        # Lipides
    32000: "sugars_g",     # Sucres
    40302: "sat_fat_g",    # AG saturés
    34100: "fiber_g",      # Fibres alimentaires
    10004: "salt_g",       # Sel chlorure de sodium
}
REQUIRED = ("energy_kcal", "protein_g", "carbs_g", "fat_g")
COLUMNS = ["code", "name", "group_code", "energy_kcal", "protein_g", "carbs_g", "fat_g",
           "sugars_g", "sat_fat_g", "fiber_g", "salt_g"]


def blocks(text, tag):
    return re.findall(rf"<{tag}>(.*?)</{tag}>", text, flags=re.S)


def field(block, name):
    m = re.search(rf"<{name}>(.*?)</{name}>", block, flags=re.S | re.I)
    return m.group(1).strip() if m else None


def parse_value(raw):
    if raw is None:
        return None
    v = raw.strip().lower()
    if v in ("", "-"):
        return None
    if v == "traces" or v.startswith("<"):
        return 0.0
    try:
        return float(v.replace(",", "."))
    except ValueError:
        return None


def main(zip_path, out_path):
    data = open(zip_path, "rb").read()
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED_SHA256:
        print(f"sha256 inattendu : {digest}", file=sys.stderr)
        sys.exit(1)
    with zipfile.ZipFile(zip_path) as z:
        def read(prefix):
            name = next(n for n in z.namelist() if n.startswith(prefix))
            return z.read(name).decode("cp1252")
        alim = read("alim_2")
        compo = read("compo_")

    values = {}
    for b in blocks(compo, "COMPO"):
        code, const = field(b, "alim_code"), field(b, "const_code")
        if code is None or const is None or not const.isdigit() or int(const) not in CONSTS:
            continue
        values.setdefault(int(code), {})[CONSTS[int(const)]] = parse_value(field(b, "teneur"))

    rows, excluded = [], 0
    for b in blocks(alim, "ALIM"):
        code = int(field(b, "alim_code"))
        name = re.sub(r"\s+", " ", field(b, "alim_nom_fr") or "").strip()
        v = values.get(code, {})
        if not name or any(v.get(c) is None for c in REQUIRED):
            excluded += 1
            continue
        row = [code, name, field(b, "alim_grp_code") or ""]
        row += ["" if v.get(c) is None else repr(round(v[c], 4)) for c in COLUMNS[3:]]
        rows.append(row)
    rows.sort(key=lambda r: r[0])

    with open(out_path, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(COLUMNS)
        w.writerows(rows)
    print(f"{len(rows)} aliments écrits, {excluded} exclus (macros principales inconnues)", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
