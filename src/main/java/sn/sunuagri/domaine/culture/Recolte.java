package sn.sunuagri.domaine.culture;

import java.time.LocalDate;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;

/** Récolte déclarée pour une culture. Agrégat dédié, référencé par les livraisons. */
public record Recolte(String id, String campagneId, String cultureId, String producteurId,
                      TypeCulture typeCulture, Quantite quantite, LocalDate dateRecolte) {

    public Recolte {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant de la récolte est obligatoire");
        }
        if (cultureId == null || cultureId.isBlank()) {
            throw new RegleMetierException("La culture est obligatoire");
        }
        if (producteurId == null || producteurId.isBlank()) {
            throw new RegleMetierException("Le producteur est obligatoire");
        }
        if (typeCulture == null) {
            throw new RegleMetierException("La culture de référence est obligatoire");
        }
        if (quantite == null) {
            throw new RegleMetierException("La quantité récoltée est obligatoire");
        }
        quantite.exigerPositive("La quantité récoltée");
        if (dateRecolte == null) {
            throw new RegleMetierException("La date de récolte est obligatoire");
        }
    }
}
