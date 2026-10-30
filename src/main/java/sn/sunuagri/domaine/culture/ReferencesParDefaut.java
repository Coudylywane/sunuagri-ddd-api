package sn.sunuagri.domaine.culture;

import java.math.BigDecimal;

/** Valeurs de référence initiales, regroupées en un seul endroit. */
public final class ReferencesParDefaut {

    private ReferencesParDefaut() {
    }

    public static final CultureReference ARACHIDE =
            new CultureReference(TypeCulture.ARACHIDE, new BigDecimal("1200"), new BigDecimal("9"), 90);
    public static final CultureReference MIL =
            new CultureReference(TypeCulture.MIL, new BigDecimal("800"), new BigDecimal("12"), 75);
    public static final CultureReference RIZ =
            new CultureReference(TypeCulture.RIZ, new BigDecimal("4000"), new BigDecimal("14"), 110);
    public static final CultureReference MAIS =
            new CultureReference(TypeCulture.MAIS, new BigDecimal("2500"), new BigDecimal("14"), 90);

    public static java.util.List<CultureReference> toutes() {
        return java.util.List.of(ARACHIDE, MIL, RIZ, MAIS);
    }
}
