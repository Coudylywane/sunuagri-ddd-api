package sn.sunuagri.domaine.commun;

/** Rôle de l'utilisateur simulé (voir docs/DECISIONS.md, ADR-003). */
public enum Role {
    PRODUCTEUR,
    ACHETEUR,
    AGENT,
    ADMINISTRATEUR;

    public static Role depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new RegleMetierException("Le rôle est obligatoire");
        }
        try {
            return Role.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegleMetierException("Rôle inconnu : " + valeur);
        }
    }
}
