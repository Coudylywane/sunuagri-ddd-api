# Démonstration — du producteur au paiement dû

Ce document décrit le scénario complet rejoué par [`scripts/demo.sh`](../scripts/demo.sh)
et par le test `ScenarioCompletTest`. Il permet de vérifier de bout en bout que le système
respecte les règles métier, y compris la conservation exacte des montants.

---

## 1. Préparer la démonstration

```bash
# Terminal 1 : démarrer l'application (données en mémoire, donc repartir à zéro)
cd sunuagri
mvn spring-boot:run

# Terminal 2 : jouer le scénario
./scripts/demo.sh
```

Prérequis : `jq` installé. L'application doit être **fraîchement démarrée** : la persistance est
en mémoire et le scénario utilise des numéros de téléphone fixes (ADR-002).

La démonstration est aussi vérifiée automatiquement, sans serveur :

```bash
mvn -Dtest=ScenarioCompletTest test
```

---

## 2. Le scénario en un coup d'œil

| Étape | Action | Résultat attendu |
|---|---|---|
| 1 | Agent enregistre Awa Diop (77 123 45 67, Thiès) | identifiant producteur, téléphone normalisé |
| 2 | Déclare la parcelle « Nord » de 10 ha | parcelle créée |
| 3 | Consulte les parcelles | nom, commune, superficie |
| 4 | Ouvre la campagne hivernage 2026 | campagne ouverte |
| 5 | Déclare 10 ha d'arachide | culture planifiée, paramètres figés (1 200 kg/ha, 9 %, 90 j) |
| 6 | Sème le 5 juin 2026 | statut semée |
| 7 | Récolte 11 000 kg le 5 octobre 2026 | statut récoltée, rendement 1 100 kg/ha ≤ 2 400 |
| 8 | Livre 8 000 kg puis 2 000 kg (qualité A, 8 %) | deux lots, stock 10 000 kg |
| 9 | Consulte le stock | total 10 000, réservé 0, disponible 10 000 |
| 10 | Consulte le prix du marché de l'arachide | 300 FCFA/kg |
| 11 | Publie une offre de 6 000 kg à 350 FCFA/kg | offre ouverte, pas d'avertissement (écart 16,7 %) |
| 12 | Consulte le stock | réservé 6 000, disponible 4 000 |
| 13 | L'acheteur commande 3 000 kg | montant 1 050 000 FCFA, statut en attente |
| 14 | Consulte son historique | 1 commande, paginée |
| 15 | L'agent livre la commande | commission 52 500, net 997 500, 1 paiement dû |
| 16 | Le producteur consulte ses paiements | 997 500 FCFA, à payer, SMS envoyé |
| 17 | Consulte le stock final | total 7 000, réservé 3 000, disponible 4 000 |
| 18 | Un autre producteur tente d'accéder aux paiements | HTTP 404, sans révéler l'existence |
| 19 | Import CSV de 3 lignes | 2 acceptées, 1 refusée avec motif et ligne |
| 20 | L'administrateur modifie le rendement du mil | 850 kg/ha, effet sur les déclarations futures |

---

## 3. Déroulé détaillé et résultats

### Étape 1 — US-01 : enregistrer un producteur

```http
POST /api/v1/producteurs
X-Utilisateur-Id: agent-1
X-Role: AGENT

{"nom":"Diop","prenom":"Awa","telephone":"77 123 45 67","village":"Thiès"}
```

```json
{
  "id": "6e1e5ca3-f4a6-4e4b-8249-8cc45636ea30",
  "nom": "Diop",
  "prenom": "Awa",
  "telephone": "771234567",
  "village": "Thiès"
}
```

Le numéro est normalisé sur 9 chiffres. Un second enregistrement avec le même numéro renverrait
`409 CONFLIT` avec le message « Un producteur est déjà enregistré avec le téléphone 771234567 ».

### Étape 2 — US-02 : déclarer une parcelle de 10 ha

```http
POST /api/v1/producteurs/{producteurId}/parcelles
{"nom":"Nord","superficieHa":10,"commune":"Thiès"}
```

La parcelle reçoit un identifiant. Le total des superficies du producteur reste ≤ 50 ha et deux
parcelles ne peuvent pas porter le même nom.

### Étapes 3 à 5 — US-03, US-04, US-05 : campagne et culture

```http
POST /api/v1/campagnes
{"saison":"hivernage","annee":2026,"dateDebut":"2026-06-01","dateFin":"2026-11-30"}
```

```http
POST /api/v1/cultures
{"campagneId":"…","parcelleId":"…","culture":"Arachide","superficieHa":10}
```

La culture est créée avec le statut `planifiee` et **fige** les paramètres de l'arachide :
rendement 1 200 kg/ha, humidité maximale 9 %, cycle minimal 90 jours. Une future modification
du référentiel ne changera pas cette culture (US-19).

### Étape 6 — US-06 : semer

```http
POST /api/v1/cultures/{cultureId}/semis
{"dateSemis":"2026-06-05"}
```

Statut `semee`.

### Étape 7 — US-07 : récolter 11 000 kg le 5 octobre

