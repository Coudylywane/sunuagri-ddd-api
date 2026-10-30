package sn.sunuagri.domaine.commun;

import java.time.LocalDate;

/** Période inclusive, la fin doit être strictement postérieure au début. */
public record Periode(LocalDate debut, LocalDate fin) {

    public Periode {
        if (debut == null || fin == null) {
            throw new RegleMetierException("Les dates de début et de fin sont obligatoires");
        }
        if (!fin.isAfter(debut)) {
            throw new RegleMetierException("La date de fin doit être postérieure à la date de début");
        }
    }
}
