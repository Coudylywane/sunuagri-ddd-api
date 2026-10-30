package sn.sunuagri.domaine.stock;

import sn.sunuagri.domaine.commun.RegleMetierException;

/** Qualité d'un lot de stock. */
public enum Qualite {
    A,
    B,
    C;

    public static Qualite depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new RegleMetierException("La qualité est obligatoire (A, B ou C)");
        }
        try {
            return Qualite.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegleMetierException("La qualité doit être A, B ou C");
        }
    }
}
