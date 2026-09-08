---
name: deploy
description: >-
  Prépare une release Play Store Collectes : bump de version, build AAB,
  mise à jour README/PLAY_STORE/politique de confidentialité, et notes de
  version à coller dans Play Console. Use when the user runs /deploy or asks
  to release, publish, ship a beta, or generate an AAB for Collectes.
disable-model-invocation: true
---

# Deploy Collectes (Play Store)

Workflow de release pour l’app Android **Collectes**. Exécuter toutes les étapes dans l’ordre. Ne pas committer ni pousser sauf si Mathieu le demande explicitement.

## Checklist

```
Deploy Progress:
- [ ] 1. Analyser les changements depuis la dernière version
- [ ] 2. Confirmer / appliquer le bump de version
- [ ] 3. Mettre à jour la documentation (README + PLAY_STORE)
- [ ] 4. Mettre à jour la politique de confidentialité si besoin
- [ ] 5. Builder l’AAB release
- [ ] 6. Livrer les notes de version (copie-coller) + résumé
```

## 1. Analyser les changements

1. Lire la version actuelle dans `android/app/build.gradle.kts` (`versionCode`, `versionName`).
2. Comparer avec `README.md` et `android/store/PLAY_STORE.md` (doivent être alignés).
3. Déterminer le contenu de la release :
   - `git log` depuis le dernier commit de bump de version (messages du type `Update version to…` / `Prepare release…`)
   - `git diff` / fichiers touchés (communes, UI, sync, permissions, sources réseau)
4. Synthétiser **3 à 6 bullets utilisateurs** (français, ton Play Store : bénéfice concret, pas jargon technique).

Sources de vérité communes / domaines réseau :
- `android/app/src/main/java/com/collectes/app/data/VexinCommunes.kt`
- `WasteGuideTerritory.kt`, fetchers SMIRTOM / CCVT / mairies

## 2. Bump de version

Règles :
- `versionCode` : **+1** à chaque AAB destiné à Play Console (strictement croissant)
- `versionName` : semver affiché (`MAJOR.MINOR.PATCH`)
  - bugfix / polish → patch (`1.5.1` → `1.5.2`)
  - nouvelle commune / feature visible → minor (`1.5.1` → `1.6.0`)
  - breaking / refonte majeure → major

Avant de modifier :
1. Proposer à Mathieu le couple `(versionCode, versionName)` et un résumé des notes.
2. Si `/deploy 1.6.0` (ou équivalent) est fourni, utiliser ce `versionName` et `versionCode = actuel + 1`.
3. Si non précisé : proposer un bump raisonnable et **attendre confirmation** avant d’écrire les fichiers.

Appliquer ensuite dans `android/app/build.gradle.kts` uniquement :
```kotlin
versionCode = N
versionName = "X.Y.Z"
```

Avant build multi-poste : rappeler `git pull` pour éviter un `versionCode` déjà utilisé.

## 3. Documentation à mettre à jour

### `README.md` (doc utilisateur final uniquement)
- Tableau **Communes prises en charge** si une commune a été ajoutée/retirée
- Introduction / fonctionnalités / premier lancement / réglages seulement si le comportement produit change vraiment
- Ne pas y remettre version, build, keystore, structure projet ou tests

### `android/store/PLAY_STORE.md`
- En-tête **Version actuelle**
- Textes fiche store (description courte/longue) si communes ou positionnement changent
- **Section Data safety / domaines** si nouvelle source réseau
- Ajouter une section **Notes de version — test fermé X.Y.Z** **en tête** des notes (garder l’historique en dessous)
- Ne pas mettre à jour la section **Parcours Console**

Format des notes dans `PLAY_STORE.md` :

```markdown
### Notes de version — test fermé X.Y.Z

À coller dans Play Console → Tests fermés → Notes de version :

\`\`\`
Bullet 1.
Bullet 2.
\`\`\`
```

Chaque ligne = une phrase courte, sans markdown, prête à coller dans Play Console.

## 4. Politique de confidentialité

Fichier : `docs/privacy-policy.html`  
URL publique (ne pas changer sans raison) :
`https://mathieudamoisy-perso.github.io/Collectes/privacy-policy.html`

**Mettre à jour si** l’une de ces conditions est vraie :
- nouvelle commune ou nouveau domaine de téléchargement / info
- changement de permissions ou de données stockées
- changement de comportement réseau (analytics, etc. — aujourd’hui : aucun)

Sinon : **ne pas** toucher le HTML (pas de bump de date cosmétique).

Quand une MAJ est nécessaire :
1. Mettre à jour la date « Dernière mise à jour » (jour du deploy, format français : `8 septembre 2026`)
2. Aligner la liste des domaines dans **Réseau** avec les sources réelles du code
3. Aligner permissions / données locales si besoin
4. Aligner la section Data safety de `PLAY_STORE.md` avec le même contenu

Rappeler en fin de deploy : **pousser `docs/privacy-policy.html` sur `main` avant de publier la beta** pour que GitHub Pages soit à jour.

## 5. Build AAB

Prérequis (vérifier, ne pas committer) :
- `android/keystore.properties` présent (depuis `keystore.properties.example`)
- `android/keystore/upload.jks` présent

Windows (machine de Mathieu) :

```powershell
cd android
.\gradlew.bat :app:bundleRelease
```

Unix :

```bash
cd android
./gradlew :app:bundleRelease
```

Artefact attendu :
`android/app/build/outputs/bundle/release/app-release.aab`

En cas d’échec :
- relayer l’erreur Gradle clairement
- ne pas inventer un AAB
- proposer le correctif minimal (keystore manquant, JDK, etc.)

Optionnel si le diff touche la logique métier : `.\gradlew.bat test` avant le bundle. Ne pas bloquer le deploy sur des tests non liés sans demander.

## 6. Livrable final (réponse à Mathieu)

Structurer la réponse ainsi :

1. **Version** : `X.Y.Z` (`versionCode` N)
2. **AAB** : chemin absolu ou relatif du fichier généré + confirmation OK
3. **Notes de version** : bloc prêt à coller (fenced, sans backticks internes) — **exactement** le texte mis dans `PLAY_STORE.md`
4. **Docs** : liste des fichiers touchés (README, PLAY_STORE, privacy si applicable)
5. **Prochaines étapes manuelles** (courtes) :
   - (si privacy modifiée) push `main` pour GitHub Pages
   - Play Console → Tests fermés → Créer une version → uploader l’AAB → coller les notes
   - vérifier Data safety + URL privacy
   - commit/push du bump de version si souhaité (ne pas le faire automatiquement)

### Style des notes

- Français
- Phrases utilisateur finales (pas « refactor ViewModel »)
- Une idée par ligne
- Exemple de ton :

```
Bouconvillers : nouvelle commune disponible (calendrier CCVT / Vexin-Thelle).
Interface : barre de navigation plus stable au défilement.
```

## Hors scope

- Ne pas uploader vers Play Console (pas d’API)
- Ne pas committer / pusher sauf demande explicite
- Ne pas régénérer icônes / feature graphic / captures sauf demande
- Ne pas modifier le keystore ni committer `keystore.properties` / `*.jks`
