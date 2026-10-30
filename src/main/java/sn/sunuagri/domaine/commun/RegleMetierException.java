package sn.sunuagri.domaine.commun;

/** Une règle du métier n'est pas respectée (requête compréhensible mais refusée). */
public class RegleMetierException extends DomaineException {

    public RegleMetierException(String message) {
        super("REGLE_METIER", message);
    }
}
