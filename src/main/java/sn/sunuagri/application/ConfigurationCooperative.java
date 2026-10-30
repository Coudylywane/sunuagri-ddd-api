package sn.sunuagri.application;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import sn.sunuagri.domaine.commun.RegleMetierException;

/**
 * Paramètres métier de la coopérative, externalisés dans application.yml.
 * Ils sont injectés dans les services, jamais recopiés dans le code métier.
 */
@ConfigurationProperties(prefix = "cooperative")
public record ConfigurationCooperative(
        BigDecimal superficieMaxProducteurHa,
        BigDecimal commandeMinKg,
        BigDecimal commissionPct,
        BigDecimal ecartPrixPct) {

    public ConfigurationCooperative {
        exiger(superficieMaxProducteurHa, "cooperative.superficie-max-producteur-ha");
        exiger(commandeMinKg, "cooperative.commande-min-kg");
        exiger(commissionPct, "cooperative.commission-pct");
        exiger(ecartPrixPct, "cooperative.ecart-prix-pct");
    }

    private static void exiger(BigDecimal valeur, String nom) {
        if (valeur == null || valeur.signum() <= 0) {
            throw new RegleMetierException("La configuration " + nom + " doit être strictement positive");
        }
    }
}
