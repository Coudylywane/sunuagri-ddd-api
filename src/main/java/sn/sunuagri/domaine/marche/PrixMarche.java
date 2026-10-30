package sn.sunuagri.domaine.marche;

import java.time.LocalDate;
import sn.sunuagri.domaine.commun.Montant;

/** Prix de référence du marché, éventuellement issu du dernier prix connu (US-18). */
public record PrixMarche(Montant prixKg, LocalDate date, boolean issuDuCache) {

    public PrixMarche {
        if (prixKg == null || !prixKg.estPositif()) {
            throw new sn.sunuagri.domaine.commun.RegleMetierException(
                    "Le prix de référence doit être strictement positif");
        }
        if (date == null) {
            throw new sn.sunuagri.domaine.commun.RegleMetierException(
                    "La date du prix de référence est obligatoire");
        }
    }
}
