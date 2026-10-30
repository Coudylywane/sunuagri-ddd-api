package sn.sunuagri.domaine.producteur;

import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Superficie;

/** Parcelle d'un producteur. Entité interne de l'agrégat Producteur. */
public record Parcelle(String id, String nom, Superficie superficie, String commune) {

    public Parcelle {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant de la parcelle est obligatoire");
        }
        if (nom == null || nom.isBlank()) {
            throw new RegleMetierException("Le nom de la parcelle est obligatoire");
        }
        if (superficie == null) {
            throw new RegleMetierException("La superficie de la parcelle est obligatoire");
        }
        if (commune == null || commune.isBlank()) {
            throw new RegleMetierException("La commune de la parcelle est obligatoire");
        }
        nom = nom.trim();
        commune = commune.trim();
    }

    public boolean memeNomQue(String autreNom) {
        return nom.equalsIgnoreCase(autreNom == null ? "" : autreNom.trim());
    }
}
