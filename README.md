# SunuAgri

Cœur de la plateforme numérique de la coopérative agricole **SunuAgri** (cas fictif, Sénégal).
Le projet couvre tout le cycle métier : enregistrement des producteurs et de leurs parcelles,
campagnes et cultures, récoltes, livraisons à l'entrepôt, stock, offres de vente, commandes,
livraison des commandes, répartition du montant et paiements dus aux producteurs.

Projet individuel du Module 5.1 — *DDD, architecture hexagonale et API Design*.

---

## 1. Pile technique

| Élément | Choix |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot 3.3.5 (Web, Validation) |
| Contrat d'API | OpenAPI 3, documenté avec springdoc |
| Tests | JUnit 5 + AssertJ + MockMvc |
| Build | Maven 3.9+ |
| Persistance | Adaptateurs en mémoire (voir [ADR-002](docs/DECISIONS.md)) |
| Intégrations | SMS et prix du marché simulés |

L'application ne dépend d'aucune base de données : elle démarre avec un stock de données vide
et se réinitialise à chaque redémarrage.

---

## 2. Lancer le projet

Prérequis : JDK 21 et Maven 3.9+.

```bash
cd sunuagri
mvn spring-boot:run
```

L'API écoute sur <http://localhost:8080>.

| Ressource | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| Contrat OpenAPI (YAML) | [`docs/openapi.yaml`](docs/openapi.yaml) |
| Contrat OpenAPI (runtime) | <http://localhost:8080/v3/api-docs> |

Les paramètres métier (superficie maximale, commande minimale, commission, écart de prix)
sont dans `src/main/resources/application.yml`, section `cooperative`.

---

## 3. Lancer les tests

```bash
mvn test
```

- **61 tests** automatisés, tous verts.
- Les tests métier (domaine et application) sont **rapides** : ils n'ouvrent ni serveur web
  ni base de données. Ils câblent les services et les adaptateurs en mémoire à la main
  (`src/test/java/sn/sunuagri/support/ContexteTest.java`), conformément à l'exigence **E1**.
- Seule la classe `ApiIntegrationTest` démarre un contexte Spring pour vérifier le contrat
  HTTP, le format d'erreur et l'absence de fuite d'information.

Répartition :

| Suite | Tests |
|---|---|
| Domaine (objets-valeurs, agrégats, répartition) | 25 |
| Application (règles par story, scénario complet, autorisations) | 31 |
| API (MockMvc, erreurs, OpenAPI) | 5 |

---

## 4. Démonstration complète

L'application doit tourner. Puis :

```bash
./scripts/demo.sh
```

Le script déroule le scénario complet, de l'enregistrement d'un producteur au paiement dû :
producteur → parcelle → campagne → culture → semis → récolte → livraisons → stock →
offre → commande → livraison de commande → commission → paiement → SMS → contrôle d'accès
→ import CSV → paramètres. Voir [`docs/DEMO.md`](docs/DEMO.md) pour le déroulé commenté
et les résultats attendus.

---

## 5. Architecture

Architecture **hexagonale** : le domaine et les cas d'usage ne connaissent ni Spring, ni HTTP,
ni la persistance.

```
sn.sunuagri
├── domaine/                 # Cœur métier pur (aucune dépendance technique)
│   ├── commun/              # Identite, Role, Telephone, Quantite, Montant, Superficie...
│   ├── producteur/          # Agrégat Producteur (entité Parcelle)
│   ├── campagne/            # Agrégat Campagne (entité Culture)
│   ├── culture/             # Récolte, référentiel des cultures, paramètres
│   ├── stock/               # Agrégat Lot, Livraison, port ReservationsStock
│   ├── commercialisation/   # Offre, Commande, PaiementDu, RepartitionPaiement
│   └── marche/              # PrixMarche, AnalysePrix
├── application/             # Cas d'usage (services) + ports sortants
│   └── port/                # SmsPort, PrixMarchePort, HorlogePort, IdGeneratorPort
└── adapters/
    ├── web/                 # Contrôleurs REST, DTO, erreurs, identité simulée, OpenAPI
    ├── persistence/memoire/ # Adaptateurs des ports de persistance
    ├── integration/         # SMS simulé, prix du marché simulé
    └── systeme/             # Horloge système, générateur d'identifiants UUID
```

