# Ajout du filtrage par rangement dans les produits

Ce plan détaille les modifications nécessaires pour permettre aux utilisateurs de filtrer la liste des produits par rangement (storage) de manière plus directe, même si aucun domicile n'est sélectionné.

## Changements proposés

### Ressources (Cordes)

Ajout de nouvelles chaînes de caractères pour les filtres "Tous" dans toutes les langues supportées.

#### [MODIFY] [strings.xml](file:///E:/Antigravity/frigo/android/app/src/main/res/values/strings.xml)
#### [MODIFY] [strings.xml](file:///E:/Antigravity/frigo/android/app/src/main/res/values-fr/strings.xml)
#### [MODIFY] [strings.xml](file:///E:/Antigravity/frigo/android/app/src/main/res/values-de/strings.xml)
#### [MODIFY] [strings.xml](file:///E:/Antigravity/frigo/android/app/src/main/res/values-es/strings.xml)
#### [MODIFY] [strings.xml](file:///E:/Antigravity/frigo/android/app/src/main/res/values-nl/strings.xml)

### Interface Utilisateur (UI)

#### [MODIFY] [AllItemsScreen.kt](file:///E:/Antigravity/frigo/android/app/src/main/java/com/mystockmanager/app/ui/items/AllItemsScreen.kt)
- Toujours afficher la ligne de filtres par rangement (au lieu de l'afficher seulement quand un domicile est sélectionné).
- Si aucun domicile n'est sélectionné, afficher tous les rangements.
- Si un domicile est sélectionné, afficher seulement les rangements de ce domicile.
- Utiliser les nouvelles ressources de chaînes pour les puces de filtrage "Tous".
- Afficher le nom du rangement dans chaque ligne de produit pour plus de clarté.

## Plan de vérification

### Tests Manuels
1. Ouvrir l'onglet "Produits".
2. Vérifier que les filtres "Tous Domiciles" et "Tous Rangements" sont visibles.
3. Sélectionner un rangement spécifique sans sélectionner de domicile : la liste doit se filtrer correctement.
4. Sélectionner un domicile : la liste des rangements disponibles doit se mettre à jour pour ne montrer que ceux de ce domicile.
5. Vérifier que le nom du rangement est bien affiché dans les cartes de produits.
