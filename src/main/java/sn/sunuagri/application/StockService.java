package sn.sunuagri.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import sn.sunuagri.application.port.HorlogePort;
import sn.sunuagri.application.port.IdGeneratorPort;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.CampagneRepository;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.RecolteRepository;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.stock.Livraison;
import sn.sunuagri.domaine.stock.LivraisonRepository;
import sn.sunuagri.domaine.stock.Lot;
import sn.sunuagri.domaine.stock.LotRepository;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.domaine.stock.ReservationsStock;

/** Cas d'usage des livraisons à l'entrepôt et du stock (US-08, US-09). */
@Service
public class StockService {

    private final RecolteRepository recoltes;
    private final CampagneRepository campagnes;
    private final LotRepository lots;
    private final LivraisonRepository livraisons;
    private final ReservationsStock reservations;
    private final IdGeneratorPort ids;
    private final HorlogePort horloge;

    public StockService(RecolteRepository recoltes, CampagneRepository campagnes, LotRepository lots,
                        LivraisonRepository livraisons, ReservationsStock reservations,
                        IdGeneratorPort ids, HorlogePort horloge) {
        this.recoltes = recoltes;
        this.campagnes = campagnes;
        this.lots = lots;
        this.livraisons = livraisons;
        this.reservations = reservations;
        this.ids = ids;
        this.horloge = horloge;
    }

    public record Livrer(String recolteId, BigDecimal quantiteKg, BigDecimal tauxHumiditePct,
                         String qualite) {
    }

    public record LigneStock(TypeCulture culture, Qualite qualite, Quantite total,
                             Quantite reserve, Quantite disponible) {
    }

    public Livraison livrer(Identite identite, Livrer commande) {
        Autorisations.exigerAgent(identite);
        Recolte recolte = recoltes.parId(commande.recolteId())
                .orElseThrow(() -> new RessourceIntrouvableException("Récolte introuvable"));
        Campagne campagne = campagnes.parCultureId(recolte.cultureId())
                .orElseThrow(() -> new RessourceIntrouvableException("Campagne introuvable"));
        campagne.exigerOuverte();
        Culture culture = campagne.cultureExistante(recolte.cultureId());

        Quantite quantite = Quantite.de(commande.quantiteKg());
        BigDecimal humidite = commande.tauxHumiditePct();
        if (humidite == null) {
            throw new RegleMetierException("Le taux d'humidité est obligatoire");
        }
        if (humidite.signum() < 0 || humidite.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RegleMetierException("Le taux d'humidité doit être compris entre 0 et 100 %");
        }
        if (!culture.reference().humiditeAcceptee(humidite)) {
            throw new RegleMetierException(
                    "Livraison refusée : le taux d'humidité dépasse le seuil de "
                            + culture.reference().humiditeMaxPct().toPlainString() + " % pour "
                            + culture.reference().type().libelle());
        }
        BigDecimal dejaLivree = livraisons.parRecolte(recolte.id()).stream()
                .map(l -> l.quantite().kg())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dejaLivree.add(quantite.kg()).compareTo(recolte.quantite().kg()) > 0) {
            throw new RegleMetierException(
                    "Le cumul des quantités livrées dépasse la quantité récoltée ("
                            + recolte.quantite().kg().toPlainString() + " kg)");
        }

        Qualite qualite = Qualite.depuis(commande.qualite());
        LocalDate date = horloge.aujourdHui();
        Lot lot = new Lot(ids.nouvelId(), recolte.producteurId(), recolte.id(),
                recolte.typeCulture(), qualite, quantite, humidite, date);
        Livraison livraison = new Livraison(ids.nouvelId(), recolte.id(), lot.id(),
                recolte.producteurId(), qualite, quantite, humidite, date);
        lots.sauvegarder(lot);
        livraisons.sauvegarder(livraison);
        return livraison;
    }

    /** US-09 : stock par culture et qualité, avec total, réservé et disponible. */
    public List<LigneStock> consulterStock(Identite identite) {
        Autorisations.exigerAgent(identite);
        Map<CleStock, Quantite> totaux = new ConcurrentHashMap<>();
        for (Lot lot : lots.tous()) {
            CleStock cle = new CleStock(lot.typeCulture(), lot.qualite());
            totaux.merge(cle, lot.quantiteRestante(), Quantite::plus);
        }
        List<LigneStock> resultat = new ArrayList<>();
        for (Map.Entry<CleStock, Quantite> entree : totaux.entrySet()) {
            Quantite total = entree.getValue();
            Quantite reserve = reservations.quantiteReservee(
                    entree.getKey().culture(), entree.getKey().qualite());
            Quantite disponible = total.kg().compareTo(reserve.kg()) >= 0
                    ? total.moins(reserve)
                    : Quantite.zero();
            resultat.add(new LigneStock(entree.getKey().culture(), entree.getKey().qualite(),
                    total, reserve, disponible));
        }
        resultat.sort(Comparator.comparing((LigneStock l) -> l.culture().name())
                .thenComparing(l -> l.qualite().name()));
        return resultat;
    }

    private record CleStock(TypeCulture culture, Qualite qualite) {
    }
}
