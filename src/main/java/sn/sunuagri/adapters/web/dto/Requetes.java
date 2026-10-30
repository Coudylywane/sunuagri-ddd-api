package sn.sunuagri.adapters.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corps des requêtes HTTP. */
public final class Requetes {

    private Requetes() {
    }

    public record EnregistrerProducteur(String nom, String prenom, String telephone, String village) {
    }

    public record DeclarerParcelle(String nom, BigDecimal superficieHa, String commune) {
    }

    public record OuvrirCampagne(String saison, int annee, LocalDate dateDebut, LocalDate dateFin) {
    }

    public record DeclarerCulture(String campagneId, String parcelleId, String culture,
                                  BigDecimal superficieHa) {
    }

    public record Semis(LocalDate dateSemis) {
    }

    public record Recolte(LocalDate dateRecolte, BigDecimal quantiteKg) {
    }

    public record Livraison(BigDecimal quantiteKg, BigDecimal tauxHumiditePct, String qualite) {
    }

    public record PublierOffre(String culture, String qualite, BigDecimal quantiteKg, Long prixKg) {
    }

    public record PasserCommande(String offreId, BigDecimal quantiteKg) {
    }

    public record ModifierParametre(BigDecimal rendementReferenceKgHa, BigDecimal humiditeMaxPct,
                                    Integer cycleMinJours) {
    }
}
