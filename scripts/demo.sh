#!/usr/bin/env bash
#
# Démonstration complète SunuAgri : de l'enregistrement d'un producteur
# jusqu'au paiement dû, en passant par la livraison à l'entrepôt et la vente.
#
# Prérequis : l'application tourne sur http://localhost:8080 (mvn spring-boot:run)
#             et jq est installé.
#
# Usage : ./scripts/demo.sh [url-de-base]
#
set -euo pipefail

BASE="${1:-http://localhost:8080}"
AGENT_ID="agent-1"
ACHETEUR_ID="acheteur-1"
CAMPAGNE_2026="2026"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq est requis pour cette démonstration." >&2
  exit 1
fi

titre() {
  printf '\n\033[1;34m== %s\033[0m\n' "$1"
}

agent() {
  curl -sS -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" "$@"
}

acheteur() {
  curl -sS -H "X-Utilisateur-Id: ${ACHETEUR_ID}" -H "X-Role: ACHETEUR" "$@"
}

producteur() {
  curl -sS -H "X-Utilisateur-Id: ${PRODUCTEUR_ID}" -H "X-Role: PRODUCTEUR" "$@"
}

json() {
  curl -sS -H "Content-Type: application/json" "$@"
}

# Extrait un identifiant de la réponse ; arrête la démonstration avec le message
# d'erreur de l'API si la ressource n'a pas été créée.
id_de() {
  local reponse="$1" chemin="${2:-.id}" identifiant
  identifiant=$(echo "$reponse" | jq -r "${chemin} // empty")
  if [ -z "$identifiant" ]; then
    echo "Échec de l'étape : l'API a répondu :" >&2
    echo "$reponse" | jq . >&2
    exit 1
  fi
  echo "$identifiant"
}

echo "SunuAgri — démonstration complète sur ${BASE}"

# ---------------------------------------------------------------------------
titre "US-01 · Enregistrer un producteur"
PRODUCTEUR=$(json -X POST "${BASE}/api/v1/producteurs" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"nom":"Diop","prenom":"Awa","telephone":"77 123 45 67","village":"Thiès"}')
PRODUCTEUR_ID=$(id_de "$PRODUCTEUR")
echo "$PRODUCTEUR" | jq '{id, nom, prenom, telephone, village}'

# ---------------------------------------------------------------------------
titre "US-02 · Déclarer une parcelle de 10 ha"
PARCELLE=$(json -X POST "${BASE}/api/v1/producteurs/${PRODUCTEUR_ID}/parcelles" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"nom":"Nord","superficieHa":10,"commune":"Thiès"}')
PARCELLE_ID=$(id_de "$PARCELLE")
echo "$PARCELLE" | jq .

# ---------------------------------------------------------------------------
titre "US-03 · Consulter les parcelles du producteur"
agent "${BASE}/api/v1/producteurs/${PRODUCTEUR_ID}/parcelles" | jq .

# ---------------------------------------------------------------------------
titre "US-04 · Ouvrir la campagne hivernage 2026"
CAMPAGNE=$(json -X POST "${BASE}/api/v1/campagnes" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"saison":"hivernage","annee":2026,"dateDebut":"2026-06-01","dateFin":"2026-11-30"}')
CAMPAGNE_ID=$(id_de "$CAMPAGNE")
echo "$CAMPAGNE" | jq .

# ---------------------------------------------------------------------------
titre "US-05 · Déclarer 10 ha d'arachide sur la parcelle"
CULTURE=$(json -X POST "${BASE}/api/v1/cultures" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d "{\"campagneId\":\"${CAMPAGNE_ID}\",\"parcelleId\":\"${PARCELLE_ID}\",\"culture\":\"Arachide\",\"superficieHa\":10}")
CULTURE_ID=$(id_de "$CULTURE")
echo "$CULTURE" | jq '{id, culture, statut, superficieHa, rendementReferenceKgHa, humiditeMaxPct, cycleMinJours}'

# ---------------------------------------------------------------------------
titre "US-06 · Semer le 5 juin, puis récolter le 5 octobre (cycle ≥ 90 jours)"
json -X POST "${BASE}/api/v1/cultures/${CULTURE_ID}/semis" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"dateSemis":"2026-06-05"}' | jq '{statut, dateSemis}'

# ---------------------------------------------------------------------------
titre "US-07 · Déclarer 11 000 kg récoltés"
RECOLTE=$(json -X POST "${BASE}/api/v1/cultures/${CULTURE_ID}/recolte" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"dateRecolte":"2026-10-05","quantiteKg":11000}')
RECOLTE_ID=$(id_de "$RECOLTE")
echo "$RECOLTE" | jq '{id, culture, quantiteKg, dateRecolte}'

# ---------------------------------------------------------------------------
titre "US-08 · Livrer 8 000 kg qualité A (humidité 8 %), puis 2 000 kg"
json -X POST "${BASE}/api/v1/recoltes/${RECOLTE_ID}/livraisons" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"quantiteKg":8000,"tauxHumiditePct":8,"qualite":"A"}' \
  | jq '{id, lotId, qualite, quantiteKg, tauxHumiditePct, date}'
