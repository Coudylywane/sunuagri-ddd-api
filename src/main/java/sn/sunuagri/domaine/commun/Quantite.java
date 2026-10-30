package sn.sunuagri.domaine.commun;

import java.math.BigDecimal;

/**
 * Quantité en kilogrammes. La valeur zéro est autorisée en interne
 * (stock épuisé, quantité restante nulle) ; les points d'entrée métier
 * utilisent {@link #de(BigDecimal)} qui impose une quantité strictement positive.
 */
public record Quantite(BigDecimal kg) {

    public Quantite {
        if (kg == null) {
            throw new RegleMetierException("La quantité est obligatoire");
        }
        if (kg.signum() < 0) {
            throw new RegleMetierException("La quantité ne peut pas être négative");
        }
        kg = kg.stripTrailingZeros();
    }

    public static Quantite de(BigDecimal kg) {
        Quantite quantite = new Quantite(kg);
        quantite.exigerPositive("La quantité");
        return quantite;
    }

    public static Quantite de(long kg) {
        return de(BigDecimal.valueOf(kg));
    }

    public static Quantite de(String kg) {
        if (kg == null || kg.isBlank()) {
            throw new RegleMetierException("La quantité est obligatoire");
        }
        return de(new BigDecimal(kg.trim().replace(',', '.')));
    }

    public static Quantite zero() {
        return new Quantite(BigDecimal.ZERO);
    }

    public Quantite exigerPositive(String libelle) {
        if (kg.signum() <= 0) {
            throw new RegleMetierException(libelle + " doit être strictement positive");
        }
        return this;
    }

    public Quantite plus(Quantite autre) {
        return new Quantite(kg.add(autre.kg));
    }

    public Quantite moins(Quantite autre) {
        return new Quantite(kg.subtract(autre.kg));
    }

    public Quantite minimum(Quantite autre) {
        return kg.compareTo(autre.kg) <= 0 ? this : autre;
    }

    public boolean estSuperieureA(Quantite autre) {
        return kg.compareTo(autre.kg) > 0;
    }

    public boolean estNulle() {
        return kg.signum() == 0;
    }

    public BigDecimal enKg() {
        return kg;
    }
}