Règles de dépendance : `adapters → application → domaine`. Le domaine ne dépend de rien.
Les ports sortants sont déclarés côté domaine ou application et implémentés côté adaptateurs,
ce qui permet de remplacer la base, le service SMS ou la source de prix sans toucher aux règles
(exigence **E2**). Chaque règle métier n'existe qu'à un seul endroit, dans l'agrégat concerné
(exigence **E3**).

Détails : [`docs/CONCEPTION.md`](docs/CONCEPTION.md) et [`docs/DECISIONS.md`](docs/DECISIONS.md).

---

## 6. Conventions d'API

- **Base** : `/api/v1`, contenu JSON, dates ISO-8601 (`2026-06-05`), montants en francs CFA entiers.
- **Identité simulée** : chaque requête porte les en-têtes `X-Utilisateur-Id` et `X-Role`
  (`PRODUCTEUR`, `ACHETEUR`, `AGENT`, `ADMINISTRATEUR`). L'authentification réelle est hors périmètre.
- **Erreurs** : format unique, sans détail interne.

```json
{
  "code": "REGLE_METIER",
  "message": "La quantité minimale d'une commande est de 50 kg",
  "details": null,
  "horodatage": "2026-10-30T15:12:25.983684Z",
  "chemin": "/api/v1/commandes"
}
```

Codes HTTP : `400` règle métier ou requête mal formée, `403` rôle insuffisant,
`404` ressource inexistante **ou hors du périmètre de l'utilisateur** (aucune fuite d'existence),
`409` conflit avec l'état courant, `500` erreur interne.

- **Pagination** : `page` (défaut 0) et `taille` (défaut 20, plafond 100).
- **Évolution** : version dans le chemin (`/api/v1`), ajouts de champs uniquement, codes d'erreur
  stables (voir [ADR-006](docs/DECISIONS.md)).

### Endpoints

| Méthode | Chemin | Rôle | Story |
|---|---|---|---|
| POST | `/api/v1/producteurs` | Agent | US-01 |
| GET | `/api/v1/producteurs` | Agent | US-01 |
| POST | `/api/v1/producteurs/import` | Agent | US-15 |
| POST | `/api/v1/producteurs/{id}/parcelles` | Agent | US-02 |
| GET | `/api/v1/producteurs/{id}/parcelles` | Agent, producteur concerné | US-03 |
| GET | `/api/v1/producteurs/{id}/parcelles/{parcelleId}` | Agent, producteur concerné | US-03 |
| GET | `/api/v1/producteurs/{id}/cultures` | Agent, producteur concerné | US-06 |
| GET | `/api/v1/producteurs/{id}/recoltes` | Agent, producteur concerné | US-07 |
| GET | `/api/v1/producteurs/{id}/paiements` | Agent, producteur concerné | US-14, US-17 |
| POST | `/api/v1/campagnes` | Agent | US-04 |
| GET | `/api/v1/campagnes` | Agent | US-04 |
| POST | `/api/v1/campagnes/{id}/cloture` | Agent | US-04 |
| GET | `/api/v1/campagnes/{id}/cultures` | Agent | US-05 |
| POST | `/api/v1/cultures` | Agent | US-05 |
| POST | `/api/v1/cultures/{id}/semis` | Agent | US-06 |
| POST | `/api/v1/cultures/{id}/recolte` | Agent | US-06, US-07 |
| POST | `/api/v1/recoltes/{id}/livraisons` | Agent | US-08 |
| GET | `/api/v1/stock` | Agent | US-09 |
| POST | `/api/v1/offres` | Agent | US-10, US-18 |
| GET | `/api/v1/offres` | Tous | US-10 |
| POST | `/api/v1/offres/{id}/retrait` | Agent | US-10 |
| POST | `/api/v1/commandes` | Acheteur | US-11 |
| POST | `/api/v1/commandes/{id}/annulation` | Acheteur | US-12 |
| GET | `/api/v1/commandes` | Acheteur | US-16 |
| POST | `/api/v1/commandes/{id}/livraison` | Agent | US-13 |
| GET | `/api/v1/commandes/{id}/paiements` | Agent | US-13 |
| GET | `/api/v1/prix-marche/{culture}` | Agent | US-18 |
| GET | `/api/v1/parametres/cultures` | Administrateur | US-19 |
| PUT | `/api/v1/parametres/cultures/{culture}` | Administrateur | US-19 |

---

