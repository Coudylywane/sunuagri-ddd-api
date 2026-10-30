# Glossaire du projet SunuAgri

Le vocabulaire du métier est celui de la coopérative. Les mêmes termes se retrouvent dans le
code, le contrat d'API et la documentation (exigence **E4**). Ce glossaire fixe le sens retenu
et signale les mots évités.

## Acteurs et identités

| Terme | Définition | Code / API |
|---|---|---|
| **Producteur** | Agriculteur membre de la coopérative. Il cultive des parcelles et apporte sa récolte. Il est identifié par son numéro de téléphone, unique dans la coopérative. | `Producteur`, `ProducteurVue` |
| **Acheteur** | Grossiste ou transformateur qui commande de la marchandise à la coopérative. | `Role.ACHETEUR`, `acheteurId` |
| **Agent de coopérative** | Employé qui enregistre les producteurs, suit les campagnes, reçoit les récoltes et gère les ventes. | `Role.AGENT` |
| **Administrateur** | Responsable des paramètres de la plateforme (paramètres des cultures). | `Role.ADMINISTRATEUR` |
| **Identité** | Utilisateur simulé de l'appel : identifiant + rôle, portés par les en-têtes `X-Utilisateur-Id` et `X-Role`. L'authentification réelle est hors périmètre. | `Identite`, `UtilisateurCourant` |

## Production agricole

| Terme | Définition | Code / API |
|---|---|---|
| **Parcelle** | Terrain d'un producteur, avec une superficie en hectares et une commune. Une parcelle appartient à un seul producteur ; deux parcelles d'un même producteur ne portent pas le même nom. | `Parcelle`, `ParcelleVue` |
| **Superficie** | Surface en hectares. Toujours strictement positive. | `Superficie`, `superficieHa` |
| **Campagne** | Saison agricole (`hivernage` ou `contre-saison`) d'une année donnée, avec une date de début et une date de fin. Une campagne est ouverte puis clôturée. | `Campagne`, `Saison`, `CampagneVue` |
| **Culture** | Plante cultivée sur une parcelle pendant une campagne (arachide, mil, riz, maïs), avec la superficie emblavée. Une culture appartient à une campagne. | `Culture`, `CultureVue` |
| **Superficie emblavée** | Partie de la parcelle effectivement semée pour une culture. Sur une parcelle et pour une campagne, la somme des superficies emblavées ne dépasse pas la superficie de la parcelle. | `superficieEmblavee`, champ `superficieHa` |
| **Statut de culture** | Cycle de vie ordonné : `planifiée` → `semée` → `récoltée`. Aucun retour en arrière. | `StatutCulture` (`PLANIFIEE`, `SEMEE`, `RECOLTEE`) |
| **Cycle minimal** | Durée minimale, en jours, entre le semis et la récolte pour une culture donnée. | `cycleMinJours` |
| **Récolte** | Quantité produite par une culture, en kilogrammes, à une date donnée. Une culture n'est récoltée qu'une seule fois. Dans ce projet, date et quantité sont déclarées ensemble. | `Culture.recolter`, `Recolte`, `RecolteVue` |
| **Rendement de référence** | Production attendue en kg par hectare pour une culture. Sert à refuser une récolte suspecte (plus du double). | `rendementReferenceKgHa` |

## Entrepôt et stock

