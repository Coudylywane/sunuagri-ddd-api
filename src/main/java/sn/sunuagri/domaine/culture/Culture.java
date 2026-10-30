package sn.sunuagri.domaine.culture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import sn.sunuagri.domaine.campagne.StatutCulture;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Superficie;

/**
 * Culture déclarée sur une parcelle pour une campagne.
 * Entité interne de l'agrégat Campagne ; elle porte une copie figée des
 * paramètres de référence (US-19).
 */
public class Culture {

    private final String id;
    private final String campagneId;
    private final String parcelleId;
    private final String producteurId;
    private final CultureReference reference;
    private final Superficie superficieEmblavee;
    private StatutCulture statut;
    private LocalDate dateSemis;
    private LocalDate dateRecolte;
    private Quantite quantiteRecoltee;
    private String recolteId;

    public Culture(String id, String campagneId, String parcelleId, String producteurId,
                   CultureReference reference, Superficie superficieEmblavee) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant de la culture est obligatoire");
        }
        if (campagneId == null || campagneId.isBlank()) {
            throw new RegleMetierException("La campagne est obligatoire");
        }
        if (parcelleId == null || parcelleId.isBlank()) {
            throw new RegleMetierException("La parcelle est obligatoire");
        }
        if (producteurId == null || producteurId.isBlank()) {
            throw new RegleMetierException("Le producteur est obligatoire");
        }
        if (reference == null) {
            throw new RegleMetierException("La culture de référence est obligatoire");
        }
        if (superficieEmblavee == null) {
            throw new RegleMetierException("La superficie emblavée est obligatoire");
        }
        this.id = id;
        this.campagneId = campagneId;
        this.parcelleId = parcelleId;
        this.producteurId = producteurId;
        this.reference = reference;
        this.superficieEmblavee = superficieEmblavee;
        this.statut = StatutCulture.PLANIFIEE;
    }

    /** US-06 : passage de « planifiée » à « semée ». */
    public void semer(LocalDate dateSemis) {
        if (dateSemis == null) {
            throw new RegleMetierException("La date de semis est obligatoire");
        }
        if (statut != StatutCulture.PLANIFIEE) {
            throw new ConflitException("Seule une culture planifiée peut être semée");
        }
        statut.verifierTransitionVers(StatutCulture.SEMEE);
        this.statut = StatutCulture.SEMEE;
        this.dateSemis = dateSemis;
    }

    /**
     * US-06 + US-07 : enregistre la récolte (date et quantité) en une seule opération.
     * La date respecte le cycle minimal, la quantité ne dépasse pas deux fois le
     * rendement de référence, et une culture ne peut être récoltée qu'une fois.
     */
    public void recolter(LocalDate dateRecolte, Quantite quantite) {
        if (statut != StatutCulture.SEMEE) {
            throw new ConflitException("La culture doit être semée pour être récoltée");
        }
        if (dateRecolte == null) {
            throw new RegleMetierException("La date de récolte est obligatoire");
        }
        if (quantite == null) {
            throw new RegleMetierException("La quantité récoltée est obligatoire");
        }
        quantite.exigerPositive("La quantité récoltée");
        LocalDate dateMinimale = dateSemis.plusDays(reference.cycleMinJours());
        if (dateRecolte.isBefore(dateMinimale)) {
            throw new RegleMetierException(
                    "La date de récolte doit être postérieure d'au moins " + reference.cycleMinJours()
                            + " jours au semis (à partir du " + dateMinimale + ")");
        }
        BigDecimal rendementObtenu = quantite.kg()
                .divide(superficieEmblavee.hectares(), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal rendementSuspect = reference.rendementReferenceKgHa().multiply(BigDecimal.valueOf(2));
        if (rendementObtenu.compareTo(rendementSuspect) > 0) {
            throw new RegleMetierException(
                    "Récolte refusée comme suspecte : le rendement dépasse deux fois le rendement de référence");
        }
        statut.verifierTransitionVers(StatutCulture.RECOLTEE);
        this.statut = StatutCulture.RECOLTEE;
        this.dateRecolte = dateRecolte;
        this.quantiteRecoltee = quantite;
    }

    public void associerRecolte(String recolteId) {
        this.recolteId = Objects.requireNonNull(recolteId);
    }

    public String id() {
        return id;
    }

    public String campagneId() {
        return campagneId;
    }

    public String parcelleId() {
        return parcelleId;
    }

    public String producteurId() {
        return producteurId;
    }

    public CultureReference reference() {
        return reference;
    }

    public TypeCulture typeCulture() {
        return reference.type();
    }

    public Superficie superficieEmblavee() {
        return superficieEmblavee;
    }

    public StatutCulture statut() {
        return statut;
    }

    public LocalDate dateSemis() {
        return dateSemis;
    }

    public LocalDate dateRecolte() {
        return dateRecolte;
    }

    public Quantite quantiteRecoltee() {
        return quantiteRecoltee;
    }

    public String recolteId() {
        return recolteId;
    }
}
