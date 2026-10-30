package sn.sunuagri.domaine.commercialisation;

import java.time.LocalDate;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.stock.Qualite;

/** Agrégat Commande passée par un acheteur sur une offre. */
public class Commande {

    private final String id;
    private final String offreId;
    private final String acheteurId;
    private final TypeCulture typeCulture;
    private final Qualite qualite;
    private final Quantite quantite;
    private final Montant prixKg;
    private final Montant montant;
    private final LocalDate dateCreation;
    private StatutCommande statut = StatutCommande.EN_ATTENTE;
    private LocalDate dateLivraison;

    private Commande(String id, String offreId, String acheteurId, TypeCulture typeCulture,
                     Qualite qualite, Quantite quantite, Montant prixKg, Montant montant,
                     LocalDate dateCreation) {
        this.id = id;
        this.offreId = offreId;
        this.acheteurId = acheteurId;
        this.typeCulture = typeCulture;
        this.qualite = qualite;
        this.quantite = quantite;
        this.prixKg = prixKg;
        this.montant = montant;
        this.dateCreation = dateCreation;
    }

    /**
     * US-11 : crée une commande en attente. Le montant est la quantité multipliée
     * par le prix au kg. La quantité minimale vient de la configuration.
     */
    public static Commande creer(String id, Offre offre, String acheteurId, Quantite quantite,
                                 Quantite quantiteMinimale, LocalDate dateCreation) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant de la commande est obligatoire");
        }
        if (offre == null) {
            throw new RegleMetierException("L'offre est obligatoire");
        }
        if (acheteurId == null || acheteurId.isBlank()) {
            throw new RegleMetierException("L'acheteur est obligatoire");
        }
        if (dateCreation == null) {
            throw new RegleMetierException("La date de commande est obligatoire");
        }
        if (quantite == null) {
            throw new RegleMetierException("La quantité commandée est obligatoire");
        }
        quantite.exigerPositive("La quantité commandée");
        if (quantiteMinimale == null) {
            throw new RegleMetierException("La quantité minimale de commande est obligatoire");
        }
        if (quantite.kg().compareTo(quantiteMinimale.kg()) < 0) {
            throw new RegleMetierException(
                    "La quantité minimale d'une commande est de "
                            + quantiteMinimale.kg().toPlainString() + " kg");
        }
        Montant montant = offre.prixKg().fois(quantite);
        return new Commande(id, offre.id(), acheteurId, offre.typeCulture(), offre.qualite(),
                quantite, offre.prixKg(), montant, dateCreation);
    }

    /** US-12 : annulation possible tant que la commande n'est pas livrée. */
    public void annuler() {
        if (statut == StatutCommande.LIVREE) {
            throw new ConflitException("Une commande livrée ne peut pas être annulée");
        }
        if (statut == StatutCommande.ANNULEE) {
            throw new ConflitException("La commande est déjà annulée");
        }
        statut = StatutCommande.ANNULEE;
    }

    /** US-13 : confirmation de livraison, uniquement depuis l'état en attente. */
    public void livrer(LocalDate dateLivraison) {
        if (statut == StatutCommande.LIVREE) {
            throw new ConflitException("La commande est déjà livrée");
        }
        if (statut == StatutCommande.ANNULEE) {
            throw new ConflitException("Une commande annulée ne peut pas être livrée");
        }
        if (dateLivraison == null) {
            throw new RegleMetierException("La date de livraison est obligatoire");
        }
        this.statut = StatutCommande.LIVREE;
        this.dateLivraison = dateLivraison;
    }

    public boolean estEnAttente() {
        return statut == StatutCommande.EN_ATTENTE;
    }

    public String id() {
        return id;
    }

    public String offreId() {
        return offreId;
    }

    public String acheteurId() {
        return acheteurId;
    }

    public TypeCulture typeCulture() {
        return typeCulture;
    }

    public Qualite qualite() {
        return qualite;
    }

    public Quantite quantite() {
        return quantite;
    }

    public Montant prixKg() {
        return prixKg;
    }

    public Montant montant() {
        return montant;
    }

    public LocalDate dateCreation() {
        return dateCreation;
    }

    public StatutCommande statut() {
        return statut;
    }

    public LocalDate dateLivraison() {
        return dateLivraison;
    }
}
