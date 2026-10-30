package sn.sunuagri.domaine.marche;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import sn.sunuagri.domaine.commun.Montant;

/** US-18 : un écart de prix trop important produit un avertissement, jamais un refus. */
public final class AnalysePrix {

    private AnalysePrix() {
    }

    public static Optional<String> avertissementSiEcart(Montant prixOffre, PrixMarche reference,
                                                       BigDecimal ecartMaxPct) {
        if (prixOffre == null || reference == null || ecartMaxPct == null) {
            return Optional.empty();
        }
        BigDecimal ecart = BigDecimal.valueOf(Math.abs(prixOffre.francs() - reference.prixKg().francs()))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(reference.prixKg().francs()), 2, RoundingMode.HALF_UP);
        if (ecart.compareTo(ecartMaxPct) > 0) {
            String cache = reference.issuDuCache()
                    ? " (dernier prix connu du " + reference.date() + ")"
                    : " (prix du " + reference.date() + ")";
            return Optional.of("Le prix de " + prixOffre.francs()
                    + " FCFA/kg s'écarte de " + ecart.toPlainString()
                    + " % du prix de référence de " + reference.prixKg().francs()
                    + " FCFA/kg" + cache + " ; l'offre est publiée avec cet avertissement");
        }
        return Optional.empty();
    }
}