json -X POST "${BASE}/api/v1/recoltes/${RECOLTE_ID}/livraisons" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"quantiteKg":2000,"tauxHumiditePct":8,"qualite":"A"}' \
  | jq '{lotId, quantiteKg}'

# ---------------------------------------------------------------------------
titre "US-09 · Consulter le stock (total / réservé / disponible)"
agent "${BASE}/api/v1/stock" | jq '.[] | select(.culture=="Arachide" and .qualite=="A")'

# ---------------------------------------------------------------------------
titre "US-18 · Prix de référence du marché de l'arachide"
agent "${BASE}/api/v1/prix-marche/Arachide" | jq .

# ---------------------------------------------------------------------------
titre "US-10 · Publier une offre de 6 000 kg à 350 FCFA/kg"
OFFRE=$(json -X POST "${BASE}/api/v1/offres" \
  -H "X-Utilisateur-Id: ${AGENT_ID}" -H "X-Role: AGENT" \
  -d '{"culture":"Arachide","qualite":"A","quantiteKg":6000,"prixKg":350}')
OFFRE_ID=$(id_de "$OFFRE" ".offre.id")
echo "$OFFRE" | jq .

# ---------------------------------------------------------------------------
titre "US-09 (suite) · Le stock réservé reflète l'offre publiée"
agent "${BASE}/api/v1/stock" | jq '.[] | select(.culture=="Arachide" and .qualite=="A")'

# ---------------------------------------------------------------------------
titre "US-11 · Un acheteur commande 3 000 kg"
COMMANDE=$(acheteur -X POST "${BASE}/api/v1/commandes" \
  -H "Content-Type: application/json" \
  -d "{\"offreId\":\"${OFFRE_ID}\",\"quantiteKg\":3000}")
COMMANDE_ID=$(id_de "$COMMANDE")
echo "$COMMANDE" | jq '{id, quantiteKg, prixKg, montantFrancs, statut}'

# ---------------------------------------------------------------------------
titre "US-16 · Historique paginé des commandes de l'acheteur"
acheteur "${BASE}/api/v1/commandes?page=0&taille=10" | jq .

# ---------------------------------------------------------------------------
titre "US-13 · Livrer la commande : commission, répartition, paiements dus"
LIVRAISON=$(agent -X POST "${BASE}/api/v1/commandes/${COMMANDE_ID}/livraison")
echo "$LIVRAISON" | jq '{commande: .commande.id, statut: .commande.statut,
  montantFrancs: .commande.montantFrancs, commissionFrancs, montantNetFrancs,
  paiements: [.paiements[] | {id, producteurId, montantFrancs, statut, statutSms}]}'

# ---------------------------------------------------------------------------
titre "US-14 · Le producteur consulte ses paiements (SMS déjà envoyé)"
producteur "${BASE}/api/v1/producteurs/${PRODUCTEUR_ID}/paiements" \
  | jq '.[] | {montantFrancs, statut, statutSms, commandeId}'

# ---------------------------------------------------------------------------
titre "US-09 (fin) · Stock après la vente"
agent "${BASE}/api/v1/stock" | jq '.[] | select(.culture=="Arachide" and .qualite=="A")'

# ---------------------------------------------------------------------------
titre "US-17 · Un autre producteur ne voit pas ces données (404, sans révéler l'existence)"
curl -sS -o /tmp/sunuagri-403.json -w "HTTP %{http_code}\n" \
  -H "X-Utilisateur-Id: un-autre-producteur" -H "X-Role: PRODUCTEUR" \
  "${BASE}/api/v1/producteurs/${PRODUCTEUR_ID}/paiements"
cat /tmp/sunuagri-403.json | jq .

# ---------------------------------------------------------------------------
titre "US-15 · Import CSV : lignes valides acceptées, lignes invalides rapportées"
agent -X POST "${BASE}/api/v1/producteurs/import" \
  -H "Content-Type: text/csv" \
  --data-binary $'nom;prenom;telephone;village\nFall;Moussa;772222222;Kaolack\nNdiaye;;773333333;Dakar\nSarr;Fatou;774444444;Fatick\n' \
  | jq .

# ---------------------------------------------------------------------------
titre "US-19 · Un administrateur modifie le rendement de référence du mil"
json -X PUT "${BASE}/api/v1/parametres/cultures/Mil" \
  -H "X-Utilisateur-Id: admin-1" -H "X-Role: ADMINISTRATEUR" \
  -d '{"rendementReferenceKgHa":850,"humiditeMaxPct":12,"cycleMinJours":75}' | jq .

titre "Fin de la démonstration"
echo "Producteur ${PRODUCTEUR_ID} — commande ${COMMANDE_ID} livrée, paiement dû enregistré."