```http
POST /api/v1/cultures/{cultureId}/recolte
{"dateRecolte":"2026-10-05","quantiteKg":11000}
```

- 122 jours entre semis et récolte ≥ 90 : accepté.
- Rendement = 11 000 / 10 = 1 100 kg/ha, inférieur au double de 1 200 : accepté.
- La culture passe à `recoltee` et ne pourra plus être récoltée.

Une récolte le 1er septembre (88 jours) serait refusée (« doit être postérieure d'au moins
90 jours »). Une récolte de 25 000 kg serait refusée comme suspecte (rendement > 2 400 kg/ha).

### Étape 8 — US-08 : livrer à l'entrepôt

```http
POST /api/v1/recoltes/{recolteId}/livraisons
{"quantiteKg":8000,"tauxHumiditePct":8,"qualite":"A"}
```

```json
{"id":"fdf62b8b-…","lotId":"707ca59e-…","qualite":"A","quantiteKg":8000,
 "tauxHumiditePct":8,"date":"2026-10-30"}
```

Puis une seconde livraison de 2 000 kg qualité A. Chaque livraison crée un lot distinct, daté et
rattaché au producteur. L'humidité de 8 % est sous le seuil de 9 % ; une livraison à 10 % serait
refusée et **ne créerait rien**. Le cumul livré (10 000) ne dépasse pas la récolte (11 000).

### Étape 9 — US-09 : consulter le stock

```json
{"culture":"Arachide","qualite":"A","totalKg":10000,"reserveKg":0,"disponibleKg":10000}
```

### Étape 10 — US-18 : prix de référence

```json
{"culture":"Arachide","prixKg":300,"date":"2026-10-30","issuDuCache":false}
```

Si le service simulé tombe en panne, le dernier prix connu est renvoyé avec `issuDuCache: true`
et sa date : l'application continue de fonctionner.

### Étape 11 — US-10 : publier une offre

```http
POST /api/v1/offres
{"culture":"Arachide","qualite":"A","quantiteKg":6000,"prixKg":350}
```

```json
{
  "offre": {
    "id": "d211b97a-…",
    "culture": "Arachide",
    "qualite": "A",
    "quantiteKg": 6000,
    "quantiteRestanteKg": 6000,
    "quantiteVendueKg": 0,
    "prixKg": 350,
    "statut": "ouverte"
  },
  "avertissementPrix": null
}
```

L'écart au prix de référence est de `|350 − 300| / 300 = 16,67 %`, sous le seuil de 30 % :
aucun avertissement. À 500 FCFA/kg, l'offre serait **publiée quand même** avec un
`avertissementPrix` explicite.

### Étape 12 — US-09 : le stock réserve l'offre

```json
{"culture":"Arachide","qualite":"A","totalKg":10000,"reserveKg":6000,"disponibleKg":4000}
```

Une deuxième offre de plus de 4 000 kg serait refusée.

### Étape 13 — US-11 : passer commande

```http
POST /api/v1/commandes
X-Utilisateur-Id: acheteur-1
X-Role: ACHETEUR

{"offreId":"…","quantiteKg":3000}
```

```json
{
  "id": "1e27b602-…",
  "quantiteKg": 3000,
  "prixKg": 350,
  "montantFrancs": 1050000,
  "statut": "en_attente"
}
```

Montant = 3 000 × 350 = **1 050 000 FCFA**. Moins de 50 kg serait refusé, plus que la quantité
restante aussi.

### Étape 14 — US-16 : historique

`GET /api/v1/commandes?page=0&taille=10` renvoie la page de commandes de l'acheteur, de la plus
récente à la plus ancienne, avec `total`, `page` et `taille`.

### Étape 15 — US-13 : livrer la commande et répartir

```http
POST /api/v1/commandes/{commandeId}/livraison
X-Utilisateur-Id: agent-1
X-Role: AGENT
```

```json
{
  "commande": "1e27b602-…",
  "statut": "livree",
  "montantFrancs": 1050000,
  "commissionFrancs": 52500,
  "montantNetFrancs": 997500,
  "paiements": [
    {
      "id": "35d8d960-…",
      "producteurId": "6e1e5ca3-…",
      "montantFrancs": 997500,
      "statut": "a_payer",
      "statutSms": "envoye"
    }
  ]
}
```

Calculs :

| Élément | Valeur |
|---|---|
| Montant de la commande | 3 000 kg × 350 FCFA = **1 050 000** |
| Commission (5 %, plancher) | **52 500** |
| Montant net réparti | **997 500** |
| Contrôle | 997 500 + 52 500 = 1 050 000 ✅ |

La consommation est FIFO : le lot de 8 000 kg est consommé en premier (il reste 5 000 kg), puis
1 000 kg sont pris sur le lot de 2 000 kg (il reste 1 000 kg). Avec un seul producteur, il reçoit
tout le net. Avec plusieurs producteurs, chacun recevrait sa part au prorata des kg apportés.

### Étape 16 — US-14 : le producteur consulte ses paiements

