package sn.sunuagri.domaine.culture;

import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Validations;

/** Culture de référence de la coopérative. */
public enum TypeCulture {
    ARACHIDE("Arachide"),
    MIL("Mil"),
    RIZ("Riz"),
    MAIS("Maïs");

    private final String libelle;

    TypeCulture(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    public static TypeCulture depuis(String valeur) {
        String v = Validations.requis(valeur, "La culture");
        for (TypeCulture type : values()) {
            if (type.libelle.equalsIgnoreCase(v)
                    || type.name().equalsIgnoreCase(v.replace(' ', '_').replace('ï', 'I'))) {
                return type;
            }
        }
        throw new RegleMetierException(
                "La culture doit faire partie de la liste de référence : Arachide, Mil, Riz, Maïs");
    }
}
