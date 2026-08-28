# Smirtom

Application Android pour recevoir des rappels la veille des collectes de déchets du **SMIRTOM du Vexin** (commune de **Magny-en-Vexin**).

## Fonctionnalités

- Téléchargement automatique du calendrier PDF SMIRTOM de l'année en cours
- Parsing des dates de collecte (ordures, emballages, verre)
- Notifications locales la veille au soir (heure configurable, défaut 19h)
- Changement d'année automatique (re-sync du calendrier)
- Fonctionne hors ligne après la première synchronisation

## Prérequis pour compiler

- [Android Studio](https://developer.android.com/studio) (recommandé) avec **JDK 17**
- Android 8.0+ (API 26) sur le téléphone

Ouvrir le dossier `android/` dans Android Studio, puis **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Compiler l'APK

```bash
cd android
./gradlew assembleDebug
```

L'APK debug est généré ici :
`android/app/build/outputs/apk/debug/app-debug.apk`

Pour une version release :

```bash
./gradlew assembleRelease
```

## Installer sur le téléphone (sans Play Store)

1. Transférer l'APK sur le téléphone (USB, Drive, email…)
2. **Paramètres** → **Sécurité** → activer **Installer des applications inconnues** pour votre gestionnaire de fichiers
3. Ouvrir l'APK et confirmer **Installer**
4. Si Play Protect affiche un avertissement → **Installer quand même**

Alternative via USB :

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

## Premier lancement

1. Autoriser les **notifications** (indispensable)
2. L'app télécharge le calendrier SMIRTOM (connexion Internet requise)
3. Vérifier l'écran d'accueil : les prochaines collectes s'affichent
4. *(Recommandé)* Désactiver l'**optimisation batterie** pour Smirtom dans les paramètres Android

## Réglages

- Heure du rappel : 17h à 22h (défaut 19h)
- Bouton **Actualiser le calendrier** pour forcer une re-synchronisation
- Lien vers le site SMIRTOM

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
```

## Tests

```bash
cd android
./gradlew test
```

## Licence

Usage personnel.
