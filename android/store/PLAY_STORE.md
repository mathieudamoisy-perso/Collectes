# Publication Google Play — Collectes

Application : **Collectes** (`com.collectes.app`)  
Version actuelle : `1.5.0` (`versionCode` 8)

## 1. Compte développeur

1. Créer un compte sur [Google Play Console](https://play.google.com/console) (~25 USD, une fois).
2. Remplir le profil développeur (identité, contact).
3. Créer une app **Collectes**, gratuite, langue par défaut français.

Les comptes personnels récents doivent souvent passer par un **test fermé** (testeurs réels, ~14 jours) avant la production. Prévoir des e-mails de proches.

## 2. Signature (upload key)

Le keystore n’est **pas** dans git. Il est généré localement :

- Fichier : `android/keystore/upload.jks`
- Config : `android/keystore.properties` (copier depuis `android/keystore.properties.example`)

**Sauvegarde obligatoire** du `.jks` + des mots de passe (coffre-fort, disque hors-ligne). Perdre cette clé empêche de publier des mises à jour avec la même clé d’upload. Play App Signing garde la clé de distribution côté Google.

## 3. Build AAB

```bash
cd android
./gradlew :app:bundleRelease
```

Sur Windows :

```powershell
cd android
.\gradlew.bat :app:bundleRelease
```

Fichier à envoyer : `android/app/build/outputs/bundle/release/app-release.aab`

### Numéro de version (bureau + domicile)

La version est définie **une seule fois** dans `android/app/build.gradle.kts` (fichier versionné dans git) :

- `versionCode` : entier **strictement croissant** à chaque upload Play Store (2, 3, 4…)
- `versionName` : version affichée aux utilisateurs (ex. `1.5.0`)

**Workflow multi-postes :**

1. Avant de builder : `git pull` pour récupérer le dernier `versionCode`
2. Si un nouvel upload est nécessaire : incrémenter `versionCode` (+1) et ajuster `versionName` si besoin
3. Committer et pousser ce changement **avant** de builder sur l’autre machine

Ne jamais uploader deux AAB avec le même `versionCode` : Play Console le refuse.

## 4. Assets fiche store

| Asset | Fichier | Format |
|-------|---------|--------|
| Icône | [ic_launcher_play_store_512.png](ic_launcher_play_store_512.png) | 512×512 |
| Bannière | [feature_graphic_1024x500.png](feature_graphic_1024x500.png) | 1024×500 |
| Captures téléphone | à prendre sur appareil / émulateur | min. 2, idéalement 4–8 |

Captures utiles : accueil « Demain », liste des prochaines collectes, guide du tri, réglages (sélecteur de commune + lien calendrier), filtre par type de bac.

### Textes FR (à coller dans la Console)

**Titre :** Collectes

**Description courte :**
Rappels la veille des collectes de déchets, selon votre commune.

**Description longue (À propos de l'application) :**
Collectes vous rappelle la veille de sortir les bacs, pour les communes prises en charge dans le Vexin, à Sannois et à Ermont.

L’application télécharge le calendrier officiel de votre commune, affiche la collecte de demain et les prochaines dates (ordures, emballages, verre, encombrants, végétaux selon les jours prévus), et envoie une notification locale à l’heure que vous choisissez.

Un guide du tri intégré indique quoi mettre (ou ne pas mettre) dans chaque bac, selon les règles de votre territoire.

Communes disponibles : Bouconvillers, Magny-en-Vexin, Théméricourt, Cormeilles-en-Vexin, Épiais-Rhus, Sannois, Ermont.

Après la première synchronisation, le calendrier fonctionne hors ligne. Aucun compte n’est requis.

**Catégorie :** Outils (ou Style de vie)

### Notes de version — test fermé 1.5.0

À coller dans Play Console → Tests fermés → Notes de version :

```
Bouconvillers : nouvelle commune disponible (calendrier CCVT / Vexin-Thelle).
Interface : barre de navigation plus stable au défilement.
```

### Notes de version — test fermé 1.4.1 (historique)

```
Chargement du calendrier : affichage plus épuré pendant la synchronisation (squelette sans message texte).
```

### Notes de version — test fermé 1.4.0 (historique)

```
Guide du tri : consignes par type de bac (à mettre / à ne pas mettre), adaptées à votre commune.
Épiais-Rhus : nouvelle commune disponible.
Navigation par onglets : Collectes, Guide du tri, Paramètres.
Contact développeur : mail ou WhatsApp depuis les réglages.
Interface plus fluide : actualisation par glissement, barre de navigation qui se cache au défilement.
```

### Notes de version — test fermé 1.3.0 (historique)

```
Sannois (Pavillons) : bac jaune hebdomadaire et collecte des végétaux (ex. jaune + végétaux le même jour).
Ermont : commune renommée (plus « Ermont-Eaubonne »), calendrier Pavillons corrigé (emballages, végétaux, encombrants).
Théméricourt : correction des dates de bac jaune.
Actualisation plus fluide : les collectes restent visibles pendant le rechargement ou le changement de commune.
Resynchronisation automatique après mise à jour du calendrier.
```

### Notes de version — test fermé 1.2.0 (historique)

```
Calendrier plus fiable : meilleure lecture des dates selon chaque commune (jours officiels, emballages et verre).
Liste des collectes : uniquement les dates à venir (plus de dates passées).
Changement de commune : le cache est réinitialisé pour récupérer le bon calendrier.
Règles de secours si le site officiel est injoignable.
```

## 5. Politique de confidentialité

Fichier source : [docs/privacy-policy.html](../../docs/privacy-policy.html)

URL utilisée dans l’app (`privacy_policy_url`) :
`https://mathieudamoisy-perso.github.io/Smirtom/privacy-policy.html`

Activer **GitHub Pages** sur le dépôt (`Settings → Pages`, source : dossier `/docs` de la branche principale) pour que cette URL réponde. Coller la même URL dans Play Console → Politique de confidentialité.

**Important :** pousser la mise à jour de `docs/privacy-policy.html` sur `main` avant de publier la beta, pour que l’URL publique soit à jour.

## 6. Sécurité des données (Data safety)

Déclarer en cohérence avec la politique :

- Pas de compte, pas de collecte partagée avec des tiers
- Données **sur l’appareil uniquement** : commune, heure de rappel, calendrier
- Réseau : téléchargement de documents publics sur les sites officiels des collectivités / syndicats, notamment :
  - `smirtomduvexin.net` (communes du Vexin — SMIRTOM)
  - `vexinthelle.fr` (CCVT / Vexin-Thelle, ex. Bouconvillers)
  - `bouconvillers.fr` (page d’information tri sélectif)
  - `ville-sannois.fr` (Sannois)
  - `ermont.fr` (Ermont)
  - `syndicat-emeraude.fr` (Syndicat Emeraude)
- Pas de pub, pas d’analytics
- Chiffrement en transit (HTTPS)

## 7. Permissions à justifier

- **Alarmes exactes** (`SCHEDULE_EXACT_ALARM`) : rappel à l’heure choisie la veille d’une collecte. Ne pas utiliser `USE_EXACT_ALARM`.
- **Optimisation batterie** : optionnelle, pour que le système n’endorme pas les rappels.
- **Notifications** : rappels locaux.

## 8. Parcours Console — beta 1.5.0

1. `git pull` puis vérifier `versionCode = 8` dans `build.gradle.kts`
2. Builder l’AAB release (section 3)
3. Play Console → **Tests fermés** → **Créer une version**
4. Uploader `app-release.aab`
5. Coller les **notes de version** (section 4)
6. Mettre à jour la description longue (ajouter Bouconvillers) si ce n’est pas déjà fait
7. Vérifier **Data safety** et l’URL de politique de confidentialité
8. **Réviser et publier** la piste fermée
9. Vérifier sur un appareil testeur : Bouconvillers (sync calendrier CCVT), barre de navigation au scroll, guide du tri, ouverture du PDF

Ensuite seulement : **Production** (après validation et délai éventuel imposé par Google).
