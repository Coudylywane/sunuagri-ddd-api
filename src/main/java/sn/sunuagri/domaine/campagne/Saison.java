package sn.sunuagri.domaine.campagne;

import java.time.LocalDate;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Validations;

/** Saison agricole. */
public enum Saison {
    HIVERNAGE,
    CONTRE_SAISON;

    public static Saison depuis(String valeur) {
        String v = Validations.requis(valeur, "La saison");
        String normalisee = v.toUpperCase().replace('-', '_').replace(' ', '_');
        if ("HIVERNAGE".equals(normalisee)) {
            return HIVERNAGE;
        }
        if ("CONTRE_SAISON".equals(normalisee) || "CONTRESAISON".equals(normalisee)) {
            return CONTRE_SAISON;
        }
        throw new RegleMetierException("La saison doit être « hivernage » ou « contre-saison »");
    }
}
