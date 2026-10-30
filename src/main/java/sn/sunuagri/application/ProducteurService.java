package sn.sunuagri.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import sn.sunuagri.application.port.HorlogePort;
import sn.sunuagri.application.port.IdGeneratorPort;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.DomaineException;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.commun.Superficie;
import sn.sunuagri.domaine.commun.Telephone;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.producteur.ProducteurRepository;

/** Cas d'usage des producteurs et de leurs parcelles (US-01, US-02, US-03, US-15). */
@Service
public class ProducteurService {

    private final ProducteurRepository producteurs;
    private final ConfigurationCooperative configuration;
    private final IdGeneratorPort ids;
    private final HorlogePort horloge;

    public ProducteurService(ProducteurRepository producteurs,
                             ConfigurationCooperative configuration,
                             IdGeneratorPort ids,
                             HorlogePort horloge) {
        this.producteurs = producteurs;
        this.configuration = configuration;
        this.ids = ids;
        this.horloge = horloge;
    }

    public record EnregistrerProducteur(String nom, String prenom, String telephone, String village) {
    }

    public record DeclarerParcelle(String nom, BigDecimal superficieHa, String commune) {
    }

    public record ErreurImport(int ligne, String motif) {
    }

    public record ResultatImport(int acceptes, int refuses, List<ErreurImport> erreurs) {
    }

    public Producteur enregistrer(Identite identite, EnregistrerProducteur commande) {
        Autorisations.exigerAgent(identite);
        if (commande == null) {
            throw new RessourceIntrouvableException("Demande d'enregistrement absente");
        }
        Telephone telephone = new Telephone(commande.telephone());
        producteurs.parTelephone(telephone).ifPresent(existant -> {
            throw new ConflitException(
                    "Un producteur est déjà enregistré avec le téléphone " + telephone.valeur());
        });
        Producteur producteur = new Producteur(ids.nouvelId(), commande.nom(), commande.prenom(),
                telephone, commande.village(), horloge.aujourdHui());
        producteurs.sauvegarder(producteur);
        return producteur;
    }

    public Parcelle declarerParcelle(Identite identite, String producteurId,
                                     DeclarerParcelle commande) {
        Autorisations.exigerAgent(identite);
        Producteur producteur = producteur(producteurId);
        Parcelle parcelle = producteur.declarerParcelle(
                ids.nouvelId(),
                commande.nom(),
                new Superficie(commande.superficieHa()),
                commande.commune(),
                new Superficie(configuration.superficieMaxProducteurHa()));
        producteurs.sauvegarder(producteur);
        return parcelle;
    }

    public List<Parcelle> parcelles(Identite identite, String producteurId) {
        Autorisations.accederProducteur(identite, producteurId);
        return producteur(producteurId).parcelles();
    }

    public Parcelle parcelle(Identite identite, String producteurId, String parcelleId) {
        Autorisations.accederProducteur(identite, producteurId);
        return producteur(producteurId).parcelle(parcelleId)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcelle introuvable"));
    }

    public List<Producteur> tous(Identite identite) {
        Autorisations.exigerAgent(identite);
        return producteurs.tous();
    }

    /**
     * US-15 : import CSV au format « nom;prenom;telephone;village ».
     * Les lignes valides sont enregistrées, les autres sont rapportées avec
     * leur numéro de ligne et leur motif. Une ligne d'en-tête est ignorée.
     */
    public ResultatImport importerCsv(Identite identite, String contenu) {
        Autorisations.exigerAgent(identite);
        if (contenu == null || contenu.isBlank()) {
            throw new sn.sunuagri.domaine.commun.RegleMetierException("Le fichier CSV est vide");
        }
        String normalise = contenu.startsWith("\uFEFF") ? contenu.substring(1) : contenu;
        String[] lignes = normalise.split("\\R");
        int acceptes = 0;
        List<ErreurImport> erreurs = new ArrayList<>();
        boolean entetePossible = true;

        for (int index = 0; index < lignes.length; index++) {
            int numeroLigne = index + 1;
            String ligne = lignes[index].trim();
            if (ligne.isEmpty()) {
                continue;
            }
            if (entetePossible && ligne.toLowerCase().startsWith("nom;")) {
                entetePossible = false;
                continue;
            }
            entetePossible = false;
            String[] champs = ligne.split(";", -1);
            if (champs.length != 4) {
                erreurs.add(new ErreurImport(numeroLigne,
                        "Format attendu : nom;prenom;telephone;village (4 champs)"));
                continue;
            }
            try {
                enregistrer(identite, new EnregistrerProducteur(
                        champs[0].trim(), champs[1].trim(), champs[2].trim(), champs[3].trim()));
                acceptes++;
            } catch (DomaineException e) {
                erreurs.add(new ErreurImport(numeroLigne, e.getMessage()));
            }
        }
        return new ResultatImport(acceptes, erreurs.size(), erreurs);
    }

    private Producteur producteur(String producteurId) {
        return producteurs.parId(producteurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Producteur introuvable"));
    }
}
