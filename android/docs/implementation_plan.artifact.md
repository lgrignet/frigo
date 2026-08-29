# Plan d'implémentation : Dictée Vocale Multilingue des Dates (v2)

Ce plan détaille l'ajout d'une fonctionnalité de reconnaissance vocale locale et multilingue pour saisir rapidement les dates de péremption par la voix, incluant les expressions relatives.

## Objectifs
- Permettre la saisie d'une date par dictée vocale.
- Support natif des 5 langues de l'app (FR, EN, NL, DE, ES).
- **Expressions relatives** : "Demain", "Dans 3 jours", "Dans 6 mois", "Fin du mois".
- Transformation du texte dicté en date normalisée (`JJ/MM/AAAA` ou `AAAA-MM-JJ`).
- Confirmation visuelle immédiate dans le champ de saisie.

## Architecture Technique

### 1. Reconnaissance Vocale (Speech-To-Text)
- Utilisation de `android.speech.SpeechRecognizer`.
- Initialisation avec le `Locale` de l'utilisateur.
- Permission : `android.permission.RECORD_AUDIO`.

### 2. Moteur de Parsing (`DateVoiceParser`)
Un moteur capable d'interpréter :
- **Dates absolues** : "31 décembre 2026", "Douze Octobre".
- **Dates relatives** :
    - "Demain", "Après-demain".
    - "Dans [X] jours/semaines/mois".
    - "Fin de l'année", "Fin du mois".
- Dictionnaires multilingues pour les mots-clés (tomorrow, tomorrow, morgen, mañana, etc.).

### 3. Interface Utilisateur (UI)
- **Bouton Micro** : Intégré au champ "Date de péremption" (trailing icon).
- **État d'écoute** : Changement d'icône ou animation pendant l'enregistrement.

## Plan d'Exécution

### Phase 1 : Infrastructure
- [ ] Ajouter la permission `RECORD_AUDIO` au Manifest.
- [ ] Créer `VoiceRecognitionManager.kt`.

### Phase 2 : Logique de Parsing Intelligent
- [ ] Créer `DateVoiceParser.kt`.
- [ ] Implémenter les dictionnaires de mots-clés (mois, unités de temps, expressions relatives) pour les 5 langues.
- [ ] Ajouter la logique de calcul de date (ex: Date actuelle + 6 mois).

### Phase 3 : Intégration UI
- [ ] Modifier `ItemFormScreen.kt` pour ajouter l'action vocale.
- [ ] Gérer l'affichage visuel de la date reconnue.

## User Review Required

> [!NOTE]
> La confirmation sera uniquement visuelle (mise à jour du champ texte) comme demandé.
