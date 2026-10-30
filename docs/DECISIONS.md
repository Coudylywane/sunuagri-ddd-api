# Décisions d'architecture (ADR)

Chaque décision suit le format : **contexte**, **décision**, **conséquences**, **alternatives
écartées**. Les décisions sont datées du rendu initial (octobre 2026).

Sommaire :

1. [ADR-001 — Domaine pur et architecture hexagonale](#adr-001--domaine-pur-et-architecture-hexagonale)
2. [ADR-002 — Persistance par adaptateurs en mémoire](#adr-002--persistance-par-adaptateurs-en-mémoire)
3. [ADR-003 — Identité simulée et non-divulgation des ressources](#adr-003--identité-simulée-et-non-divulgation-des-ressources)
4. [ADR-004 — Arrondis monétaires et répartition sans perte de franc](#adr-004--arrondis-monétaires-et-répartition-sans-perte-de-franc)
5. [ADR-005 — Paramètres métier externalisés et instantané des paramètres de culture](#adr-005--paramètres-métier-externalisés-et-instantané-des-paramètres-de-culture)
6. [ADR-006 — Contrat OpenAPI, format d'erreur unique et versionnement](#adr-006--contrat-openapi-format-derreur-unique-et-versionnement)

---

## ADR-001 — Domaine pur et architecture hexagonale

**Statut** : acceptée.

### Contexte

Le sujet exige que les règles métier soient testables sans serveur ni base (E1), que les
technologies périphériques soient remplaçables (E2) et qu'une règle n'existe qu'à un seul
endroit (E3). Les règles de SunuAgri sont riches : plafonds de superficie, cycles de culture,
FIFO, répartition de paiements. Les mélanger au code Spring ou SQL rendrait leur évolution
risquée et leurs tests lents.

### Décision

- Le paquet `domaine` est du Java pur : aucune annotation Spring, aucun import `jakarta`,
  `org.springframework` ou SQL.
- Les cas d'usage vivent dans `application` et orchestrent les agrégats.
- Les dépendances techniques sont inversées via des **ports sortants** déclarés côté
  domaine/application (`ProducteurRepository`, `SmsPort`, `PrixMarchePort`, `HorlogePort`,
  `IdGeneratorPort`, `ReservationsStock`…).
- Les adaptateurs (`adapters/web`, `adapters/persistence`, `adapters/integration`,
  `adapters/systeme`) implémentent ces ports.

### Conséquences

**Positives**

- Les tests métier sont rapides (61 tests, dont la majorité en quelques millisecondes) et
  n'ouvrent aucun contexte Spring.
- Changer de base, de SMS ou de source de prix ne touche ni le domaine ni les cas d'usage.
- Les frontières entre contextes sont visibles dans l'arborescence.
- Les règles sont localisées dans les agrégats, donc réutilisées quel que soit le point d'entrée
  (REST aujourd'hui, batch ou messagerie demain).

**Négatives / coûts**

- Plus de classes et d'interfaces qu'une application en couches classiques.
- Le mapping entre domaine et DTO web est explicite (une couche de traduction à maintenir).
- Les transactions et la persistance doivent être pensées dans les adaptateurs, pas dans le domaine.

### Alternatives écartées

- **Architecture en couches (contrôleur → service → repository JPA)** : plus rapide à démarrer,
  mais les entités JPA contaminent le domaine et les tests exigent une base.
- **Domain model anémique** : les règles seraient dispersées dans les services, ce qui viole E3.

---

## ADR-002 — Persistance par adaptateurs en mémoire

**Statut** : acceptée pour ce rendu.

### Contexte

Le livrable est le **cœur** de la plateforme, évalué sur le domaine, les tests et l'API. Aucune
base n'est imposée. Le choix d'une base ajouterait des dépendances, des migrations et des tests
plus lents, sans valeur démontrée pour les règles métier.

### Décision

- Chaque port de persistance possède un adaptateur en mémoire (`*RepositoryMemoire`) fondé sur
  des `ConcurrentHashMap`.
- Les collections renvoyées sont triées selon les besoins métier (date de réception pour le FIFO,
  date de commande décroissante pour l'historique, etc.).
- Les données sont perdues au redémarrage ; c'est assumé et documenté.

### Conséquences

**Positives**

- Démarrage instantané, aucune installation externe, démonstration reproductible.
- Les tests métier utilisent exactement les mêmes adaptateurs que l'application.
- La substitution par JPA/PostgreSQL ne demande que de nouveaux adaptateurs : aucun changement
  dans `domaine` ni dans `application`.

**Négatives / coûts**

- Pas de durabilité, pas de transactions, pas de requêtes complexes.
- Certaines contraintes d'unicité (téléphone, campagne) sont vérifiées en mémoire ; en SQL elles
  seraient aussi garanties par des index uniques.
- Le tri en mémoire ne passe pas à l'échelle ; un adaptateur SQL devra pousser ces tris en base.

### Alternatives écartées

- **H2 + JPA** : démonstration plus lourde, tests plus lents, aucun gain sur les règles évaluées.
- **Fichiers JSON** : durabilité partielle mais gestion de concurrence et de transactions
  plus complexe qu'un simple adaptateur mémoire.

---

## ADR-003 — Identité simulée et non-divulgation des ressources

**Statut** : acceptée.

### Contexte

L'inscription et la connexion complètes sont hors périmètre, mais US-17 exige que chaque
utilisateur n'accède qu'à ce qui le concerne, et qu'une ressource d'autrui ne révèle pas son
existence. Il faut donc une identité crédible sans infrastructure d'authentification.

### Décision

- L'identité est portée par deux en-têtes : `X-Utilisateur-Id` et `X-Role`
  (`PRODUCTEUR`, `ACHETEUR`, `AGENT`, `ADMINISTRATEUR`).
- Un `HandlerMethodArgumentResolver` transforme ces en-têtes en objet `Identite` injecté dans
  les contrôleurs ; les contrôleurs ne lisent jamais les en-têtes eux-mêmes.
- Les **autorisations vivent dans la couche application** (`Autorisations`), pas dans les
  contrôleurs : elles s'appliqueraient de la même façon à un autre point d'entrée.
- Un rôle insuffisant renvoie **403** (`ACCES_REFUSE`). Une ressource existante mais hors du
  périmètre renvoie **404** (`RESSOURCE_INTROUVABLE`) avec le même message qu'une ressource
  inexistante, pour ne pas révéler son existence.
- Les en-têtes sont documentés sur chaque opération dans le contrat OpenAPI.

### Conséquences

**Positives**

- US-17 est démontrée sans complexité d'authentification.
- La non-divulgation est testée (`ApiIntegrationTest.ne_revele_pas_la_ressource_d_un_autre_producteur`).
- Le remplacement par une authentification réelle ne touche que le résolveur d'identité.

**Négatives / coûts**

- N'importe quel client peut se déclarer agent : ce n'est pas une sécurité de production.
- Le 404 pour un refus d'accès peut surprendre les développeurs ; c'est un choix de sécurité
  assumé et documenté.

### Alternatives écartées

- **Spring Security + JWT** : hors périmètre, et la démonstration n'en a pas besoin.
- **401/403 systématiques** : révéleraient l'existence de ressources d'autrui, ce que US-17 interdit.

---

## ADR-004 — Arrondis monétaires et répartition sans perte de franc

**Statut** : acceptée.

### Contexte

US-13 impose : « la somme des paiements et de la commission est exactement égale au montant de
la commande : aucun franc ne se perd ni n'apparaît. La règle d'arrondi est à vous de définir et
de documenter. » Les montants sont en francs CFA, sans sous-unité, et les parts proportionnelles
produisent des divisions non entières. Un arrondi naïf par producteur ferait apparaître ou
disparaître des francs.

### Décision

1. **Montant de la commande** : `quantité × prix`, arrondi au franc le plus proche
   (`HALF_UP`). Avec des quantités entières, le montant est exact.
2. **Commission** : `plancher(montant × 5 %)`. La coopérative ne retient jamais un franc
   supérieur à 5 %.
3. **Répartition du net** : méthode des **plus forts restes** :
   - part de base de chaque producteur = plancher(net × kg producteur / kg total) ;
   - francs restants = net − somme des bases ;
   - distribution des francs restants, un par producteur, aux plus grands restes ;
   - égalité de restes tranchée par ordre croissant d'identifiant producteur (déterministe).
4. Un **contrôle final** lève une erreur interne si la somme des parts diffère du net : le
   système ne peut pas produire silencieusement un déséquilibre.

Le calcul est un service de domaine pur (`RepartitionPaiement`), testé sur des cas normaux,
des petits montants et des montants indivisibles.

### Conséquences

**Positives**

- Invariant comptable garanti et testé : `somme(paiements) + commission = montant`.
- Règle unique, documentée, indépendante de la base et de l'API.
- Résultat déterministe, donc rejouable et auditable.

**Négatives / coûts**

- Un producteur peut recevoir un franc de plus qu'un calcul strictement proportionnel ; l'écart
  est d'au plus un franc et la règle est explicite.
- La commission au plancher peut être inférieure de moins d'un franc à 5 % exactement.

### Alternatives écartées

- **Arrondi au plus proche par producteur** : la somme peut dépasser ou manquer le net.
- **Attribuer tout le reste au premier producteur** : simple mais injuste et non proportionnel.
- **Calcul en décimales puis arrondi global** : impossible à répartir en francs entiers sans
  la même méthode de reste.

---

## ADR-005 — Paramètres métier externalisés et instantané des paramètres de culture

**Statut** : acceptée.

### Contexte

Le sujet indique que les valeurs de référence « peuvent changer : elles ne doivent pas être
écrites partout dans votre code ». US-19 exige qu'une modification s'applique aux déclarations
futures, jamais aux anciennes. Deux catégories de valeurs coexistent : les paramètres de la
coopérative (50 ha, 50 kg, 5 %, 30 %) et les paramètres par culture (rendement, humidité, cycle).

### Décision

- Les paramètres de la coopérative sont dans `application.yml`, section `cooperative`, liés au
  record `ConfigurationCooperative` et injectés dans les services.
- Les paramètres par culture sont dans le port `ReferentielCultures`, initialisés par
  `ReferencesParDefaut`, et modifiables par un administrateur.
- Chaque `Culture` **copie** la `CultureReference` en vigueur au moment de sa déclaration
  (instantané immuable). Les contrôles de cycle, de rendement et d'humidité utilisent cette copie.
- Aucune valeur métier n'est codée en dur dans les agrégats ou les contrôleurs.

### Conséquences

**Positives**

- US-19 est satisfaite sans mécanisme de version : l'historique est porté par chaque culture.
- Les seuils sont modifiables sans recompilation (coopérative) ou via l'API (cultures).
- Un seul endroit par valeur, ce qui facilite l'audit (E3).

**Négatives / coûts**

- Un référentiel modifié ne se propage pas aux cultures existantes : c'est voulu, mais il faut
  le documenter clairement pour les agents.
- La configuration de la coopérative n'est pas modifiable à chaud ; un redémarrage est nécessaire.

### Alternatives écartées

- **Constantes statiques** : viole l'exigence de valeurs centralisées et empêche US-19.
- **Lire le référentiel à chaque contrôle** : une modification changerait rétroactivement les
  cultures, ce que US-19 interdit.
- **Versionner le référentiel avec des dates d'effet** : plus puissant mais plus complexe que
  l'instantané, pour un besoin couvert simplement.

---

## ADR-006 — Contrat OpenAPI, format d'erreur unique et versionnement

**Statut** : acceptée.

### Contexte

L'API doit être décrite par un contrat cohérent avec le comportement réel (E5), toute erreur
doit suivre un format unique sans détail interne (E6), et l'API doit évoluer sans casser les
clients (E7).

### Décision

- Le contrat est **généré depuis le code** (springdoc), puis exporté dans `docs/openapi.yaml`.
  Il n'est pas maintenu à la main : il ne peut donc pas dériver silencieusement.
- Un `OperationCustomizer` ajoute à **chaque** opération les en-têtes d'identité et les réponses
  d'erreur 400/403/404/409/500, toutes référençant le schéma `ErreurApi`.
- Toutes les erreurs applicatives passent par `GestionnaireErreurs` et produisent
  `{code, message, details, horodatage, chemin}`. Les exceptions techniques sont journalisées
  et remplacées par un message générique.
- L'API est **versionnée dans le chemin** (`/api/v1`). Les évolutions sont additives : nouveaux
  champs facultatifs, nouveaux endpoints, nouveaux codes d'erreur sans changer les existants.
  Un changement incompatible imposerait `/api/v2`.
- Un test d'intégration vérifie que `/v3/api-docs` est exposé et contient les chemins attendus,
  et qu'une erreur réelle respecte le format.

### Conséquences

**Positives**

- Le contrat est toujours à jour par construction.
- Les clients disposent d'un format d'erreur stable et de codes métier exploitables.
- Le versionnement dans l'URL est visible, simple et compatible avec les caches.

**Négatives / coûts**

- springdoc ajoute une dépendance et un peu de configuration.
- Le contrat généré est verbeux ; il est exporté pour être lisible hors runtime.
- Le versionnement d'URL impose de maintenir `/v1` lors d'une future `/v2`.

### Alternatives écartées

- **Contrat écrit à la main** : risque de divergence avec le code, contraire à E5.
- **Versionnement par en-tête** : plus flexible mais moins visible et plus difficile à tester.
- **Pas de version** : toute évolution incompatible casserait les clients, contraire à E7.
