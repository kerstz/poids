#!/bin/sh
# Contrôle les métadonnées F-Droid (longueurs max : titre 50, court 80, long 4000, changelog 500).
set -e
base="$(dirname "$0")/../fastlane/metadata/android"
for loc in en-US fr-FR; do
  for spec in title.txt:50 short_description.txt:80 full_description.txt:4000 changelogs/1.txt:500; do
    f="${spec%%:*}"; max="${spec##*:}"
    [ -f "$base/$loc/$f" ] || { echo "manquant: $loc/$f"; exit 1; }
    n=$(wc -m < "$base/$loc/$f")
    [ "$n" -le "$max" ] || { echo "trop long: $loc/$f ($n > $max)"; exit 1; }
  done
done
echo "fastlane OK"
