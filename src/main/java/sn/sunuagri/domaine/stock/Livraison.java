package sn.sunuagri.domaine.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;

/** Livraison acceptée à l'entrepôt ; chaque livraison crée exactement un lot. */
public record Livraison(String id, String recolteId, String lotId, String producteurId,
                        Qualite qualite, Quantite quantite, BigDecimal tauxHumiditePct,
                        LocalDate date) {

    public Livraison {
        if (id == null || id.isBlank() || lotId == null || lotId.isBlank()) {
            throw new RegleMetierException("Les identifiants de livraison et de lot sont obligatoires");
        }
        if (recolteId == null || recolteId.isBlank() || producteurId == null || producteurId.isBlank()) {
            throw new RegleMetierException("La récolte et le producteur sont obligatoires");
        }
        if (qualite == null || quantite == null || date == null) {
            throw new RegleMetierException("La qualité, la quantité et la date sont obligatoires");
        }
        quantite.exigerPositive("La quantité livrée");
        if (tauxHumiditePct == null || tauxHumiditePct.signum() < 0) {
            throw new RegleMetierException("Le taux d'humidité est obligatoire");
        }
    }
}
