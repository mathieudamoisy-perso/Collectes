# Smirtom / Collectes

Application Android **Collectes** pour recevoir des rappels la veille des collectes de déchets, selon votre commune (SMIRTOM du Vexin, Syndicat Emeraude, etc.).

## Fonctionnalités

- Téléchargement automatique du calendrier officiel de l'année en cours (PDF)
- Parsing des dates de collecte (ordures, emballages, verre, encombrants selon les communes)
- Notifications locales la veille (heure configurable par créneaux de 30 min, défaut 12h, plage 5h–23h)
- Changement d'année automatique (re-sync du calendrier)
- Fonctionne hors ligne après la première synchronisation

## Communes prises en charge

| Commune | Source calendrier |
|---------|-------------------|
| Magny-en-Vexin | SMIRTOM du Vexin |
| Théméricourt | SMIRTOM du Vexin |
| Cormeilles-en-Vexin | SMIRTOM du Vexin |
| Sannois | Ville de Sannois |
| Ermont-Eaubonne | Syndicat Emeraude (Ermont) |

## Prérequis

- [Android Studio](https://developer.android.com/studio) (recommandé) avec **JDK 17**
- Android 8.0+ (API 26)

Ouvrir le dossier `android/` dans Android Studio.

## Publication Play Store

Guide complet : [android/store/PLAY_STORE.md](android/store/PLAY_STORE.md)

**Version actuelle :** `1.2.0` (`versionCode` 4)

### Keystore (une fois)

Copier `android/keystore.properties.example` vers `android/keystore.properties` et renseigner le mot de passe du keystore local (`android/keystore/upload.jks`). Ne jamais committer ces fichiers.

### Bundle release (AAB)

```bash
cd android
./gradlew :app:bundleRelease
```

Sur Windows :

```powershell
cd android
.\gradlew.bat :app:bundleRelease
```

Fichier à envoyer à Play Console :
`android/app/build/outputs/bundle/release/app-release.aab`

## Compiler en debug (hors Play Store)

```bash
cd android
./gradlew assembleDebug
```

APK : `android/app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

## Premier lancement

1. Autoriser les **notifications**
2. Choisir sa **commune** dans les réglages
3. L'app télécharge le calendrier (connexion Internet requise)
4. Vérifier l'écran d'accueil : les prochaines collectes s'affichent
5. *(Recommandé)* Désactiver l'**optimisation batterie** pour Collectes dans les réglages Android

## Réglages

- Commune
- Heure du rappel (5h à 23h, pas de 30 min, défaut 12h)
- Test de notification (rappel fictif avec un type de collecte aléatoire)
- Lien vers le calendrier officiel (SMIRTOM ou source municipale selon la commune)
- Politique de confidentialité

## Structure du projet

```
android/
  app/src/main/java/com/smirtom/app/
    data/           # Fetcher, parser PDF, base Room
    notifications/  # Alarmes et rappels locaux
    ui/             # Interface Compose
  store/            # Assets et checklist Play Store
docs/
  privacy-policy.html
```

## Tests

```bash
cd android
./gradlew test
```

## Licence

Usage personnel.
