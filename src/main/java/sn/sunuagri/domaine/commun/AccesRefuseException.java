package sn.sunuagri.domaine.commun;

/** L'utilisateur est authentifié mais son rôle ne permet pas l'opération. */
public class AccesRefuseException extends DomaineException {

    public AccesRefuseException(String message) {
        super("ACCES_REFUSE", message);
    }
}
