package sn.sunuagri.domaine.culture;

import java.math.BigDecimal;
import sn.sunuagri.domaine.commun.Validations;

/**
 * Paramètres de référence d'une culture. Une copie est figée dans chaque Culture
 * au moment de sa déclaration : une modification ultérieure ne s'applique donc
 * qu'aux déclarations futures (US-19).
 */
public record CultureReference(TypeCulture type, BigDecimal rendementReferenceKgHa,
                               BigDecimal humiditeMaxPct, int cycleMinJours) {

    public CultureReference {
        if (type == null) {
            throw new sn.sunuagri.domaine.commun.RegleMetierException("La culture est obligatoire");
        }
        Validations.strictementPositif(rendementReferenceKgHa, "Le rendement de référence");
        Validations.strictementPositif(humiditeMaxPct, "L'humidité maximale");
        if (humiditeMaxPct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new sn.sunuagri.domaine.commun.RegleMetierException(
                    "L'humidité maximale doit être inférieure ou égale à 100 %");
        }
        Validations.strictementPositif(cycleMinJours, "Le cycle minimal");
    }

    public boolean humiditeAcceptee(BigDecimal tauxHumiditePct) {
        return tauxHumiditePct != null
                && tauxHumiditePct.signum() >= 0
                && tauxHumiditePct.compareTo(humiditeMaxPct) <= 0;
    }
}
