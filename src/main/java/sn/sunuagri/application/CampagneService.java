package sn.sunuagri.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import sn.sunuagri.application.port.IdGeneratorPort;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.CampagneRepository;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.commun.Superficie;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.CultureReference;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.RecolteRepository;
import sn.sunuagri.domaine.culture.ReferentielCultures;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.producteur.ProducteurRepository;

/** Cas d'usage des campagnes, cultures et récoltes (US-04 à US-07). */
@Service
public class CampagneService {

    private final CampagneRepository campagnes;
    private final RecolteRepository recoltes;
    private final ReferentielCultures referentiel;
    private final ProducteurRepository producteurs;
    private final IdGeneratorPort ids;

    public CampagneService(CampagneRepository campagnes, RecolteRepository recoltes,
                           ReferentielCultures referentiel, ProducteurRepository producteurs,
                           IdGeneratorPort ids) {
        this.campagnes = campagnes;
        this.recoltes = recoltes;
        this.referentiel = referentiel;
        this.producteurs = producteurs;
        this.ids = ids;
    }

    public record OuvrirCampagne(String saison, int annee, LocalDate dateDebut, LocalDate dateFin) {
    }

    public record DeclarerCulture(String campagneId, String parcelleId, String culture,
                                  BigDecimal superficieHa) {
    }

    public record Recolter(LocalDate dateRecolte, BigDecimal quantiteKg) {
    }

    public Campagne ouvrir(Identite identite, OuvrirCampagne commande) {
        Autorisations.exigerAgent(identite);
        Saison saison = Saison.depuis(commande.saison());
        campagnes.parSaisonEtAnnee(saison, commande.annee()).ifPresent(c -> {
            throw new ConflitException(
                    "Une campagne " + saison.name().toLowerCase() + " existe déjà pour l'année "
                            + commande.annee());
        });
        Campagne campagne = new Campagne(ids.nouvelId(), saison, commande.annee(),
                commande.dateDebut(), commande.dateFin());
        campagnes.sauvegarder(campagne);
        return campagne;
    }

    public Campagne cloturer(Identite identite, String campagneId) {
        Autorisations.exigerAgent(identite);
        Campagne campagne = campagne(campagneId);
        campagne.cloturer();
        campagnes.sauvegarder(campagne);
        return campagne;
    }

    public Culture declarerCulture(Identite identite, DeclarerCulture commande) {
        Autorisations.exigerAgent(identite);
        Campagne campagne = campagne(commande.campagneId());
        Producteur producteur = producteurs.parParcelleId(commande.parcelleId())
                .orElseThrow(() -> new RessourceIntrouvableException("Parcelle introuvable"));
        Parcelle parcelle = producteur.parcelle(commande.parcelleId())
                .orElseThrow(() -> new RessourceIntrouvableException("Parcelle introuvable"));
        TypeCulture type = TypeCulture.depuis(commande.culture());
        CultureReference reference = referentiel.parType(type)
                .orElseThrow(() -> new RessourceIntrouvableException("Culture de référence introuvable"));
        Culture culture = campagne.declarerCulture(ids.nouvelId(), parcelle.id(), producteur.id(),
                reference, new Superficie(commande.superficieHa()), parcelle.superficie());
        campagnes.sauvegarder(campagne);
        return culture;
    }

    public Culture semer(Identite identite, String cultureId, LocalDate dateSemis) {
        Autorisations.exigerAgent(identite);
        Campagne campagne = campagneDeCulture(cultureId);
        Culture culture = campagne.cultureExistante(cultureId);
        campagne.exigerOuverte();
        culture.semer(dateSemis);
        campagnes.sauvegarder(campagne);
        return culture;
    }

    public Recolte recolter(Identite identite, String cultureId, Recolter commande) {
        Autorisations.exigerAgent(identite);
        Campagne campagne = campagneDeCulture(cultureId);
        Culture culture = campagne.cultureExistante(cultureId);
        campagne.exigerOuverte();
        Quantite quantite = Quantite.de(commande.quantiteKg());
        culture.recolter(commande.dateRecolte(), quantite);
        Recolte recolte = new Recolte(ids.nouvelId(), campagne.id(), culture.id(),
                culture.producteurId(), culture.typeCulture(), quantite, commande.dateRecolte());
        culture.associerRecolte(recolte.id());
        campagnes.sauvegarder(campagne);
        recoltes.sauvegarder(recolte);
        return recolte;
    }

    public List<Culture> culturesDuProducteur(Identite identite, String producteurId) {
        Autorisations.accederProducteur(identite, producteurId);
        producteurs.parId(producteurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Producteur introuvable"));
        List<Culture> resultat = new ArrayList<>();
        for (Campagne campagne : campagnes.toutes()) {
            campagne.cultures().stream()
                    .filter(c -> c.producteurId().equals(producteurId))
                    .forEach(resultat::add);
        }
        resultat.sort(Comparator.comparing(c -> c.reference().type().name()));
        return resultat;
    }

    public List<Culture> culturesDeLaCampagne(Identite identite, String campagneId) {
        Autorisations.exigerAgent(identite);
        return campagne(campagneId).cultures();
    }

    public Recolte recolte(Identite identite, String recolteId) {
        Recolte recolte = recoltes.parId(recolteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Récolte introuvable"));
        Autorisations.accederProducteur(identite, recolte.producteurId());
        return recolte;
    }

    public List<Recolte> recoltesDuProducteur(Identite identite, String producteurId) {
        Autorisations.accederProducteur(identite, producteurId);
        return recoltes.parProducteur(producteurId);
    }

    public List<Campagne> toutes(Identite identite) {
        Autorisations.exigerAgent(identite);
        return campagnes.toutes();
    }

    private Campagne campagne(String campagneId) {
        return campagnes.parId(campagneId)
                .orElseThrow(() -> new RessourceIntrouvableException("Campagne introuvable"));
    }

    private Campagne campagneDeCulture(String cultureId) {
        return campagnes.parCultureId(cultureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Culture introuvable"));
    }
}
