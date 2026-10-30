package sn.sunuagri.domaine.commun;

import java.math.BigDecimal;

/** Validations élémentaires partagées par les objets-valeurs. */
public final class Validations {

    private Validations() {
    }

    public static String requis(String valeur, String libelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new RegleMetierException(libelle + " est obligatoire");
        }
        return valeur.trim();
    }

    public static BigDecimal strictementPositif(BigDecimal valeur, String libelle) {
        if (valeur == null) {
            throw new RegleMetierException(libelle + " est obligatoire");
        }
        if (valeur.signum() <= 0) {
            throw new RegleMetierException(libelle + " doit être strictement positif");
        }
        return valeur;
    }

    public static int strictementPositif(int valeur, String libelle) {
        if (valeur <= 0) {
            throw new RegleMetierException(libelle + " doit être strictement positif");
        }
        return valeur;
    }
}
