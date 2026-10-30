package sn.sunuagri.domaine.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.culture.TypeCulture;

/**
 * Lot de stock reçu à l'entrepôt. Un lot est rattaché à un producteur et daté.
 * Il est consommé du plus ancien au plus récent lors d'une livraison de commande.
 */
public class Lot {

    private final String id;
    private final String producteurId;
    private final String recolteId;
    private final TypeCulture typeCulture;
    private final Qualite qualite;
    private final Quantite quantiteInitiale;
    private Quantite quantiteRestante;
    private final BigDecimal tauxHumiditePct;
    private final LocalDate dateReception;

    public Lot(String id, String producteurId, String recolteId, TypeCulture typeCulture,
               Qualite qualite, Quantite quantiteInitiale, BigDecimal tauxHumiditePct,
               LocalDate dateReception) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant du lot est obligatoire");
        }
        if (producteurId == null || producteurId.isBlank()) {
            throw new RegleMetierException("Le producteur du lot est obligatoire");
        }
        if (recolteId == null || recolteId.isBlank()) {
            throw new RegleMetierException("La récolte du lot est obligatoire");
        }
        if (typeCulture == null) {
            throw new RegleMetierException("La culture du lot est obligatoire");
        }
        if (qualite == null) {
            throw new RegleMetierException("La qualité du lot est obligatoire");
        }
        if (quantiteInitiale == null) {
            throw new RegleMetierException("La quantité du lot est obligatoire");
        }
        quantiteInitiale.exigerPositive("La quantité du lot");
        if (tauxHumiditePct == null || tauxHumiditePct.signum() < 0) {
            throw new RegleMetierException("Le taux d'humidité est obligatoire et ne peut pas être négatif");
        }
        if (dateReception == null) {
            throw new RegleMetierException("La date de réception est obligatoire");
        }
        this.id = id;
        this.producteurId = producteurId;
        this.recolteId = recolteId;
        this.typeCulture = typeCulture;
        this.qualite = qualite;
        this.quantiteInitiale = quantiteInitiale;
        this.quantiteRestante = quantiteInitiale;
        this.tauxHumiditePct = tauxHumiditePct;
        this.dateReception = dateReception;
    }

    /** Consomme une quantité du lot ; refuse de descendre sous zéro. */
    public void consommer(Quantite quantite) {
        if (quantite == null || quantite.estNulle()) {
            throw new RegleMetierException("La quantité à consommer doit être strictement positive");
        }
        if (quantite.estSuperieureA(quantiteRestante)) {
            throw new ConflitException("Le lot " + id + " ne contient pas assez de marchandise");
        }
        quantiteRestante = quantiteRestante.moins(quantite);
    }

    public String id() {
        return id;
    }

    public String producteurId() {
        return producteurId;
    }

    public String recolteId() {
        return recolteId;
    }

    public TypeCulture typeCulture() {
        return typeCulture;
    }

    public Qualite qualite() {
        return qualite;
    }

    public Quantite quantiteInitiale() {
        return quantiteInitiale;
    }

    public Quantite quantiteRestante() {
        return quantiteRestante;
    }

    public BigDecimal tauxHumiditePct() {
        return tauxHumiditePct;
    }

    public LocalDate dateReception() {
        return dateReception;
    }
}
