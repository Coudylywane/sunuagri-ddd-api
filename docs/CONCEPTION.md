# Document de conception — SunuAgri

Ce document décrit les contextes bornés, les agrégats, les règles métier story par story,
les hypothèses retenues et les mécanismes transversaux. Il complète le [glossaire](GLOSSAIRE.md)
et les [décisions d'architecture](DECISIONS.md).

---

## 1. Démarche

Le projet suit une approche **DDD** dans une **architecture hexagonale** :

1. Le **langage ubiquitaire** est français et issu du métier de la coopérative
   (producteur, parcelle, campagne, culture, récolte, lot, offre, commande, paiement dû).
2. Le **domaine** est du Java pur : aucune annotation Spring, aucun import HTTP ou SQL.
3. Les **cas d'usage** orchestrent le domaine et les ports sortants ; ils portent les règles
   d'autorisation et de coordination entre agrégats.
4. Les **adaptateurs** implémentent les ports : web (REST), persistance en mémoire, SMS simulé,
   prix du marché simulé, horloge, identifiants.
5. Les **invariants** sont garantis par les agrégats, jamais par les contrôleurs : une règle
   n'existe qu'à un seul endroit, quel que soit le mode d'appel (**E3**).

---

## 2. Contextes bornés

| Contexte | Responsabilité | Agrégats / objets | Dépend de |
|---|---|---|---|
| **Adhésion** | Producteurs et parcelles | `Producteur` (contient `Parcelle`) | — |
| **Production** | Campagnes, cultures, récoltes, paramètres de culture | `Campagne` (contient `Culture`), `Recolte`, `CultureReference` | Adhésion (référence parcelle/producteur) |
| **Entrepôt** | Livraisons, lots, stock | `Lot`, `Livraison` | Production (référence récolte) |
| **Commercialisation** | Offres, commandes, livraison, répartition, paiements | `Offre`, `Commande`, `PaiementDu`, `RepartitionPaiement` | Entrepôt (lots), Adhésion (producteur) |
| **Marché** | Prix de référence externe et analyse d'écart | `PrixMarche`, `AnalysePrix` | — |
| **Accès** (transverse) | Identité simulée et autorisations | `Identite`, `Role`, `Autorisations` | tous |

Les contextes ne se référencent que par **identifiant** (`producteurId`, `parcelleId`,
`recolteId`, `cultureId`). Aucun objet d'un contexte n'est navigable depuis un autre, ce qui
préserve les frontières et permettrait de séparer les modules en services distincts.

### Dépendances entre paquets

```
adapters.web ─┐
adapters.persistence.memoire ─┼─> application ─> domaine <─ adapters.integration
adapters.systeme ─┘
```

Le sens des dépendances est toujours dirigé vers le domaine. Les ports sortants
(`ProducteurRepository`, `SmsPort`, `PrixMarchePort`, `ReservationsStock`, `HorlogePort`,
`IdGeneratorPort`…) sont déclarés côté domaine/application et implémentés côté adaptateurs.

---

## 3. Agrégats et frontières de cohérence

### 3.1 `Producteur` (racine) — entité `Parcelle`

- **Invariants** : téléphone valide et unique (unicité portée par le repository, car elle
  traverse tous les producteurs), nom/prénom/village obligatoires, superficie strictement
  positive, total des superficies ≤ 50 ha, noms de parcelles uniques pour un producteur.
- **Pourquoi une racine unique** : la limite des 50 ha est un invariant qui porte sur *toutes*
  les parcelles d'un producteur. Elle exige une frontière transactionnelle unique.
- **Référence** : une parcelle est référencée par `parcelleId` depuis la Production ; elle
  n'existe jamais seule.

### 3.2 `Campagne` (racine) — entité `Culture`

- **Invariants** : saison valide, fin > début, unicité saison + année (portée par le repository),
  aucune culture ajoutée après clôture, somme des superficies emblavées d'une parcelle ≤ sa
  superficie, statut initial `PLANIFIEE`.
- **Choix** : `Culture` est une entité interne à `Campagne` car la règle de somme des
  superficies emblavées exige de voir toutes les cultures de la campagne pour une parcelle.
- **Instantané des paramètres** : chaque `Culture` détient une copie immuable de
  `CultureReference` (rendement, humidité, cycle) au moment de sa déclaration. C'est ce qui
  rend US-19 sûre : modifier le référentiel n'altère pas les cultures existantes.

### 3.3 `Recolte` (racine)

- Enregistre la quantité récoltée, la date, la culture, la campagne et le producteur.
- Une culture ne peut être récoltée qu'une fois : la contrainte est portée par
  `Culture.statut` (passage à `RECOLTEE`) ; la `Recolte` est l'enregistrement persistant.
- Les livraisons la référencent par `recolteId`.

### 3.4 `Lot` (racine)

- Quantité initiale, quantité restante, qualité, humidité, date de réception, producteur, récolte.
- `consommer` refuse de descendre sous zéro : c'est le garde-fou de la consommation FIFO.
- La quantité restante est la seule vérité physique du stock.

### 3.5 `Livraison` (racine, immuable)

- Trace d'un apport accepté : identifiant, récolte, lot créé, producteur, qualité, quantité,
  humidité, date. Une livraison refusée n'est jamais enregistrée.

### 3.6 `Offre` (racine)

- Quantité publiée, prix au kg, quantité engagée par les commandes en attente, quantité vendue.
- `restante = quantité - engagée - vendue` (jamais stockée en double).
- **Réservation de stock** : tant que l'offre n'est pas retirée, elle immobilise
  `quantité - vendue` (la part non vendue, qu'elle soit déjà commandée ou non). Cela empêche
  deux offres de promettre la même marchandise.
- Transitions : `OUVERTE → EPUISEE` quand `restante = 0`, `OUVERTE/EPUISEE → RETIREE`.
  Le retrait est refusé s'il reste des commandes en attente.

### 3.7 `Commande` (racine)

- Instantané de l'offre : culture, qualité, prix au kg, quantité, montant.
- Statuts : `EN_ATTENTE → LIVREE` ou `EN_ATTENTE → ANNULEE`. Aucune transition depuis un
  état final.
- Le montant est figé à la création (le prix de l'offre ne changera pas, mais la commande
  reste juste même si l'offre évoluait).

### 3.8 `PaiementDu` (racine)

- Montant dû à un producteur pour une commande, statut `A_PAYER` (le règlement effectif est
  hors périmètre), statut SMS (`NON_ENVOYE`, `ENVOYE`, `ECHEC`).
- L'unicité « un SMS par paiement » est portée par `statutSms` : un paiement déjà traité
  n'est jamais renvoyé automatiquement.

### 3.9 `CultureReference` (référentiel)

- Paramètres d'une culture : rendement de référence, humidité maximale, cycle minimal.
- Valeurs initiales centralisées dans `ReferencesParDefaut`, modifiables par un administrateur
  via `ReferentielCultures`.

---

## 4. Règles métier par story

Légende : **Où** indique le lieu unique d'application de la règle.

### US-01 — Enregistrer un producteur (Indispensable)

| Règle | Où |
|---|---|
| Nom, prénom, village obligatoires (non vides) | `Producteur` (constructeur, `Validations.requis`) |
| Téléphone obligatoire, format sénégalais normalisé sur 9 chiffres | `Telephone` |
| Téléphone unique dans la coopérative → refus explicite | `ProducteurService.enregistrer` + `ProducteurRepository.parTelephone` |
| Identifiant attribué | `IdGeneratorPort`, adaptateur UUID |
| Opération réservée à un agent | `Autorisations.exigerAgent` |

### US-02 — Déclarer une parcelle (Indispensable)

| Règle | Où |
|---|---|
| Superficie strictement positive | `Superficie` |
| Total des superficies ≤ 50 ha | `Producteur.declarerParcelle` (seuil injecté) |
| Nom de parcelle unique pour un producteur (insensible à la casse et aux espaces) | `Producteur.declarerParcelle` |
| Une parcelle appartient à un seul producteur | Structure : `Parcelle` interne à `Producteur`, jamais partagée |
| Commune obligatoire | `Parcelle` |

### US-03 — Consulter les parcelles (Importante)

| Règle | Où |
|---|---|
| Liste et détail : nom, commune, superficie | `ParcelleVue` |
| Producteur inexistant → message clair (404) | `ProducteurService.parcelles` |
| Un producteur ne voit que ses parcelles | `Autorisations.accederProducteur` |

### US-04 — Ouvrir et clôturer une campagne (Indispensable)

| Règle | Où |
|---|---|
| Saison `hivernage` ou `contre-saison` | `Saison.depuis` |
| Date de fin strictement postérieure au début | `Periode` |
| Une seule campagne par saison et par année | `CampagneService.ouvrir` + `CampagneRepository.parSaisonEtAnnee` |
| Campagne clôturée : plus de culture, de récolte ni de livraison | `Campagne.exigerOuverte` (appelée par les services Production et Entrepôt) |

### US-05 — Déclarer une culture (Indispensable)

| Règle | Où |
|---|---|
| Campagne ouverte | `Campagne.declarerCulture` |
| Culture dans la liste de référence | `TypeCulture.depuis` |
| Superficie emblavée strictement positive | `Superficie` |
| Somme des superficies emblavées ≤ superficie de la parcelle, pour la campagne | `Campagne.declarerCulture` |
| Statut initial `planifiée` | `Culture` |
| Paramètres figés pour les déclarations futures | instantané `CultureReference` |
| Parcelle existante | `CampagneService.declarerCulture` |

### US-06 — Suivre l'avancement d'une culture (Importante)

| Règle | Où |
|---|---|
| Ordre `planifiée → semée → récoltée`, aucun retour | `StatutCulture.verifierTransitionVers`, `Culture.semer`, `Culture.recolter` |
| Date de récolte ≥ date de semis + cycle minimal | `Culture.recolter` |
| Une culture planifiée seulement peut être semée | `Culture.semer` |
| Campagne ouverte | `CampagneService.semer` / `recolter` |

L'API expose `POST /cultures/{id}/semis` puis `POST /cultures/{id}/recolte`
(date et quantité ensemble, voir hypothèse H1).

### US-07 — Déclarer une récolte (Indispensable)

| Règle | Où |
|---|---|
| Culture semée | `Culture.recolter` |
| Quantité strictement positive | `Quantite.de` |
| Rendement > 2 × rendement de référence → refus suspect | `Culture.recolter` (rendement = quantité / superficie emblavée) |
| Une seule récolte par culture | `Culture.statut` (`RECOLTEE`), `Culture.recolter` |
| Campagne ouverte | `CampagneService.recolter` |

### US-08 — Livrer une récolte (Indispensable)

| Règle | Où |
|---|---|
| Cumul des livraisons ≤ quantité récoltée | `StockService.livrer` (somme des livraisons de la récolte) |
| Humidité ≤ seuil de la culture (instantané de la culture) | `CultureReference.humiditeAcceptee` |
| Humidité entre 0 et 100 % | `StockService.livrer` |
| Chaque livraison acceptée crée un lot identifiable, daté et rattaché au producteur | `StockService.livrer` |
| Livraison refusée : aucune écriture (ni lot, ni livraison) | validations avant `sauvegarder` |
| Campagne ouverte | `StockService.livrer` |

### US-09 — Consulter le stock (Importante)

| Règle | Où |
|---|---|
| Total = somme des quantités restantes des lots | `StockService.consulterStock` |
| Réservé = somme des quantités non vendues des offres non retirées | `ReservationsStockAdapter` |
| Disponible = max(total − réservé, 0) | `StockService.consulterStock` |
| Cohérence après vente, annulation, livraison | les quantités sont dérivées, jamais dupliquées |
| Réservé à l'agent | `Autorisations.exigerAgent` |

### US-10 — Publier une offre (Indispensable)

| Règle | Où |
|---|---|
| Quantité ≤ stock disponible (total − réservé) | `CommercialisationService.publierOffre` |
| Prix strictement positif | `Offre`, `Montant` |
| Publier réserve la quantité | `Offre.quantiteReservee` + `ReservationsStock` |
| Statuts `ouverte`, `épuisée`, `retirée` | `Offre.rafraichirStatut` |
| Retrait libère la quantité non vendue | `Offre.retirer`, `quantiteReservee` |
| Retrait impossible avec des commandes en attente | `Offre.retirer` |
| Écart > 30 % du prix de référence → avertissement, pas de refus | `AnalysePrix`, retour `avertissementPrix` |

### US-11 — Passer une commande (Indispensable)

| Règle | Où |
|---|---|
| Quantité ≤ quantité restante de l'offre | `Offre.commander` |
| Quantité minimale 50 kg | `Commande.creer` (seuil injecté) |
| Montant = quantité × prix au kg, arrondi au franc (demi vers le haut) | `Montant.fois` |
| Statut initial `en_attente` | `Commande` |
| Offre `épuisée` quand plus rien ne reste | `Offre.rafraichirStatut` |
| Réservé à un acheteur | `Autorisations.exigerAcheteur` |

### US-12 — Annuler une commande (Importante)

| Règle | Où |
|---|---|
| Annulation possible tant que la commande n'est pas livrée | `Commande.annuler` |
| Commande livrée : refus | `Commande.annuler` |
| Commande déjà annulée : refus | `Commande.annuler` |
| La quantité annulée redevient disponible dans l'offre | `Offre.annuler` |
| Un acheteur n'annule que ses propres commandes (sinon 404) | `CommercialisationService.commandeDeLAcheteur` |

### US-13 — Livrer une commande et répartir (Indispensable)

| Règle | Où |
|---|---|
| Seule une commande en attente peut être livrée | `CommercialisationService.livrerCommande` |
| Consommation des lots du plus ancien au plus récent | `LotRepository.parCultureEtQualite` (tri date + id) |
| Stock physique suffisant, sinon conflit | `CommercialisationService.livrerCommande` |
| Commission de 5 % avant répartition, arrondie au franc inférieur | `Montant.pourcentageArrondiInferieur` |
| Répartition proportionnelle aux kg apportés dans les lots consommés | `RepartitionPaiement.calculer` |
| Somme des paiements + commission = montant exact de la commande | `RepartitionPaiement` (méthode des plus forts restes) + contrôle final |
| Paiements créés au statut `à payer` | `PaiementDu` |
| Commande déjà livrée ou annulée : refus | `Commande.livrer` |
| Réservé à un agent | `Autorisations.exigerAgent` |

**Déroulé** :

1. vérifier que la commande est en attente ;
2. lire les lots de la culture et de la qualité, triés par date de réception ;
3. consommer les lots jusqu'à hauteur de la quantité commandée, en cumulant les kg par producteur ;
4. commission = plancher(montant × 5 %) ;
5. montant net = montant − commission ;
6. répartir le net selon les kg apportés (plus forts restes) ;
7. enregistrer un `PaiementDu` par producteur ;
8. passer la commande à `LIVREE`, décrémenter la réservation de l'offre ;
9. notifier les paiements par SMS.

### US-14 — Être prévenu par SMS (Importante)

| Règle | Où |
|---|---|
| Message en français avec le montant | `NotificationPaiementService` |
| Un seul SMS par paiement | `PaiementDu.smsDejaTraite` |
| Échec d'envoi : livraison valide, échec tracé (`ECHEC` + journal) | `NotificationPaiementService.notifier` |
| Aucun renvoi automatique d'un paiement déjà traité | `NotificationPaiementService.notifier` |

Le port `SmsPort` permet de remplacer la simulation par un vrai fournisseur (**E2**).

### US-15 — Importer des producteurs en lot (Importante)

| Règle | Où |
|---|---|
| Format `nom;prenom;telephone;village` | `ProducteurService.importerCsv` |
| Mêmes règles que l'enregistrement unitaire | réutilisation de `enregistrer` |
| Lignes valides enregistrées, lignes invalides rapportées avec numéro de ligne et motif | `ResultatImport` |
| Compte rendu final : acceptées / refusées | `ResultatImport` |
| En-tête facultative, BOM UTF-8 accepté, lignes vides ignorées | `importerCsv` |
| Réservé à un agent | `Autorisations.exigerAgent` |

### US-16 — Historique de mes commandes (Importante)

| Règle | Où |
|---|---|
| Pagination `page` / `taille` (défaut 20, plafond 100) | `CommercialisationService.historiqueCommandes` |
| Tri du plus récent au plus ancien | `CommandeRepositoryMemoire.parAcheteur` (date, puis id, inversés) |
| Filtre par statut | paramètre `statut` |
| Un acheteur ne voit que ses commandes | filtre sur `acheteurId` + `Autorisations.exigerAcheteur` |
| Réponses légères | projection `PageCommandesVue` (pas de chargement des paiements) |

### US-17 — Accéder uniquement à ce qui me concerne (Indispensable)

| Règle | Où |
|---|---|
| Producteur : ses parcelles, cultures, récoltes, paiements uniquement | `Autorisations.accederProducteur` |
| Acheteur : offres ouvertes et ses commandes | `offresVisibles`, `commercialisation` |
| Agent : données de la coopérative | `Autorisations.exigerAgent` |
| Ressource d'autrui : 404, sans révéler son existence | `RessourceIntrouvableException` (même message que l'inexistence) |
| Opération interdite par le rôle : 403 | `AccesRefuseException` |

La distinction est volontaire : un **rôle** insuffisant est un 403 explicite ; une **ressource**
hors périmètre est un 404 indistinguable d'une ressource inexistante.

### US-18 — Prix de référence du marché (Bonus)

| Règle | Où |
|---|---|
| Prix fourni par un service externe | `PrixMarchePort`, `PrixMarcheSimuleAdapter` |
| Service indisponible → dernier prix connu, avec sa date | adaptateur (`issuDuCache = true`) |
| L'application continue de fonctionner | repli sur cache, jamais d'exception |
| Écart > 30 % → offre publiée avec avertissement | `AnalysePrix.avertissementSiEcart` |
| Seuil paramétrable | `cooperative.ecart-prix-pct` |
| Consultation réservée à l'agent | `ParametreService.prixDeReference` |

### US-19 — Gérer les paramètres des cultures (Bonus)

| Règle | Où |
|---|---|
| Consulter / modifier rendement, humidité maximale, cycle minimal | `ParametreService`, `ReferentielCultures` |
| Modification appliquée aux déclarations futures uniquement | instantané `CultureReference` dans `Culture` |
| Valeurs strictement positives ; humidité ≤ 100 % | `CultureReference` |
| Réservé à un administrateur | `Autorisations.exigerAdministrateur` |

---

## 5. Règle d'arrondi monétaire

Les montants sont en francs CFA, sans sous-unité, donc entiers. Deux arrondis interviennent :

1. **Montant de la commande** : `quantité × prix`, arrondi au franc le plus proche
   (`HALF_UP`). Si les quantités sont entières, aucun arrondi n'a lieu.
2. **Commission** : `plancher(montant × 5 %)`. La coopérative ne s'attribue jamais un franc
   qu'elle n'a pas encaissé.
3. **Répartition du net** : chaque part est calculée au prorata des kg, arrondie au franc
   **inférieur**, puis les francs restants sont distribués un par un aux plus grands restes
   (à égalité, ordre des identifiants producteur pour être déterministe).

Conséquence garantie et testée : `somme(paiements) + commission = montant de la commande`.
Aucun franc ne se perd ni n'apparaît. Voir [ADR-004](DECISIONS.md).

---

## 6. Hypothèses retenues

Le sujet laisse certaines règles ouvertes. Les hypothèses suivantes sont assumées :

- **H1 — Récolte en une seule opération.** Le sujet distingue la date de récolte (US-06) et la
  quantité (US-07). Pour éviter deux saisies incohérentes, l'API enregistre les deux ensemble :
  `POST /cultures/{id}/recolte {dateRecolte, quantiteKg}`. Le statut passe à `récoltée` à cette occasion.
- **H2 — Cycle minimal inclusif.** La récolte est acceptée à partir de `dateSemis + cycleMinJours`
  (le jour du cycle est autorisé).
- **H3 — Seuil d'humidité inclusif.** Une humidité égale au maximum de la culture est acceptée ;
  seule une humidité strictement supérieure est refusée.
- **H4 — Rendement suspect strict.** Le refus intervient seulement si le rendement dépasse
  *strictement* deux fois la référence. Le rendement est calculé sur la superficie emblavée.
- **H5 — Réservation d'offre.** Une offre ouverte immobilise toute sa quantité non encore vendue,
  qu'elle soit déjà commandée ou non. Une annulation libère la quantité pour de nouvelles
  commandes sur la même offre, mais ne réduit pas la réservation de stock tant que l'offre
  n'est pas retirée.
- **H6 — Retrait d'offre.** Une offre ne peut être retirée s'il reste des commandes en attente ;
  l'agent doit d'abord attendre leur livraison ou leur annulation. Cela évite de laisser des
  commandes sans marchandise réservée.
- **H7 — Stock physique.** Le stock total est la somme des quantités *restantes* des lots.
  La livraison d'une commande décrémente donc le total ; le réservé correspond aux offres
  encore ouvertes.
- **H8 — Qualité.** Une offre porte une seule qualité ; seuls les lots de cette culture et de
  cette qualité sont consommés. Les qualités ne sont pas substituables.
- **H9 — Annulation.** Une commande annulée n'est plus livrable. Une commande livrée n'est plus
  annulable. Les états finaux sont définitifs.
- **H10 — Paiement.** Le paiement dû est créé au statut `à payer` ; le règlement effectif
  (mobile money, virement) est hors périmètre. Aucun endpoint ne marque un paiement comme payé.
- **H11 — SMS.** Un SMS en échec n'est pas réessayé automatiquement : il reste `ECHEC` et
  pourra être repris par un traitement ultérieur (hors périmètre). La livraison reste valide.
- **H12 — Identité.** L'authentification est simulée par les en-têtes `X-Utilisateur-Id` et
  `X-Role`. Pour un producteur, `X-Utilisateur-Id` doit être son identifiant producteur.
- **H13 — Import CSV.** Les numéros de ligne sont ceux du fichier physique (en-tête incluse).
  Les lignes vides sont ignorées. Une ligne d'en-tête commençant par `nom;` est ignorée.
- **H14 — Paramètres.** Les paramètres de culture s'appliquent aux déclarations futures.
  Une culture conserve les valeurs en vigueur au moment de sa déclaration, y compris pour
  les contrôles de récolte et d'humidité.
- **H15 — Dates de campagne.** Aucun contrôle n'impose que le semis ou la récolte tombent dans
  la période de la campagne ; seule l'ouverture de la campagne est vérifiée. Les dates de
  campagne servent au calendrier et à l'unicité saison/année.
- **H16 — Concurrence.** Le projet est un cœur métier mono-instance ; la persistance en mémoire
  est thread-safe pour les collections, mais aucune transaction distribuée n'est gérée.
  Un adaptateur SQL apporterait les verrous et transactions nécessaires.

---

## 7. Erreurs et contrat

Toutes les exceptions métier héritent de `DomaineException` et portent un **code stable** :

| Exception | Code | HTTP |
|---|---|---|
| `RegleMetierException` | `REGLE_METIER` | 400 |
| `RessourceIntrouvableException` | `RESSOURCE_INTROUVABLE` | 404 |
| `ConflitException` | `CONFLIT` | 409 |
| `AccesRefuseException` | `ACCES_REFUSE` | 403 |
| Erreur non gérée | `ERREUR_INTERNE` | 500 |
| Corps/paramètre invalide | `REQUETE_INVALIDE` | 400 |

Le corps est toujours `{code, message, details, horodatage, chemin}`. Aucune trace technique
n'est exposée ; les détails sont journalisés côté serveur. Le contrat OpenAPI déclare ce schéma
`ErreurApi` et les réponses 400/403/404/409/500 sur chaque opération.

---

## 8. Stratégie de tests

| Niveau | Ce qui est vérifié | Démarrage |
|---|---|---|
| **Domaine** | Objets-valeurs, invariants des agrégats, transitions, répartition, analyse de prix | Aucun contexte Spring |
| **Application** | Règles de chaque story, autorisations, scénario complet, import, pagination | Câblage manuel, adaptateurs en mémoire |
| **API** | Contrat HTTP réel, format d'erreur, en-têtes, non-divulgation, présence OpenAPI | Contexte Spring + MockMvc |

Les tests métier n'utilisent ni base ni serveur (**E1**). L'horloge et les identifiants sont
injectés (`HorlogeFixe`, `IdSequence`) pour des résultats déterministes. Le scénario complet
(`ScenarioCompletTest`) rejoue la démonstration et vérifie les montants exacts.

---

## 9. Évolutions prévues

- Remplacer les adaptateurs en mémoire par JPA/PostgreSQL sans toucher au domaine.
- Brancher un vrai fournisseur SMS et une vraie API de prix (mêmes ports).
- Ajouter l'authentification réelle (JWT) : seule la résolution d'`Identite` change.
- Ajouter le règlement effectif des paiements (nouvel état et endpoint).
- Introduire des événements de domaine pour découpler notification et livraison.
- Passer à une API `/v2` en conservant `/v1` (voir ADR-006).
