package sn.sunuagri.domaine.commercialisation;

import java.math.BigDecimal;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.stock.Qualite;

/**
 * Agrégat Offre : quantité à vendre, prix au kg, quantité réservée par les
 * commandes en attente et quantité déjà vendue. La quantité restante est
 * toujours dérivée, jamais stockée en double.
 */
public class Offre {

    private final String id;
    private final TypeCulture typeCulture;
    private final Qualite qualite;
    private final Quantite quantite;
    private final Montant prixKg;
    private Quantite quantiteEngagee = Quantite.zero();
    private Quantite quantiteVendue = Quantite.zero();
    private StatutOffre statut = StatutOffre.OUVERTE;

    public Offre(String id, TypeCulture typeCulture, Qualite qualite, Quantite quantite, Montant prixKg) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant de l'offre est obligatoire");
        }
        if (typeCulture == null) {
            throw new RegleMetierException("La culture de l'offre est obligatoire");
        }
        if (qualite == null) {
            throw new RegleMetierException("La qualité de l'offre est obligatoire");
        }
        if (quantite == null) {
            throw new RegleMetierException("La quantité de l'offre est obligatoire");
        }
        quantite.exigerPositive("La quantité de l'offre");
        if (prixKg == null || !prixKg.estPositif()) {
            throw new RegleMetierException("Le prix au kg doit être strictement positif");
        }
        this.id = id;
        this.typeCulture = typeCulture;
        this.qualite = qualite;
        this.quantite = quantite;
        this.prixKg = prixKg;
    }

    public Quantite restante() {
        Quantite restante = quantite.moins(quantiteEngagee).moins(quantiteVendue);
        return restante.kg().signum() < 0 ? Quantite.zero() : restante;
    }

    /** Réserve une quantité pour une commande en attente. */
    public void commander(Quantite quantiteCommandee) {
        if (statut != StatutOffre.OUVERTE) {
            throw new ConflitException("L'offre n'est pas ouverte à la commande");
        }
        if (quantiteCommandee == null) {
            throw new RegleMetierException("La quantité commandée est obligatoire");
        }
        quantiteCommandee.exigerPositive("La quantité commandée");
        if (quantiteCommandee.estSuperieureA(restante())) {
            throw new RegleMetierException(
                    "La quantité commandée dépasse la quantité restante de l'offre ("
                            + restante().kg().toPlainString() + " kg)");
        }
        quantiteEngagee = quantiteEngagee.plus(quantiteCommandee);
        rafraichirStatut();
    }

    /** Annule une commande en attente : la quantité redevient disponible. */
    public void annuler(Quantite quantiteAnnulee) {
        if (quantiteAnnulee == null || quantiteAnnulee.estSuperieureA(quantiteEngagee)) {
            throw new ConflitException("La quantité à annuler n'est pas réservée sur cette offre");
        }
        quantiteEngagee = quantiteEngagee.moins(quantiteAnnulee);
        rafraichirStatut();
    }

    /** Confirme une commande : la quantité quitte la réservation et devient vendue. */
    public void livrer(Quantite quantiteLivree) {
        if (quantiteLivree == null || quantiteLivree.estSuperieureA(quantiteEngagee)) {
            throw new ConflitException("La quantité à livrer n'est pas réservée sur cette offre");
        }
        quantiteEngagee = quantiteEngagee.moins(quantiteLivree);
        quantiteVendue = quantiteVendue.plus(quantiteLivree);
        rafraichirStatut();
    }

    /** Retire l'offre et libère la quantité non vendue. */
    public void retirer() {
        if (statut == StatutOffre.RETIREE) {
            throw new ConflitException("L'offre est déjà retirée");
        }
        if (quantiteEngagee.kg().signum() > 0) {
            throw new ConflitException(
                    "Impossible de retirer une offre qui a des commandes en attente");
        }
        statut = StatutOffre.RETIREE;
    }

    private void rafraichirStatut() {
        if (statut == StatutOffre.RETIREE) {
            return;
        }
        statut = restante().estNulle() ? StatutOffre.EPUISEE : StatutOffre.OUVERTE;
    }

    public String id() {
        return id;
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

    public Quantite quantiteEngagee() {
        return quantiteEngagee;
    }

    public Quantite quantiteVendue() {
        return quantiteVendue;
    }

    public StatutOffre statut() {
        return statut;
    }

    /** Quantité encore immobilisée par cette offre : c'est la réservation de stock. */
    public Quantite quantiteReservee() {
        if (statut == StatutOffre.RETIREE) {
            return Quantite.zero();
        }
        // Toute la marchandise non encore vendue reste réservée : la part déjà
        // commandée mais pas livrée ne doit pas être proposée à un autre acheteur.
        return quantite.moins(quantiteVendue);
    }

    public BigDecimal montantTheorique() {
        return prixKg.enFrancs().multiply(quantite.kg());
    }
}
