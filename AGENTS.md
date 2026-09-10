# Contexte du Projet "Collectes"
- Je suis le seul développeur.
- GitHub pour les sources ; branche par défaut : `main`.
- Une seule application Android.
- Application de rappels de collectes de déchets selon la commune (usage personnel / Play Store).

# Stack Technique
- Kotlin, Compose.
- Mode déconnecté.
- Après sync : lire les données locales ; ne pas bloquer l’UI sur le réseau.
- Synchronisation calendriers via HTTP / PDF / HTML.
- Rappels de collectes + notifications locales.
- Code sous `android/` ; package racine `com.collectes.app`.

# Architecture (à respecter)
- Liste des communes / URLs officielles : `VexinCommunes.kt` (source de vérité).
- Réutiliser l’existant avant d’ajouter un fichier ou une couche.

# Métier / Communes
- Ne pas inventer une commune sans source officielle (PDF / page mairie ou syndicat).
- UI et messages utilisateur en français.

# Git / Secrets
- Ne committer / pousser que si je le demande.
- Aucune chaîne secrète dans le repo (`keystore.properties`, mots de passe, tokens).
- Ne pas modifier la config git.

# Directives de Code pour l'IA
- Mutualiser systématiquement le code et l’IHM.
- Commentaires : max 1 ligne par bloc ; uniquement si utile.
- Pas de nouveau module / framework sans mon autorisation explicite.
- Pas de refactor hors scope ; rester minimal.
- Suivre les patterns déjà présents dans le projet.

# Ton et Format de tes Réponses
- Concision ; listing plutôt que longs paragraphes.
- Langage simple.
- Tout plan doit avoir exactement ce format :
  - **Contexte** : résumé de ce que tu as compris.
  - **A faire** : listing simple de ce que tu t’apprêtes à réaliser.
  - **Risques** : listing des risques liés aux modifications.

# Garde-fous
- Les fichiers `.MD` ne doivent **JAMAIS** être alimentés / écrits / modifiés par une IA.
- Demande **TOUJOURS** l’autorisation avant de télécharger un nouveau module ou framework.
- Release Play Store : suivre le skill `.cursor/skills/deploy` seulement si je le demande (`/deploy`).