package sn.sunuagri.domaine.commun;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montant en francs CFA (XOF), toujours entier : le franc CFA n'a pas de sous-unité.
 * Les arrondis sont explicites et documentés (voir docs/DECISIONS.md, ADR-004).
 */
public record Montant(long francs) {

    public Montant {
        if (francs < 0) {
            throw new RegleMetierException("Le montant ne peut pas être négatif");
        }
    }

    public static Montant de(long francs) {
        return new Montant(francs);
    }

    public static Montant de(String francs) {
        if (francs == null || francs.isBlank()) {
            throw new RegleMetierException("Le montant est obligatoire");
        }
        return new Montant(new BigDecimal(francs.trim()).longValueExact());
    }

    public Montant plus(Montant autre) {
        return new Montant(Math.addExact(francs, autre.francs));
    }

    public Montant moins(Montant autre) {
        if (autre.francs > francs) {
            throw new RegleMetierException("Le montant obtenu serait négatif");
        }
        return new Montant(francs - autre.francs);
    }

    /** Quantité (kg) x prix au kg, arrondi au franc le plus proche (demi vers le haut). */
    public Montant fois(Quantite quantite) {
        BigDecimal valeur = BigDecimal.valueOf(francs)
                .multiply(quantite.kg())
                .setScale(0, RoundingMode.HALF_UP);
        return new Montant(valeur.longValueExact());
    }

    /** Pourcentage arrondi à l'inférieur, utilisé pour la commission de la coopérative. */
    public Montant pourcentageArrondiInferieur(BigDecimal pourcentage) {
        BigDecimal valeur = BigDecimal.valueOf(francs)
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        return new Montant(valeur.longValueExact());
    }

    public boolean estPositif() {
        return francs > 0;
    }

    public BigDecimal enFrancs() {
        return BigDecimal.valueOf(francs);
    }
}