## 7. Stories réalisées

Toutes les stories **Indispensables**, **Importantes** et **Bonus** sont réalisées.

| Story | Priorité | État | Où |
|---|---|---|---|
| US-01 Enregistrer un producteur | Indispensable | ✅ | `ProducteurService`, `Producteur` |
| US-02 Déclarer une parcelle | Indispensable | ✅ | `Producteur.declarerParcelle` |
| US-03 Consulter les parcelles | Importante | ✅ | `ProducteurService.parcelles` |
| US-04 Ouvrir / clôturer une campagne | Indispensable | ✅ | `Campagne`, `CampagneService` |
| US-05 Déclarer une culture | Indispensable | ✅ | `Campagne.declarerCulture` |
| US-06 Suivre l'avancement d'une culture | Importante | ✅ | `Culture.semer`, `Culture.recolter` |
| US-07 Déclarer une récolte | Indispensable | ✅ | `Culture.recolter`, `Recolte` |
| US-08 Livrer une récolte | Indispensable | ✅ | `StockService.livrer`, `Lot` |
| US-09 Consulter le stock | Importante | ✅ | `StockService.consulterStock` |
| US-10 Publier une offre | Indispensable | ✅ | `Offre`, `CommercialisationService` |
| US-11 Passer une commande | Indispensable | ✅ | `Commande.creer` |
| US-12 Annuler une commande | Importante | ✅ | `Commande.annuler` |
| US-13 Livrer une commande et répartir | Indispensable | ✅ | `CommercialisationService.livrerCommande` |
| US-14 Être prévenu par SMS | Importante | ✅ | `NotificationPaiementService`, `SmsPort` |
| US-15 Importer des producteurs CSV | Importante | ✅ | `ProducteurService.importerCsv` |
| US-16 Historique de mes commandes | Importante | ✅ | `CommercialisationService.historiqueCommandes` |
| US-17 Accéder uniquement à ce qui me concerne | Indispensable | ✅ | `Autorisations`, `UtilisateurCourantResolver` |
| US-18 Prix de référence du marché | Bonus | ✅ | `PrixMarcheSimuleAdapter`, `AnalysePrix` |
| US-19 Gérer les paramètres des cultures | Bonus | ✅ | `ParametreService`, `CultureReference` |

### Exigences transversales

| Réf. | Réponse |
|---|---|
| E1 | Tests métier sans Spring, sans web, sans base : `ContexteTest` câble les adaptateurs en mémoire. |
| E2 | Ports sortants + adaptateurs interchangeables (persistance, SMS, prix). |
| E3 | Une règle = un endroit : les invariants vivent dans les agrégats, pas dans les contrôleurs. |
| E4 | Vocabulaire métier français dans le code, l'API et les documents (`docs/GLOSSAIRE.md`). |
| E5 | Contrat OpenAPI généré depuis le code, exporté dans `docs/openapi.yaml`, testé par `ApiIntegrationTest`. |
| E6 | `GestionnaireErreurs` : un seul format, aucun détail technique. |
| E7 | API versionnée `/api/v1`, codes d'erreur stables, ajouts compatibles (ADR-006). |
| E8 | Décisions documentées : `docs/DECISIONS.md` (6 ADR). |

---

## 8. Livrables

| Livrable | Fichier |
|---|---|
| Code source et tests | `src/main`, `src/test` |
| Contrat OpenAPI | [`docs/openapi.yaml`](docs/openapi.yaml) |
| Glossaire | [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md) |
| Document de conception | [`docs/CONCEPTION.md`](docs/CONCEPTION.md) |
| Décisions d'architecture (6 ADR) | [`docs/DECISIONS.md`](docs/DECISIONS.md) |
| Démonstration du scénario complet | [`docs/DEMO.md`](docs/DEMO.md), `scripts/demo.sh` |
| Historique Git lisible | dépôt initialisé avec des commits thématiques |

---

## 9. Limites assumées

- Persistance en mémoire : les données sont perdues au redémarrage. C'est un choix documenté
  (ADR-002), réversible par un adaptateur JPA sans modifier le domaine.
- Identité simulée par en-têtes : aucune authentification réelle (hors périmètre du sujet).
- SMS simulé et prix du marché simulé : aucun appel réseau réel.
- Pas d'écrans : seuls l'API, la documentation et la démonstration en ligne de commande sont livrés.
