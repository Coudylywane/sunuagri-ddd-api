package sn.sunuagri.application;

import sn.sunuagri.domaine.commun.AccesRefuseException;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;

/** Règles d'accès transverses (US-17), indépendantes du transport HTTP. */
public final class Autorisations {

    private Autorisations() {
    }

    public static void exigerAgent(Identite identite) {
        if (identite == null || !identite.estAgentOuAdministrateur()) {
            throw new AccesRefuseException("Cette opération est réservée à un agent de coopérative");
        }
    }

    public static void exigerAdministrateur(Identite identite) {
        if (identite == null || !identite.estAdministrateur()) {
            throw new AccesRefuseException("Cette opération est réservée à un administrateur");
        }
    }

    public static void exigerAcheteur(Identite identite) {
        if (identite == null || !identite.estAcheteur()) {
            throw new AccesRefuseException("Cette opération est réservée à un acheteur");
        }
    }

    /**
     * Un producteur ne peut accéder qu'à ses propres données. Un agent ou un
     * administrateur accède aux données de la coopérative. Toute autre demande
     * reçoit un 404, sans révéler l'existence de la ressource.
     */
    public static void accederProducteur(Identite identite, String producteurId) {
        if (identite == null) {
            throw new RessourceIntrouvableException("Producteur introuvable");
        }
        if (identite.estAgentOuAdministrateur()) {
            return;
        }
        if (identite.estProducteur() && identite.utilisateurId().equals(producteurId)) {
            return;
        }
        throw new RessourceIntrouvableException("Producteur introuvable");
    }
}
