package sn.sunuagri.adapters.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.PaiementDu;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.CultureReference;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.marche.PrixMarche;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Livraison;

/** Représentations HTTP des ressources. */
public final class Reponses {

    private Reponses() {
    }

    public record ParcelleVue(String id, String nom, String commune, BigDecimal superficieHa) {
        public static ParcelleVue de(Parcelle parcelle) {
            return new ParcelleVue(parcelle.id(), parcelle.nom(), parcelle.commune(),
                    parcelle.superficie().hectares());
        }
    }

    public record ProducteurVue(String id, String nom, String prenom, String telephone, String village,
                                LocalDate dateEnregistrement, List<ParcelleVue> parcelles) {
        public static ProducteurVue de(Producteur producteur) {
            return new ProducteurVue(producteur.id(), producteur.nom(), producteur.prenom(),
                    producteur.telephone().valeur(), producteur.village(), producteur.dateEnregistrement(),
                    producteur.parcelles().stream().map(ParcelleVue::de).toList());
        }
    }

    public record CampagneVue(String id, String saison, int annee, LocalDate dateDebut, LocalDate dateFin,
                              String statut) {
        public static CampagneVue de(Campagne campagne) {
            return new CampagneVue(campagne.id(), campagne.saison().name().toLowerCase().replace('_', '-'),
                    campagne.annee(), campagne.periode().debut(), campagne.periode().fin(),
                    campagne.estOuverte() ? "ouverte" : "cloturee");
        }
    }

    public record CultureVue(String id, String campagneId, String parcelleId, String producteurId,
                             String culture, String statut, BigDecimal superficieHa,
                             LocalDate dateSemis, LocalDate dateRecolte, BigDecimal quantiteRecolteeKg,
                             BigDecimal rendementReferenceKgHa, BigDecimal humiditeMaxPct,
                             int cycleMinJours) {
        public static CultureVue de(Culture culture) {
            CultureReference reference = culture.reference();
            return new CultureVue(culture.id(), culture.campagneId(), culture.parcelleId(),
                    culture.producteurId(), reference.type().libelle(), culture.statut().name().toLowerCase(),
                    culture.superficieEmblavee().hectares(), culture.dateSemis(), culture.dateRecolte(),
                    culture.quantiteRecoltee() == null ? null : culture.quantiteRecoltee().kg(),
                    reference.rendementReferenceKgHa(), reference.humiditeMaxPct(),
                    reference.cycleMinJours());
        }
    }

    public record RecolteVue(String id, String cultureId, String producteurId, String culture,
                             BigDecimal quantiteKg, LocalDate dateRecolte) {
        public static RecolteVue de(Recolte recolte) {
            return new RecolteVue(recolte.id(), recolte.cultureId(), recolte.producteurId(),
                    recolte.typeCulture().libelle(), recolte.quantite().kg(), recolte.dateRecolte());
        }
    }

    public record LivraisonVue(String id, String recolteId, String lotId, String producteurId,
                               String qualite, BigDecimal quantiteKg, BigDecimal tauxHumiditePct,
                               LocalDate date) {
        public static LivraisonVue de(Livraison livraison) {
            return new LivraisonVue(livraison.id(), livraison.recolteId(), livraison.lotId(),
                    livraison.producteurId(), livraison.qualite().name(), livraison.quantite().kg(),
                    livraison.tauxHumiditePct(), livraison.date());
        }
    }

    public record StockVue(String culture, String qualite, BigDecimal totalKg, BigDecimal reserveKg,
                           BigDecimal disponibleKg) {
    }

    public record OffreVue(String id, String culture, String qualite, BigDecimal quantiteKg,
                           BigDecimal quantiteRestanteKg, BigDecimal quantiteVendueKg, long prixKg,
                           String statut) {
        public static OffreVue de(Offre offre) {
            return new OffreVue(offre.id(), offre.typeCulture().libelle(), offre.qualite().name(),
                    offre.quantite().kg(), offre.restante().kg(), offre.quantiteVendue().kg(),
                    offre.prixKg().francs(), offre.statut().name().toLowerCase());
        }
    }

    public record PublicationOffreVue(OffreVue offre, String avertissementPrix) {
    }

    public record CommandeVue(String id, String offreId, String acheteurId, String culture, String qualite,
                              BigDecimal quantiteKg, long prixKg, long montantFrancs, String statut,
                              LocalDate dateCreation, LocalDate dateLivraison) {
        public static CommandeVue de(Commande commande) {
            return new CommandeVue(commande.id(), commande.offreId(), commande.acheteurId(),
                    commande.typeCulture().libelle(), commande.qualite().name(), commande.quantite().kg(),
                    commande.prixKg().francs(), commande.montant().francs(),
                    commande.statut().name().toLowerCase(), commande.dateCreation(),
                    commande.dateLivraison());
        }
    }

    public record LivraisonCommandeVue(CommandeVue commande, long commissionFrancs, long montantNetFrancs,
                                       List<PaiementVue> paiements) {
    }

    public record PaiementVue(String id, String commandeId, String producteurId, long montantFrancs,
                              String statut, String statutSms, LocalDate dateCreation) {
        public static PaiementVue de(PaiementDu paiement) {
            return new PaiementVue(paiement.id(), paiement.commandeId(), paiement.producteurId(),
                    paiement.montant().francs(), paiement.statut().name().toLowerCase(),
                    paiement.statutSms().name().toLowerCase(), paiement.dateCreation());
        }
    }

    public record PageCommandesVue(List<CommandeVue> commandes, long total, int page, int taille) {
    }

    public record ErreurImportVue(int ligne, String motif) {
    }

    public record ImportVue(int acceptes, int refuses, List<ErreurImportVue> erreurs) {
    }

    public record ParametreCultureVue(String code, String culture, BigDecimal rendementReferenceKgHa,
                                      BigDecimal humiditeMaxPct, int cycleMinJours) {
        public static ParametreCultureVue de(CultureReference reference) {
            return new ParametreCultureVue(reference.type().name(), reference.type().libelle(),
                    reference.rendementReferenceKgHa(), reference.humiditeMaxPct(),
                    reference.cycleMinJours());
        }
    }

    public record PrixMarcheVue(String culture, long prixKg, LocalDate date, boolean issuDuCache) {
        public static PrixMarcheVue de(String culture, PrixMarche prix) {
            return new PrixMarcheVue(culture, prix.prixKg().francs(), prix.date(), prix.issuDuCache());
        }
    }

    public record ErreurApiVue(String code, String message, List<String> details,
                               OffsetDateTime horodatage, String chemin) {
    }
}
