package sn.sunuagri.domaine.commun;

/**
 * La ressource demandée n'existe pas, ou n'existe pas pour l'utilisateur courant.
 * Le même code est utilisé dans les deux cas pour ne pas révéler l'existence
 * d'une ressource appartenant à quelqu'un d'autre.
 */
public class RessourceIntrouvableException extends DomaineException {

    public RessourceIntrouvableException(String message) {
        super("RESSOURCE_INTROUVABLE", message);
    }
}
