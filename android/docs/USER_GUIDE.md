# Guide Utilisateur - MyStockManager

Bienvenue dans votre gestionnaire de stock intelligent. Cette application vous permet de gérer vos produits, vos courses et vos lieux de stockage de manière simple et synchronisée.

## 1. Tableau de Bord (Péremption)
L'écran d'accueil affiche les produits qui arrivent bientôt à expiration. 
- Les produits sont triés par date.
- Une icône ⏰ indique l'urgence visuelle.
- Le système vous envoie une notification quotidienne si des produits vont périmer (réglable dans les paramètres).

![Écran de péremption](screenshots/dashboard.png)

---

## 2. Gestion des Produits
Cet onglet liste l'intégralité de votre stock actuel avec une lisibilité maximale.
- **Barre de recherche** : Trouvez un produit instantanément.
- **Filtres compacts** : Deux menus déroulants sur la même ligne permettent de filtrer par **Domicile** (sélectionne automatiquement votre domicile actif par défaut) et par **Rangement**.
- **Cartes produits aérées** :
  - **Ligne supérieure** : Nom du produit mis en valeur.
  - **Ligne intermédiaire** : Quantité, unité et lieu de rangement.
  - **Ligne d'actions** : Boutons de réglage rapide de la quantité (`-` / `+`), bouton d'**ajout rapide à la liste de courses** 🛒 et bouton de suppression 🗑️.
- Utilisez le bouton flottant **＋** pour ajouter un nouvel article.

![Liste des produits](screenshots/products.png)

### Ajout et Modification
Vous pouvez saisir manuellement les informations ou utiliser le **scanner de codes-barres**.
- Le scanner interroge automatiquement internet (Open Food Facts) pour remplir le nom du produit.
- Vous pouvez prendre une photo du produit pour l'identifier plus facilement.
- Définissez un **seuil de réassort** : si votre stock tombe en dessous, le produit sera ajouté automatiquement à votre liste de courses.

![Formulaire d'ajout](screenshots/add_product.png)
![Interface du scanner](screenshots/scanner.png)

---

## 3. Liste de Courses
Gérez vos achats efficacement.
- La liste est divisée en deux sections : **À acheter** et **Acheté**.
- **Ajout manuel** : Ajoutez un produit manuellement ou envoyez n'importe quel produit du stock directement en liste de courses via l'icône de panier.
- **Filtre par magasin** : Affichez uniquement les articles d'une enseigne spécifique via le menu déroulant en haut.
- **Regroupement** : Activez ou désactivez l'agrégation des produits identiques via l'icône en haut à droite.
- **Rangement automatique** : Une fois un article acheté, cliquez sur l'icône 📦 pour le ranger. Si un lieu de stockage est défini, il est réintégré au stock instantanément.

![Liste de courses](screenshots/shopping.png)

---

## 4. Gestion des Lieux
Organisez vos rangements et vos magasins favoris.
- **Domiciles** : Créez et gérez vos différents domiciles ou résidences.
- **Rangements** : Créez vos placards, frigo ou congélateur en précisant le type de conservation et le domicile associé.
- **Magasins** : Listez les enseignes où vous faites vos courses pour mieux filtrer votre liste d'achats.
- **Unités** : Personnalisez vos unités de mesure.

![Gestion des lieux](screenshots/places.png)

---

## 5. Paramètres et Synchronisation
Personnalisez votre expérience et synchronisez vos données.
- **Profil** : Saisissez vos nom et prénom pour identifier le demandeur des articles dans le foyer.
- **Domicile Actif** : Choisissez votre domicile principal.
- **Langue** : Choisissez parmi 5 langues (Français, Anglais, Néerlandais, Allemand, Espagnol).
- **Thème** : Basculez entre le **Mode Sombre** (haute lisibilité) et le **Mode Clair**.
- **Synchronisation du foyer** :
    - **Édition & Saisie manuelle du GUID** : Saisissez ou modifiez directement votre canal de synchronisation.
    - **Copier & Coller** : Utilisez les boutons dédiés pour copier 📋 ou coller 📋 votre code GUID depuis le presse-papier.
    - **QR Code** : Affichez votre QR Code ou scannez celui d'un proche pour rejoindre instantanément son inventaire.

![Paramètres](screenshots/settings.png)
![Partage par QR Code](screenshots/share_qr.png)
