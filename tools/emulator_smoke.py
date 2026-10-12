#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Test de fumée sur émulateur démarré (voir tools/run-emulator.sh). Textes UI en anglais (locale en-US).

Parcours : journal vide → Ciqual « pomme » → ajout au journal → poids 72.5 → sauvegarde ZIP
→ restauration → annulation → écran de scan (permission). Échoue au premier écart ou crash.
Usage : python3 -I tools/emulator_smoke.py
"""
import os, re, subprocess, sys, time
import xml.etree.ElementTree as ET

ADB = [os.path.join(os.environ.get("ANDROID_HOME", os.path.expanduser("~/Android/Sdk")), "platform-tools", "adb")]


def sh(*a):
    return subprocess.run(ADB + list(a), capture_output=True, text=True).stdout


def nodes():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    out = []
    for n in ET.fromstring(sh("exec-out", "cat", "/sdcard/ui.xml")).iter("node"):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        out.append((n.get("text") or "", n.get("content-desc") or "", ((x1 + x2) // 2, (y1 + y2) // 2), n.get("class")))
    return out


def texts():
    return [t or d for t, d, _, _ in nodes() if t or d]


def tap(label, desc=False):
    for t, d, (x, y), _ in nodes():
        if (d if desc else t) == label:
            sh("shell", "input", "tap", str(x), str(y))
            time.sleep(1.5)
            return
    sys.exit(f"ÉCHEC: '{label}' introuvable. Écran: {texts()[:12]}")


def expect(fragment):
    if not any(fragment in t for t in texts()):
        sys.exit(f"ÉCHEC: '{fragment}' absent. Écran: {texts()[:12]}")


def no_crash():
    log = sh("logcat", "-d", "-s", "AndroidRuntime:E").strip()
    if log:
        sys.exit("ÉCHEC: crash\n" + log[-800:])


sh("shell", "pm", "clear", "org.tally.app")
sh("logcat", "-c")
sh("shell", "am", "start", "-W", "-n", "org.tally.app/.MainActivity")
time.sleep(3)
expect("Nothing logged")
tap("Add"); tap("Ciqual")
tap("Search"); sh("shell", "input", "text", "pomme"); time.sleep(2)
expect("Compote de pomme")
for t, _, (x, y), _ in nodes():
    if "kcal / 100" in t:
        sh("shell", "input", "tap", str(x), str(y)); break
time.sleep(2)
expect("100 g =")
tap("Add"); expect("Compote de pomme")
tap("Body"); tap("Add weight")
for _, _, (x, y), cls in nodes():
    if cls == "android.widget.EditText":
        sh("shell", "input", "tap", str(x), str(y)); break
sh("shell", "input", "text", "72.5"); tap("Save"); expect("72.5 kg")
tap("Settings"); sh("shell", "input", "swipe", "540", "1800", "540", "600", "300"); time.sleep(1)
tap("Back up"); tap("SAVE"); time.sleep(2); expect("Backup saved")
tap("Restore"); tap("tally-backup-" + time.strftime("%Y-%m-%d") + ".zip"); time.sleep(3); expect("Data restored")
tap("Undo"); time.sleep(2); expect("Previous data restored")
tap("Journal"); tap("Add")
tap("Scan a barcode", desc=True); time.sleep(2)
expect("Allow Tally")
no_crash()
print("OK")
