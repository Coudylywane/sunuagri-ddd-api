package sn.sunuagri.domaine.commun;

/**
 * Racine de toutes les erreurs métier. Le code est stable et destiné à l'API,
 * le message est lisible par un humain et ne contient aucun détail technique.
 */
public class DomaineException extends RuntimeException {

    private final String code;

    public DomaineException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
