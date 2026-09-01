# Publication Google Play — Collectes

Application : **Collectes** (`com.collectes.app`)  
Version actuelle : `1.0.1` (`versionCode` 2)

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

Fichier à envoyer : `android/app/build/outputs/bundle/release/app-release.aab`

### Numéro de version (bureau + domicile)

La version est définie **une seule fois** dans `android/app/build.gradle.kts` (fichier versionné dans git) :

- `versionCode` : entier **strictement croissant** à chaque upload Play Store (2, 3, 4…)
- `versionName` : version affichée aux utilisateurs (ex. `1.0.1`)

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

Captures utiles : accueil « Demain », liste à venir, réglages (commune + heure), carte batterie si visible.

### Textes FR (à coller dans la Console)

**Titre :** Collectes

**Description courte :**
Rappels la veille des collectes de déchets du SMIRTOM du Vexin.

**Description longue :**
Collectes vous rappelle la veille de sortir les bacs, pour les communes du SMIRTOM du Vexin.

L’application télécharge le calendrier officiel, affiche la collecte de demain et les prochaines dates (ordures, emballages, verre, encombrants selon la commune), et envoie une notification locale à l’heure que vous choisissez.

Après la première synchronisation, le calendrier fonctionne hors ligne. Aucun compte n’est requis.

**Catégorie :** Outils (ou Style de vie)

## 5. Politique de confidentialité

Fichier source : [docs/privacy-policy.html](../../docs/privacy-policy.html)

URL utilisée dans l’app (`privacy_policy_url`) :
`https://mathieudamoisy-perso.github.io/Smirtom/privacy-policy.html`

Activer **GitHub Pages** sur le dépôt (`Settings → Pages`, source : dossier `/docs` de la branche principale) pour que cette URL réponde. Coller la même URL dans Play Console → Politique de confidentialité.

## 6. Sécurité des données (Data safety)

Déclarer en cohérence avec la politique :

- Pas de compte, pas de collecte partagée avec des tiers
- Données **sur l’appareil uniquement** : commune, heure de rappel, calendrier
- Réseau : téléchargement de documents publics sur smirtomduvexin.net
- Pas de pub, pas d’analytics
- Chiffrement en transit (HTTPS)

## 7. Permissions à justifier

- **Alarmes exactes** (`SCHEDULE_EXACT_ALARM`) : rappel à l’heure choisie la veille d’une collecte. Ne pas utiliser `USE_EXACT_ALARM`.
- **Optimisation batterie** : optionnelle, pour que le système n’endorme pas les rappels.
- **Notifications** : rappels locaux.

## 8. Parcours Console

1. Activer **Play App Signing**.
2. Uploader l’AAB sur la piste **Tests fermés**.
3. Remplir contenu de l’app, Data safety, audience (18+ ou tout public, pas d’enfants ciblés), questionnaires permissions.
4. Ajouter des testeurs → publier la piste → attendre la revue.
5. Ensuite seulement : **Production**.
