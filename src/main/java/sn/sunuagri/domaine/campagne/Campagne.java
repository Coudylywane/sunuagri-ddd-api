package sn.sunuagri.domaine.campagne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import sn.sunuagri.domaine.commun.Periode;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.commun.Superficie;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.CultureReference;

/**
 * Agrégat Campagne : il contient les cultures de la saison et garantit
 * qu'aucune culture n'est ajoutée après clôture.
 */
public class Campagne {

    private final String id;
    private final Saison saison;
    private final int annee;
    private final Periode periode;
    private boolean ouverte = true;
    private final List<Culture> cultures = new ArrayList<>();

    public Campagne(String id, Saison saison, int annee, LocalDate dateDebut, LocalDate dateFin) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant de la campagne est obligatoire");
        }
        if (saison == null) {
            throw new RegleMetierException("La saison est obligatoire");
        }
        if (annee < 1900 || annee > 2100) {
            throw new RegleMetierException("L'année de campagne est invalide");
        }
        this.id = id;
        this.saison = saison;
        this.annee = annee;
        this.periode = new Periode(dateDebut, dateFin);
    }

    /**
     * US-05 : déclare une culture sur une parcelle. La somme des superficies
     * emblavées d'une même parcelle pour cette campagne ne peut dépasser la
     * superficie de la parcelle.
     */
    public Culture declarerCulture(String idCulture, String parcelleId, String producteurId,
                                   CultureReference reference, Superficie superficieEmblavee,
                                   Superficie superficieParcelle) {
        exigerOuverte();
        if (parcelleId == null || parcelleId.isBlank()) {
            throw new RegleMetierException("La parcelle est obligatoire");
        }
        if (superficieEmblavee == null) {
            throw new RegleMetierException("La superficie emblavée est obligatoire");
        }
        if (superficieParcelle == null) {
            throw new RegleMetierException("La superficie de la parcelle est obligatoire");
        }
        BigDecimal total = superficieEmblaveeSur(parcelleId).add(superficieEmblavee.hectares());
        if (total.compareTo(superficieParcelle.hectares()) > 0) {
            throw new RegleMetierException(
                    "La somme des superficies emblavées de cette parcelle dépasse sa superficie de "
                            + superficieParcelle.hectares().toPlainString() + " ha");
        }
        Culture culture = new Culture(idCulture, id, parcelleId, producteurId, reference, superficieEmblavee);
        cultures.add(culture);
        return culture;
    }

    public BigDecimal superficieEmblaveeSur(String parcelleId) {
        return cultures.stream()
                .filter(c -> c.parcelleId().equals(parcelleId))
                .map(c -> c.superficieEmblavee().hectares())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void cloturer() {
        exigerOuverte();
        this.ouverte = false;
    }

    public void exigerOuverte() {
        if (!ouverte) {
            throw new RegleMetierException(
                    "La campagne est clôturée : aucune nouvelle culture ni récolte n'est acceptée");
        }
    }

    public Optional<Culture> culture(String idCulture) {
        return cultures.stream().filter(c -> c.id().equals(idCulture)).findFirst();
    }

    public Culture cultureExistante(String idCulture) {
        return culture(idCulture)
                .orElseThrow(() -> new RessourceIntrouvableException("Culture introuvable"));
    }

    public boolean estOuverte() {
        return ouverte;
    }

    public String id() {
        return id;
    }

    public Saison saison() {
        return saison;
    }

    public int annee() {
        return annee;
    }

    public Periode periode() {
        return periode;
    }

    public List<Culture> cultures() {
        return Collections.unmodifiableList(cultures);
    }
}
