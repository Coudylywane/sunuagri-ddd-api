package sn.sunuagri.domaine.campagne;

import sn.sunuagri.domaine.commun.RegleMetierException;

public enum StatutCulture {
    PLANIFIEE,
    SEMEE,
    RECOLTEE;

    public void verifierTransitionVers(StatutCulture cible) {
        if (cible == this) {
            return;
        }
        if (cible.ordinal() != this.ordinal() + 1) {
            throw new RegleMetierException("La transition de statut doit respecter l'ordre : planifiée, semée, récoltée");
        }
    }
}
