package sn.sunuagri.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import sn.sunuagri.application.port.HorlogePort;
import sn.sunuagri.application.port.IdGeneratorPort;
import sn.sunuagri.application.port.PrixMarchePort;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.CommandeRepository;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.OffreRepository;
import sn.sunuagri.domaine.commercialisation.PaiementDu;
import sn.sunuagri.domaine.commercialisation.PaiementDuRepository;
import sn.sunuagri.domaine.commercialisation.RepartitionPaiement;
import sn.sunuagri.domaine.commercialisation.StatutCommande;
import sn.sunuagri.domaine.commercialisation.StatutOffre;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.marche.AnalysePrix;
import sn.sunuagri.domaine.stock.Lot;
import sn.sunuagri.domaine.stock.LotRepository;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.domaine.stock.ReservationsStock;

/** Cas d'usage de la commercialisation et des paiements (US-10 à US-14, US-16). */
@Service
public class CommercialisationService {

    private final OffreRepository offres;
    private final CommandeRepository commandes;
    private final PaiementDuRepository paiements;
    private final LotRepository lots;
    private final ReservationsStock reservations;
    private final PrixMarchePort prixMarche;
    private final NotificationPaiementService notifications;
    private final ConfigurationCooperative configuration;
    private final IdGeneratorPort ids;
    private final HorlogePort horloge;

    public CommercialisationService(OffreRepository offres, CommandeRepository commandes,
                                    PaiementDuRepository paiements, LotRepository lots,
                                    ReservationsStock reservations, PrixMarchePort prixMarche,
                                    NotificationPaiementService notifications,
                                    ConfigurationCooperative configuration,
                                    IdGeneratorPort ids, HorlogePort horloge) {
        this.offres = offres;
        this.commandes = commandes;
        this.paiements = paiements;
        this.lots = lots;
        this.reservations = reservations;
        this.prixMarche = prixMarche;
        this.notifications = notifications;
        this.configuration = configuration;
        this.ids = ids;
        this.horloge = horloge;
    }

    public record PublierOffre(String culture, String qualite, BigDecimal quantiteKg, Long prixKg) {
    }

    public record ResultatPublication(Offre offre, String avertissementPrix) {
    }

    public record PasserCommande(String offreId, BigDecimal quantiteKg) {
    }

    public record ResultatLivraison(Commande commande, Montant commission, Montant montantNet,
                                    List<PaiementDu> paiements) {
    }

    public record PageCommandes(List<Commande> commandes, long total, int page, int taille) {
    }

    /** US-10 : publie une offre sur le stock disponible et réserve la quantité. */
    public ResultatPublication publierOffre(Identite identite, PublierOffre commande) {
        Autorisations.exigerAgent(identite);
        TypeCulture type = TypeCulture.depuis(commande.culture());
        Qualite qualite = Qualite.depuis(commande.qualite());
        Quantite quantite = Quantite.de(commande.quantiteKg());
        if (commande.prixKg() == null) {
            throw new RegleMetierException("Le prix au kg est obligatoire");
        }
        Montant prix = Montant.de(commande.prixKg());
        if (!prix.estPositif()) {
            throw new RegleMetierException("Le prix au kg doit être strictement positif");
        }
        Quantite total = totalStock(type, qualite);
        Quantite reserve = reservations.quantiteReservee(type, qualite);
        Quantite disponible = total.kg().compareTo(reserve.kg()) >= 0
                ? total.moins(reserve) : Quantite.zero();
        if (quantite.estSuperieureA(disponible)) {
            throw new RegleMetierException(
                    "La quantité de l'offre dépasse le stock disponible (" + disponible.kg().toPlainString()
                            + " kg disponibles pour " + type.libelle() + " qualité " + qualite + ")");
        }
        Offre offre = new Offre(ids.nouvelId(), type, qualite, quantite, prix);
        String avertissement = prixMarche.prixActuel(type)
                .flatMap(reference -> AnalysePrix.avertissementSiEcart(
                        prix, reference, configuration.ecartPrixPct()))
                .orElse(null);
        offres.sauvegarder(offre);
        return new ResultatPublication(offre, avertissement);
    }

    public Offre retirerOffre(Identite identite, String offreId) {
        Autorisations.exigerAgent(identite);
        Offre offre = offre(offreId);
        offre.retirer();
        offres.sauvegarder(offre);
        return offre;
    }

    public List<Offre> offresVisibles(Identite identite) {
        if (identite == null) {
            throw new RessourceIntrouvableException("Offre introuvable");
        }
        return offres.toutes().stream()
                .filter(o -> identite.estAgentOuAdministrateur() || o.statut() == StatutOffre.OUVERTE)
                .sorted(Comparator.comparing(Offre::id))
                .toList();
    }

    /** US-11 : passe une commande sur une offre ouverte. */
    public Commande passerCommande(Identite identite, PasserCommande commande) {
        Autorisations.exigerAcheteur(identite);
        Offre offre = offre(commande.offreId());
        Quantite quantite = Quantite.de(commande.quantiteKg());
        Commande nouvelle = Commande.creer(ids.nouvelId(), offre, identite.utilisateurId(), quantite,
                new Quantite(configuration.commandeMinKg()), horloge.aujourdHui());
        offre.commander(quantite);
        commandes.sauvegarder(nouvelle);
        offres.sauvegarder(offre);
        return nouvelle;
    }

