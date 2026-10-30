package sn.sunuagri.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import sn.sunuagri.adapters.integration.PrixMarcheSimuleAdapter;
import sn.sunuagri.adapters.integration.SmsSimuleAdapter;
import sn.sunuagri.adapters.persistence.memoire.CampagneRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.CommandeRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.LivraisonRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.LotRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.OffreRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.PaiementDuRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.ProducteurRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.RecolteRepositoryMemoire;
import sn.sunuagri.adapters.persistence.memoire.ReferentielCulturesMemoire;
import sn.sunuagri.adapters.persistence.memoire.ReservationsStockAdapter;
import sn.sunuagri.application.CampagneService;
import sn.sunuagri.application.CommercialisationService;
import sn.sunuagri.application.ConfigurationCooperative;
import sn.sunuagri.application.NotificationPaiementService;
import sn.sunuagri.application.ParametreService;
import sn.sunuagri.application.ProducteurService;
import sn.sunuagri.application.StockService;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.Role;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Livraison;
import sn.sunuagri.domaine.stock.Qualite;

/**
 * Câblage manuel des services et adaptateurs en mémoire : les tests métier
 * tournent sans serveur web, sans base de données et sans contexte Spring.
 */
public class ContexteTest {

    public final HorlogeFixe horloge = new HorlogeFixe(LocalDate.of(2026, 6, 1));
    public final IdSequence ids = new IdSequence();
    public final ConfigurationCooperative configuration = new ConfigurationCooperative(
            new BigDecimal("50"), new BigDecimal("50"), new BigDecimal("5"), new BigDecimal("30"));

    public final ProducteurRepositoryMemoire producteurs = new ProducteurRepositoryMemoire();
    public final CampagneRepositoryMemoire campagnes = new CampagneRepositoryMemoire();
    public final RecolteRepositoryMemoire recoltes = new RecolteRepositoryMemoire();
    public final ReferentielCulturesMemoire referentiel = new ReferentielCulturesMemoire();
    public final LotRepositoryMemoire lots = new LotRepositoryMemoire();
    public final LivraisonRepositoryMemoire livraisons = new LivraisonRepositoryMemoire();
    public final OffreRepositoryMemoire offres = new OffreRepositoryMemoire();
    public final CommandeRepositoryMemoire commandes = new CommandeRepositoryMemoire();
    public final PaiementDuRepositoryMemoire paiements = new PaiementDuRepositoryMemoire();
    public final ReservationsStockAdapter reservations = new ReservationsStockAdapter(offres);

    public final SmsSimuleAdapter sms = new SmsSimuleAdapter();
    public final PrixMarcheSimuleAdapter prixMarche = new PrixMarcheSimuleAdapter(horloge);

    public final ProducteurService producteurService;
    public final CampagneService campagneService;
    public final StockService stockService;
    public final NotificationPaiementService notifications;
    public final CommercialisationService commercialisation;
    public final ParametreService parametreService;

    public static final Identite AGENT = new Identite("agent-1", Role.AGENT);
    public static final Identite ADMIN = new Identite("admin-1", Role.ADMINISTRATEUR);

    public ContexteTest() {
        producteurService = new ProducteurService(producteurs, configuration, ids, horloge);
        campagneService = new CampagneService(campagnes, recoltes, referentiel, producteurs, ids);
        stockService = new StockService(recoltes, campagnes, lots, livraisons, reservations, ids, horloge);
        notifications = new NotificationPaiementService(sms, producteurs, paiements);
        commercialisation = new CommercialisationService(offres, commandes, paiements, lots,
                reservations, prixMarche, notifications, configuration, ids, horloge);
        parametreService = new ParametreService(referentiel, prixMarche);
    }

    public static Identite acheteur(String id) {
        return new Identite(id, Role.ACHETEUR);
    }

    public static Identite producteur(Producteur producteur) {
        return new Identite(producteur.id(), Role.PRODUCTEUR);
    }

    public Producteur enregistrerProducteur(String prenom, String nom, String telephone, String village) {
        return producteurService.enregistrer(AGENT,
                new ProducteurService.EnregistrerProducteur(nom, prenom, telephone, village));
    }

    public Parcelle declarerParcelle(Producteur producteur, String nom, String superficie, String commune) {
        return producteurService.declarerParcelle(AGENT, producteur.id(),
                new ProducteurService.DeclarerParcelle(nom, new BigDecimal(superficie), commune));
    }

    public Campagne ouvrirCampagne(Saison saison, int annee, LocalDate debut, LocalDate fin) {
        return campagneService.ouvrir(AGENT, new CampagneService.OuvrirCampagne(
                saison.name(), annee, debut, fin));
    }

    public Culture declarerCulture(Campagne campagne, Parcelle parcelle, TypeCulture type,
                                   String superficie) {
        return campagneService.declarerCulture(AGENT, new CampagneService.DeclarerCulture(
                campagne.id(), parcelle.id(), type.libelle(), new BigDecimal(superficie)));
    }

    public Recolte recolter(Culture culture, LocalDate semis, LocalDate recolte, String quantite) {
        campagneService.semer(AGENT, culture.id(), semis);
        return campagneService.recolter(AGENT, culture.id(),
                new CampagneService.Recolter(recolte, new BigDecimal(quantite)));
    }

    public Livraison livrer(Recolte recolte, String quantite, String humidite, Qualite qualite) {
        return stockService.livrer(AGENT, new StockService.Livrer(recolte.id(),
                new BigDecimal(quantite), new BigDecimal(humidite), qualite.name()));
    }

    public Offre publierOffre(TypeCulture type, Qualite qualite, String quantite, long prix) {
        return commercialisation.publierOffre(AGENT, new CommercialisationService.PublierOffre(
                type.libelle(), qualite.name(), new BigDecimal(quantite), prix)).offre();
    }

    public Commande commander(Identite acheteur, Offre offre, String quantite) {
        return commercialisation.passerCommande(acheteur,
                new CommercialisationService.PasserCommande(offre.id(), new BigDecimal(quantite)));
    }
}
