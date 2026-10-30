package sn.sunuagri.domaine.commun;

/** La demande est valide mais entre en conflit avec l'état actuel (doublon, transition interdite...). */
public class ConflitException extends DomaineException {

    public ConflitException(String message) {
        super("CONFLIT", message);
    }
}
