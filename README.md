# Smirtom / Collectes

Application Android **Collectes** pour recevoir des rappels la veille des collectes de déchets du **SMIRTOM du Vexin**.

## Fonctionnalités

- Téléchargement automatique du calendrier PDF SMIRTOM de l'année en cours
- Parsing des dates de collecte (ordures, emballages, verre, encombrants)
- Notifications locales la veille (heure configurable par créneaux de 30 min, défaut 12h)
- Changement d'année automatique (re-sync du calendrier)
- Fonctionne hors ligne après la première synchronisation

## Prérequis

- [Android Studio](https://developer.android.com/studio) (recommandé) avec **JDK 17**
- Android 8.0+ (API 26)

Ouvrir le dossier `android/` dans Android Studio.

## Publication Play Store

Voir le guide : [android/store/PLAY_STORE.md](android/store/PLAY_STORE.md)

### Keystore (une fois)

Copier `android/keystore.properties.example` vers `android/keystore.properties` et renseigner le mot de passe du keystore local (`android/keystore/upload.jks`). Ne jamais committer ces fichiers.

### Bundle release (AAB)

```bash
cd android
./gradlew :app:bundleRelease
```

Le fichier à envoyer à Play Console :
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
2. L'app télécharge le calendrier SMIRTOM (connexion Internet requise)
3. Vérifier l'écran d'accueil : les prochaines collectes s'affichent
4. *(Recommandé)* Désactiver l'**optimisation batterie** pour Collectes dans les réglages Android

## Réglages

- Commune
- Heure du rappel (6h à 12h, pas de 30 min, défaut 12h)
- Test de notification (rappel fictif avec un type de collecte aléatoire)
- Lien vers le calendrier officiel SMIRTOM
- Politique de confidentialité

## Collectes Magny-en-Vexin (2026)

| Bac | Fréquence | Jour |
|-----|-----------|------|
| Ordures ménagères | Hebdomadaire | Lundi |
| Emballages / papiers | Bihebdomadaire | Mardi |
| Verre | Toutes les 4 semaines | Mardi |

Les bacs doivent être sortis **la veille au soir**.

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
