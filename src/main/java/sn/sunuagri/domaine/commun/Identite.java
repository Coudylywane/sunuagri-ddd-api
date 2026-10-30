package sn.sunuagri.domaine.commun;

/**
 * Identité de l'appelant. L'authentification réelle est hors périmètre :
 * l'API la simule avec les en-têtes X-Utilisateur-Id et X-Role.
 */
public record Identite(String utilisateurId, Role role) {

    public Identite {
        Validations.requis(utilisateurId, "L'identifiant utilisateur");
        if (role == null) {
            throw new RegleMetierException("Le rôle est obligatoire");
        }
    }

    public boolean estProducteur() {
        return role == Role.PRODUCTEUR;
    }

    public boolean estAcheteur() {
        return role == Role.ACHETEUR;
    }

    public boolean estAgent() {
        return role == Role.AGENT;
    }

    public boolean estAdministrateur() {
        return role == Role.ADMINISTRATEUR;
    }

    public boolean estAgentOuAdministrateur() {
        return estAgent() || estAdministrateur();
    }
}