```http
GET /api/v1/producteurs/{producteurId}/paiements
X-Utilisateur-Id: {producteurId}
X-Role: PRODUCTEUR
```

```json
{"montantFrancs":997500,"statut":"a_payer","statutSms":"envoye","commandeId":"1e27b602-…"}
```

Le SMS simulé contient : « Bonjour Awa Diop, un paiement de 997 500 FCFA vous est dû par la
coopérative SunuAgri pour la commande 1e27b602-…. » Il n'est envoyé qu'une fois par paiement.

### Étape 17 — US-09 : stock final

```json
{"culture":"Arachide","qualite":"A","totalKg":7000,"reserveKg":3000,"disponibleKg":4000}
```

- **Total 7 000** : 10 000 physiques − 3 000 livrés.
- **Réservé 3 000** : l'offre de 6 000 contenait 3 000 déjà vendus ; il reste 3 000 non vendus,
  toujours réservés par l'offre ouverte.
- **Disponible 4 000** : 7 000 − 3 000.

### Étape 18 — US-17 : non-divulgation

```http
GET /api/v1/producteurs/{producteurId}/paiements
X-Utilisateur-Id: un-autre-producteur
X-Role: PRODUCTEUR
```

```json
{
  "code": "RESSOURCE_INTROUVABLE",
  "message": "Producteur introuvable",
  "horodatage": "2026-10-30T15:12:25.983684Z",
  "chemin": "/api/v1/producteurs/6e1e5ca3-…/paiements"
}
```

HTTP **404**, exactement comme si le producteur n'existait pas : l'existence de la ressource
n'est pas révélée.

### Étape 19 — US-15 : import CSV

```http
POST /api/v1/producteurs/import
Content-Type: text/csv

nom;prenom;telephone;village
Fall;Moussa;772222222;Kaolack
Ndiaye;;773333333;Dakar
Sarr;Fatou;774444444;Fatick
```

```json
{
  "acceptes": 2,
  "refuses": 1,
  "erreurs": [
    {"ligne": 3, "motif": "Le prénom est obligatoire"}
  ]
}
```

Les lignes valides sont enregistrées ; la ligne 3 est rapportée avec son numéro et son motif,
sans bloquer les suivantes.

### Étape 20 — US-19 : modifier un paramètre de culture

```http
PUT /api/v1/parametres/cultures/Mil
X-Utilisateur-Id: admin-1
X-Role: ADMINISTRATEUR

{"rendementReferenceKgHa":850,"humiditeMaxPct":12,"cycleMinJours":75}
```

```json
{"code":"MIL","culture":"Mil","rendementReferenceKgHa":850,"humiditeMaxPct":12,"cycleMinJours":75}
```

Les cultures de mil déjà déclarées conservent leurs anciennes valeurs ; seules les nouvelles
déclarations utilisent 850 kg/ha. Une valeur nulle ou négative est refusée (`400`), et un agent
non administrateur reçoit `403`.

---

## 4. Variantes utiles

### Annuler une commande (US-12)

```bash
curl -X POST http://localhost:8080/api/v1/commandes/{id}/annulation \
  -H "X-Utilisateur-Id: acheteur-1" -H "X-Role: ACHETEUR"
```

La quantité redevient commandable sur l'offre. Annuler une commande déjà livrée renvoie `409`.

### Panne du service SMS (US-14)

La simulation expose un commutateur interne (`SmsSimuleAdapter.simulerPanne`). Le test
`PaiementsTest.un_echec_de_sms_est_trace_sans_invalider_la_livraison` vérifie que la livraison
reste valide, que le paiement est créé et que `statutSms` vaut `echec`.

### Offre hors prix (US-18)

Publier une offre d'arachide à 500 FCFA/kg renvoie `avertissementPrix` non nul, mais l'offre est
bien créée avec le statut `ouverte`.

### Stock insuffisant (US-13)

Tenter de livrer une commande dont la marchandise a déjà été vendue renvoie `409 CONFLIT`
avec le stock physique disponible.

### Erreurs transverses (E6)

```bash
# rôle insuffisant → 403
curl -i -X POST http://localhost:8080/api/v1/producteurs \
  -H "X-Utilisateur-Id: acheteur-1" -H "X-Role: ACHETEUR" \
  -H "Content-Type: application/json" \
  -d '{"nom":"X","prenom":"Y","telephone":"770000000","village":"Z"}'

# en-têtes d'identité absents → 400
curl -i http://localhost:8080/api/v1/stock
```

---

## 5. Ce que la démonstration prouve

1. **Cohérence du cycle complet** : chaque étape s'appuie sur les précédentes, sans données
   injectées artificiellement.
2. **Respect des règles** : les refus (humidité, cycle, rendement, stock, quantité minimale,
   rôles) sont testés séparément dans les suites automatisées.
3. **Exactitude comptable** : 1 050 000 = 52 500 + 997 500, sans perte de franc.
4. **Traçabilité** : chaque livraison crée un lot, chaque commande un montant, chaque paiement
   un statut SMS.
5. **Sécurité minimale** : un utilisateur ne voit pas les données d'un autre, et ne peut pas
   déduire leur existence.
