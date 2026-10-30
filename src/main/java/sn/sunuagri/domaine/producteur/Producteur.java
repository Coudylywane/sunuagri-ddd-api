package sn.sunuagri.domaine.producteur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Superficie;
import sn.sunuagri.domaine.commun.Telephone;
import sn.sunuagri.domaine.commun.Validations;

/**
 * Agrégat Producteur : il porte l'identité du producteur et ses parcelles.
 * Les invariants de superficie totale et d'unicité du nom des parcelles sont
 * donc garantis à l'intérieur d'une seule frontière de cohérence.
 */
public class Producteur {

    private final String id;
    private final String nom;
    private final String prenom;
    private final Telephone telephone;
    private final String village;
    private final LocalDate dateEnregistrement;
    private final List<Parcelle> parcelles = new ArrayList<>();

    public Producteur(String id, String nom, String prenom, Telephone telephone,
                      String village, LocalDate dateEnregistrement) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant du producteur est obligatoire");
        }
        if (telephone == null) {
            throw new RegleMetierException("Le téléphone est obligatoire");
        }
        if (dateEnregistrement == null) {
            throw new RegleMetierException("La date d'enregistrement est obligatoire");
        }
        this.id = id;
        this.nom = Validations.requis(nom, "Le nom");
        this.prenom = Validations.requis(prenom, "Le prénom");
        this.telephone = telephone;
        this.village = Validations.requis(village, "Le village");
        this.dateEnregistrement = dateEnregistrement;
    }

    public Parcelle declarerParcelle(String idParcelle, String nom, Superficie superficie,
                                     String commune, Superficie superficieMaximale) {
        if (superficie == null) {
            throw new RegleMetierException("La superficie de la parcelle est obligatoire");
        }
        if (superficieMaximale == null) {
            throw new RegleMetierException("La superficie maximale autorisée est obligatoire");
        }
        if (parcelles.stream().anyMatch(p -> p.memeNomQue(nom))) {
            throw new RegleMetierException(
                    "Le producteur possède déjà une parcelle nommée « " + (nom == null ? "" : nom.trim()) + " »");
        }
        BigDecimal nouveauTotal = totalSuperficieEnHectares().add(superficie.hectares());
        if (nouveauTotal.compareTo(superficieMaximale.hectares()) > 0) {
            throw new RegleMetierException(
                    "Le total des superficies d'un producteur ne peut pas dépasser "
                            + superficieMaximale.enHectares().toPlainString() + " ha");
        }
        Parcelle parcelle = new Parcelle(idParcelle, nom, superficie, commune);
        parcelles.add(parcelle);
        return parcelle;
    }

    public BigDecimal totalSuperficieEnHectares() {
        return parcelles.stream()
                .map(p -> p.superficie().hectares())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Optional<Parcelle> parcelle(String idParcelle) {
        return parcelles.stream().filter(p -> p.id().equals(idParcelle)).findFirst();
    }

    public String id() {
        return id;
    }

    public String nom() {
        return nom;
    }

    public String prenom() {
        return prenom;
    }

    public String nomComplet() {
        return prenom + " " + nom;
    }

    public Telephone telephone() {
        return telephone;
    }

    public String village() {
        return village;
    }

    public LocalDate dateEnregistrement() {
        return dateEnregistrement;
    }

    public List<Parcelle> parcelles() {
        return Collections.unmodifiableList(parcelles);
    }
}