| Terme | Définition | Code / API |
|---|---|---|
| **Livraison à l'entrepôt** | Apport d'une récolte à la coopérative (quantité, taux d'humidité, qualité). Une livraison acceptée crée exactement un lot. Une livraison refusée ne modifie rien. | `Livraison`, `LivraisonVue` |
| **Lot** | Quantité reçue d'un producteur, rattachée à une récolte, avec sa qualité, son taux d'humidité et sa date de réception. | `Lot` |
| **Qualité** | Niveau de qualité d'un lot : `A`, `B` ou `C`. | `Qualite` |
| **Taux d'humidité** | Pourcentage d'eau du produit livré. Une livraison au-dessus du seuil de la culture est refusée. | `tauxHumiditePct`, `humiditeMaxPct` |
| **Stock** | Marchandise physiquement présente à l'entrepôt, par culture et par qualité. | `LotRepository` |
| **Total** | Somme des quantités restantes des lots d'une culture et d'une qualité. | `totalKg` |
| **Réservé** | Quantité immobilisée par les offres publiées et non encore vendues. | `reserveKg` |
| **Disponible** | Total moins réservé : ce qui peut encore être proposé à la vente. | `disponibleKg` |
| **Consommation FIFO** | À la livraison d'une commande, les lots sont consommés du plus ancien au plus récent (date de réception). | `LotRepository.parCultureEtQualite` |

## Commercialisation et paiements

| Terme | Définition | Code / API |
|---|---|---|
| **Offre** | Quantité à vendre, pour une culture et une qualité, avec un prix au kilogramme en francs CFA. Statuts : `ouverte`, `épuisée`, `retirée`. | `Offre`, `OffreVue`, `StatutOffre` |
| **Quantité restante** | Quantité de l'offre qui n'est ni commandée ni vendue. | `Offre.restante`, `quantiteRestanteKg` |
| **Réservation** | Publier une offre immobilise sa quantité dans le stock ; retirer l'offre libère la quantité non vendue. | `Offre.quantiteReservee`, `ReservationsStock` |
| **Avertissement de prix** | Message produit, sans refus, quand le prix de l'offre s'écarte de plus de 30 % du prix de référence du marché. | `AnalysePrix`, `avertissementPrix` |
| **Commande** | Demande d'un acheteur sur une offre ouverte : quantité (minimum 50 kg) et montant. Statuts : `en_attente`, `livrée`, `annulée`. | `Commande`, `CommandeVue`, `StatutCommande` |
| **Montant de la commande** | Quantité commandée × prix au kg, en francs CFA entiers. | `Commande.montant`, `montantFrancs` |
| **Livraison de commande** | Confirmation par un agent que la marchandise part : elle consomme les lots, clôt la commande et déclenche les paiements. | `livrerCommande`, `LivraisonCommandeVue` |
| **Commission** | Part retenue par la coopérative : 5 % du montant de la commande, arrondie au franc inférieur. | `commissionFrancs` |
| **Montant net** | Montant de la commande moins la commission ; c'est ce qui est réparti entre producteurs. | `montantNetFrancs` |
| **Répartition** | Calcul de la part de chaque producteur, proportionnellement aux kilogrammes apportés dans les lots consommés. | `RepartitionPaiement` |
| **Paiement dû** | Part du montant d'une commande qui revient à un producteur. Statut `à payer`, puis `payé` (le règlement effectif est hors périmètre). | `PaiementDu`, `PaiementVue`, `StatutPaiement` |
| **SMS de paiement** | Notification envoyée une seule fois par paiement dû, indiquant le montant en français. Un échec est tracé sans invalider la livraison. | `SmsPort`, `NotificationPaiementService`, `statutSms` |
| **Prix de référence du marché** | Prix au kg fourni par un service externe pour une culture. En cas d'indisponibilité, le dernier prix connu est affiché avec sa date. | `PrixMarche`, `PrixMarcheVue`, `issuDuCache` |

## Objets-valeurs et unités

| Terme | Définition | Code / API |
|---|---|---|
| **Quantité** | Masse en kilogrammes. Strictement positive à l'entrée du métier ; zéro autorisé en interne (stock épuisé). | `Quantite`, `quantiteKg` |
| **Montant** | Somme en francs CFA (XOF), toujours entière : le franc CFA n'a pas de sous-unité. | `Montant`, `montantFrancs` |
| **Téléphone** | Numéro sénégalais normalisé sur 9 chiffres (`7XXXXXXXX`). Les formats `77 123 45 67`, `+221771234567` et `00221771234567` sont acceptés. | `Telephone`, `telephone` |
| **Période** | Intervalle de dates inclusif ; la fin est strictement postérieure au début. | `Periode` |
| **Horloge** | Source de la date du jour, injectée pour rendre les tests déterministes. | `HorlogePort`, `HorlogeFixe` (tests) |

## Mots évités

| Évité | Préféré | Raison |
|---|---|---|
| « stock réservé » pour la quantité commandée | « quantité restante de l'offre » | Le réservé désigne l'engagement de stock par une offre. |
| « vente » pour une commande | « commande », puis « livraison de commande » | La vente n'est effective qu'à la livraison. |
| « paiement » pour une commande | « paiement dû » | Le paiement dû est la part d'un producteur ; la commande a un montant. |
| « utilisateur » pour un producteur | « producteur » | Les acteurs ont des noms métier précis. |
| « produit » | « culture » ou « lot » selon le contexte | Deux notions distinctes. |
| « client » | « acheteur » | Terme de la coopérative. |