    /** US-12 : annule une commande non livrée et libère la quantité réservée. */
    public Commande annulerCommande(Identite identite, String commandeId) {
        Autorisations.exigerAcheteur(identite);
        Commande commande = commandeDeLAcheteur(identite, commandeId);
        Offre offre = offre(commande.offreId());
        commande.annuler();
        offre.annuler(commande.quantite());
        commandes.sauvegarder(commande);
        offres.sauvegarder(offre);
        return commande;
    }

    /**
     * US-13 : livre la commande, consomme les lots du plus ancien au plus récent,
     * retient la commission de la coopérative et crée un paiement dû par producteur.
     * La somme des paiements et de la commission est exactement le montant de la commande.
     */
    public ResultatLivraison livrerCommande(Identite identite, String commandeId) {
        Autorisations.exigerAgent(identite);
        Commande commande = commande(commandeId);
        if (!commande.estEnAttente()) {
            throw new ConflitException("Seule une commande en attente peut être livrée");
        }
        Offre offre = offre(commande.offreId());

        List<Lot> lotsDisponibles = lots.parCultureEtQualite(commande.typeCulture(), commande.qualite());
        Quantite stockPhysique = lotsDisponibles.stream()
                .map(Lot::quantiteRestante)
                .reduce(Quantite.zero(), Quantite::plus);
        if (commande.quantite().estSuperieureA(stockPhysique)) {
            throw new ConflitException(
                    "Stock insuffisant pour livrer la commande : "
                            + stockPhysique.kg().toPlainString() + " kg disponibles");
        }

        Quantite restant = commande.quantite();
        Map<String, BigDecimal> kgParProducteur = new LinkedHashMap<>();
        for (Lot lot : lotsDisponibles) {
            if (restant.estNulle()) {
                break;
            }
            Quantite prise = restant.minimum(lot.quantiteRestante());
            if (prise.estNulle()) {
                continue;
            }
            lot.consommer(prise);
            lots.sauvegarder(lot);
            kgParProducteur.merge(lot.producteurId(), prise.kg(), BigDecimal::add);
            restant = restant.moins(prise);
        }

        Montant montant = commande.montant();
        Montant commission = montant.pourcentageArrondiInferieur(configuration.commissionPct());
        Montant montantNet = montant.moins(commission);
        List<RepartitionPaiement.Part> parts = RepartitionPaiement.calculer(montantNet, kgParProducteur);

        LocalDate date = horloge.aujourdHui();
        List<PaiementDu> paiementsCrees = new ArrayList<>();
        for (RepartitionPaiement.Part part : parts) {
            PaiementDu paiement = new PaiementDu(ids.nouvelId(), commande.id(),
                    part.producteurId(), part.montant(), date);
            paiements.sauvegarder(paiement);
            paiementsCrees.add(paiement);
        }

        commande.livrer(date);
        offre.livrer(commande.quantite());
        commandes.sauvegarder(commande);
        offres.sauvegarder(offre);

        notifications.notifier(paiementsCrees);
        return new ResultatLivraison(commande, commission, montantNet, paiementsCrees);
    }

    /** US-16 : historique paginé de l'acheteur, du plus récent au plus ancien. */
    public PageCommandes historiqueCommandes(Identite identite, StatutCommande statut,
                                             int page, int taille) {
        Autorisations.exigerAcheteur(identite);
        int pageEffective = Math.max(page, 0);
        int tailleEffective = taille <= 0 ? 20 : Math.min(taille, 100);
        List<Commande> filtrees = commandes.parAcheteur(identite.utilisateurId()).stream()
                .filter(c -> statut == null || c.statut() == statut)
                .toList();
        int debut = Math.min(pageEffective * tailleEffective, filtrees.size());
        int fin = Math.min(debut + tailleEffective, filtrees.size());
        return new PageCommandes(filtrees.subList(debut, fin), filtrees.size(),
                pageEffective, tailleEffective);
    }

    /** US-17 : un producteur ne voit que ses propres paiements. */
    public List<PaiementDu> paiementsDuProducteur(Identite identite, String producteurId) {
        Autorisations.accederProducteur(identite, producteurId);
        return paiements.parProducteur(producteurId);
    }

    public List<PaiementDu> paiementsDeLaCommande(Identite identite, String commandeId) {
        Autorisations.exigerAgent(identite);
        return paiements.parCommande(commandeId);
    }

    private Quantite totalStock(TypeCulture type, Qualite qualite) {
        return lots.parCultureEtQualite(type, qualite).stream()
                .map(Lot::quantiteRestante)
                .reduce(Quantite.zero(), Quantite::plus);
    }

    private Offre offre(String offreId) {
        return offres.parId(offreId)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable"));
    }

    private Commande commande(String commandeId) {
        return commandes.parId(commandeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable"));
    }

    private Commande commandeDeLAcheteur(Identite identite, String commandeId) {
        Commande commande = commande(commandeId);
        if (!commande.acheteurId().equals(identite.utilisateurId())) {
            throw new RessourceIntrouvableException("Commande introuvable");
        }
        return commande;
    }
}
