# Guide Utilisateur - MyStockManager

Bienvenue dans votre gestionnaire de stock intelligent. Cette application vous permet de gérer vos produits, vos courses et vos lieux de stockage de manière simple et synchronisée.

## 1. Tableau de Bord (Péremption)
L'écran d'accueil affiche les produits qui arrivent bientôt à expiration. 
- Les produits sont triés par date.
- Une icône ⏰ indique l'urgence.
- Le système vous envoie une notification quotidienne si des produits vont périmer (réglable dans les paramètres).

![Écran de péremption](screenshots/dashboard.png)

---

## 2. Gestion des Produits
Cet onglet liste l'intégralité de votre stock.
- Utilisez la barre de recherche pour trouver un produit rapidement.
- Cliquez sur un produit pour le modifier.
- Utilisez le bouton **＋** pour ajouter un nouvel article.

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
- **Filtre par magasin** : Affichez uniquement les articles d'une enseigne spécifique via le menu déroulant en haut.
- **Rangement automatique** : Une fois un article acheté, cliquez sur l'icône 📦 pour le ranger. Si un lieu de stockage est défini, il est ajouté au stock instantanément.

![Liste de courses](screenshots/shopping.png)

---

## 4. Gestion des Lieux
Organisez vos rangements et vos magasins favoris.
- **Rangements** : Créez vos placards, frigo ou congélateur en précisant le type de conservation.
- **Magasins** : Listez les enseignes où vous faites vos courses pour mieux filtrer votre liste d'achats.

![Gestion des lieux](screenshots/places.png)

---

## 5. Paramètres et Synchronisation
Personnalisez votre expérience.
- **Langue** : Choisissez parmi 5 langues (Français, Anglais, Néerlandais, Allemand, Espagnol).
- **Thème** : Basculez entre le **Mode Sombre** (haute lisibilité) et le **Mode Clair**.
- **Synchronisation** : Partagez votre stock avec d'autres membres de votre foyer.
    - Copiez votre GUID unique.
    - Affichez votre **QR Code** pour qu'un autre téléphone puisse le scanner.
    - Scannez le code d'un proche pour rejoindre son inventaire.

![Paramètres](screenshots/settings.png)
![Partage par QR Code](screenshots/share_qr.png)
