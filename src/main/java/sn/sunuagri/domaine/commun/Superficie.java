package sn.sunuagri.domaine.commun;

import java.math.BigDecimal;

/** Superficie en hectares, strictement positive. */
public record Superficie(BigDecimal hectares) {

    public Superficie {
        Validations.strictementPositif(hectares, "La superficie");
        hectares = hectares.stripTrailingZeros();
    }

    public static Superficie de(String hectares) {
        if (hectares == null || hectares.isBlank()) {
            throw new RegleMetierException("La superficie est obligatoire");
        }
        return new Superficie(new BigDecimal(hectares.trim().replace(',', '.')));
    }

    public Superficie plus(Superficie autre) {
        return new Superficie(hectares.add(autre.hectares));
    }

    public boolean depasse(Superficie plafond) {
        return hectares.compareTo(plafond.hectares) > 0;
    }

    public BigDecimal enHectares() {
        return hectares;
    }
}
