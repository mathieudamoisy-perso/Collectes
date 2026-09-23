---
name: deploy
description: >-
  Prépare une release Play Store Collectes : bump de version, build AAB,
  mise à jour README/PLAY_STORE/politique de confidentialité, notes de
  version Play Console, et e-mail d’annonce beta fermée prêt à coller
  (lien Play Store + liste BCC des testeurs).
  Use when the user runs /deploy or asks to release, publish, ship a beta,
  or generate an AAB for Collectes.
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
- [ ] 7. Livrer l’e-mail d’annonce beta (lien Play + liste BCC testeurs)
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
4. **Destinataires BCC** : liste §7.1 prête à coller en Cci (obligatoire à chaque deploy)
5. **E-mail beta** : objet + corps prêts à coller (voir §7) — le corps **doit** contenir le lien Play §7.2
6. **Docs** : liste des fichiers touchés (README, PLAY_STORE, privacy si applicable)
7. **Prochaines étapes manuelles** (courtes) :
   - (si privacy modifiée) push `main` pour GitHub Pages
   - Play Console → Tests fermés → Créer une version → uploader l’AAB → coller les notes
   - vérifier Data safety + URL privacy
   - envoyer l’e-mail aux testeurs (après publication de la version sur le track test fermé)
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

## 7. E-mail d’annonce — test fermé

À produire **systématiquement** en fin de deploy (même si Mathieu n’envoie pas tout de suite). Texte **prêt à copier-coller** dans Gmail (ou autre), en français, ton simple et personnel.

Chaque fin de deploy doit livrer **trois** blocs collables : **BCC** (§7.1) + **objet** + **corps** (avec lien §7.2).

### 7.1 Destinataires (BCC)

- **À** : laisser vide (ou Mathieu lui-même)
- **Cci (BCC)** : coller **toutes** les adresses ci-dessous (une par ligne ou séparées par des virgules selon le client mail)
- Ne **jamais** mettre les testeurs en **À** / **Cc** (respect de la vie privée entre testeurs)
- Ne **pas** recopier cette liste dans `README.md`, `PLAY_STORE.md`, privacy, ni dans le corps du mail aux testeurs
- N’envoyer **qu’après** que la version soit disponible sur le track test fermé (sinon le lien Play ne propose pas encore la MAJ)
- Si Mathieu ajoute/retire un testeur : **mettre à jour la liste ci-dessous** dans ce skill (source de vérité pour `/deploy`)

Liste BCC à coller (source de vérité) :

```
Angeline.diot@gmail.com
Supernaut870@gmail.com
audrey.dychus@gmail.com
axel.basle@live.fr
boeselthibaut@gmail.com
dychus.sylvie2@gmail.com
emiliekerguinas.ek@gmail.com
julien@boesel.fr
mdamoisy@gmail.com
melissarahmoune@gmail.com
nath1basle@gmail.com
paulwindev@gmail.com
richard.daffniet@gmail.com
sophie27godard@gmail.com
sylvain.montergous@gmail.com
vanessa@boesel.fr
```

Dans la réponse à Mathieu, afficher cette liste sous un titre du type **Destinataires (BCC)** / **Cci :** avant l’objet du mail.

Si Mathieu demande explicitement d’**envoyer** le mail via Gmail MCP : s’authentifier si besoin, puis brouillon/envoi avec BCC = liste §7.1. Sinon : uniquement le texte à coller.

### 7.2 Lien de téléchargement / mise à jour

Lien **obligatoire** dans le corps du mail (ne pas inventer d’autre URL) :

```
https://play.google.com/store/apps/details?id=com.collectes.app
```

Lien d’opt-in test fermé (optionnel, seulement si un testeur n’a pas encore rejoint le programme) :

```
https://play.google.com/apps/testing/com.collectes.app
```

### Format de la réponse (obligatoire)

Trois blocs distincts, texte brut (pas de markdown dans le corps collable) :

**Cci :**
```
(adresses de §7.1, une par ligne)
```

**Objet :**
```
Collectes X.Y.Z — nouveautés du test fermé
```

**Corps :**
```
Bonjour,

Une nouvelle version de Collectes (X.Y.Z) est dispo en test fermé sur le Play Store.

Nouveautés :
- Bullet utilisateur 1
- Bullet utilisateur 2
- …

Télécharger / mettre à jour depuis le téléphone :
https://play.google.com/store/apps/details?id=com.collectes.app

Après installation : ouvrir l’app et tirer pour rafraîchir le calendrier si besoin.

Un souci ou une idée ? Répondre à ce mail ou utiliser Contact développeur dans les réglages de l’app.

Merci pour vos retours,
Mathieu
```

### Règles de rédaction

- Reprendre les **mêmes bullets** que les notes Play Console (reformulation légère OK si plus naturelle en mail)
- 3 à 6 puces max ; pas de jargon technique
- Mentionner une **nouvelle commune** en premier si c’est le cas
- Si privacy / domaines changent : une phrase du type « la politique de confidentialité a été mise à jour »
- Toujours inclure le lien §7.2 dans le corps (bloc « Télécharger / mettre à jour… »)
- Toujours livrer la liste BCC §7.1 dans la réponse (hors corps du mail)
- Pas d’emojis sauf si Mathieu le demande

### Exemple de ton

```
Bonjour,

Une nouvelle version de Collectes (1.6.0) est dispo en test fermé sur le Play Store.

Nouveautés :
- Cabourg : nouvelle commune (calendrier Normandie Cabourg Pays d’Auge)
- Calendrier : ordures et emballages affichés correctement le même jour quand les deux sont collectés

Télécharger / mettre à jour depuis le téléphone :
https://play.google.com/store/apps/details?id=com.collectes.app

Après installation : ouvrir l’app et tirer pour rafraîchir le calendrier si besoin.

Un souci ou une idée ? Répondre à ce mail ou utiliser Contact développeur dans les réglages de l’app.

Merci pour vos retours,
Mathieu
```

## Hors scope

- Ne pas uploader vers Play Console (pas d’API)
- Ne pas committer / pusher sauf demande explicite
- Ne pas régénérer icônes / feature graphic / captures sauf demande
- Ne pas modifier le keystore ni committer `keystore.properties` / `*.jks`
- Ne pas envoyer l’e-mail beta sans demande explicite de Mathieu
