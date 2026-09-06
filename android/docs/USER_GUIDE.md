# Guide Utilisateur - MyStockManager

Bienvenue dans votre gestionnaire de stock intelligent. Cette application vous permet de gérer vos produits, vos courses, vos lieux de stockage ainsi que la génération de recettes par IA basées sur vos ingrédients.

---

## 1. Tableau de Bord (Péremption) & Suggestions IA
L'écran d'accueil affiche les produits qui arrivent bientôt à expiration. 
- **Urgence visuelle** : Les produits sont triés par date avec une icône ⏰.
- **Recherche de recettes instantanée** : Cliquez sur l'icône de couverts 🍴 en face d'un produit pour lancer la recherche de recettes basées sur cet ingrédient.
- **Notifications** : Notification quotidienne automatique si des produits vont périmer (réglable dans les paramètres).

![Écran de péremption](screenshots/dashboard.png)

---

## 2. Gestion des Produits & Mode Sélection
Cet onglet liste l'intégralité de votre stock actuel.
- **Barre de recherche** : Trouvez un produit rapidement.
- **Filtres compacts** : Deux menus déroulants sur la même ligne permettent de filtrer par **Domicile** (sélectionne automatiquement votre domicile actif) et par **Rangement**.
- **Cartes produits optimisées** :
  - Nom du produit mis en valeur.
  - Quantité, unité et lieu de rangement.
  - Boutons de réglage rapide (`-` / `+`), **ajout rapide aux courses** 🛒 et suppression 🗑️.
- **Sélection multiple pour recettes** : Effectuez un **appui long** sur un produit pour activer le mode sélection, cochez un ou plusieurs ingrédients, puis cliquez sur `🍴 Chercher des recettes` en haut.

![Liste des produits](screenshots/products.png)
![Mode sélection multi-ingrédients](screenshots/products_selection.png)

### Ajout et Modification
Vous pouvez saisir manuellement les informations ou utiliser le **scanner de codes-barres**.
- Le scanner interroge automatiquement internet (Open Food Facts) pour remplir le nom du produit.
- Vous pouvez prendre une photo du produit pour l'identifier plus facilement.
- Définissez un **seuil de réassort** : si votre stock tombe en dessous, le produit sera ajouté automatiquement à votre liste de courses.

![Formulaire d'ajout](screenshots/add_product.png)
![Interface du scanner](screenshots/scanner.png)

---

## 3. Recherche de Recettes par IA & Mes Recettes
MyStockManager intègre un générateur de recettes par IA pour éviter le gaspillage alimentaire.

### Accès Global "Mes Recettes" 📖
Un **bouton global d'accès rapide 📖** est présent dans la barre supérieure sur tous les écrans. Il vous permet de :
- Consulter vos recettes enregistrées ou précédemment choisies.
- Retrouver vos préparations favorites à tout moment.

### Étapes de Recherche par IA
1. **Sélection de l'ingrédient** : Depuis le tableau de péremption 🍴 ou la liste de stock (mode sélection).
2. **Choix de la cuisine** : Sélectionnez un ou plusieurs styles (Française, Italienne, Chinoise, Japonaise, Indienne, Thaïlandaise, Mexicaine, Méditerranéenne, Américaine, Autre).
3. **Résultats IA** : L'IA vous propose des recettes sur-mesure utilisant vos produits.
4. **Fiche recette** : Affiche les portions, les ingrédients (avec indication des éléments déjà en stock) et les étapes de préparation.

![Choix du type de cuisine](screenshots/cuisine_picker.png)
![Résultats des recettes IA](screenshots/recipe_results.png)
![Détail d'une recette](screenshots/recipe_detail.png)
![Mes recettes enregistrées](screenshots/my_recipes.png)

---

## 4. Liste de Courses
Gérez vos achats efficacement.
- La liste est divisée en deux sections : **À acheter** et **Acheté**.
- **Ajout manuel & direct** : Ajoutez un produit manuellement ou envoyez n'importe quel produit du stock directement en liste de courses via l'icône de panier.
- **Filtre par magasin** : Affichez uniquement les articles d'une enseigne spécifique.
- **Rangement automatique** : Une fois un article acheté, cliquez sur l'icône 📦 pour le ranger. Si un lieu de stockage est défini, il est réintégré au stock instantanément.

![Liste de courses](screenshots/shopping.png)

---

## 5. Gestion des Lieux
Organisez vos rangements et vos magasins favoris.
- **Domiciles** : Créez et gérez vos différents domiciles ou résidences.
- **Rangements** : Créez vos placards, frigo ou congélateur en précisant le type de conservation et le domicile associé.
- **Magasins & Unités** : Enregistrez vos enseignes et vos unités de mesure.

![Gestion des lieux](screenshots/places.png)

---

## 6. Paramètres et Synchronisation
Personnalisez votre expérience et synchronisez vos données.
- **Profil & Domicile Actif** : Renseignez vos informations et sélectionnez votre domicile principal.
- **Multilingue** : Support du Français, Anglais, Néerlandais, Allemand et Espagnol.
- **Thèmes** : Support complet du Mode Sombre et du Mode Clair.
- **Synchronisation du foyer** : Saisie/Édition manuelle du GUID, boutons Copier 📋 / Coller 📋, et partage par QR Code.

![Paramètres](screenshots/settings.png)
![Partage par QR Code](screenshots/share_qr.png)
