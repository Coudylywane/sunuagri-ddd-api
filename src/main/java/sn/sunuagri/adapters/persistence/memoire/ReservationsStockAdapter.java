package sn.sunuagri.adapters.persistence.memoire;

import org.springframework.stereotype.Component;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.OffreRepository;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.domaine.stock.ReservationsStock;

/** Adaptateur qui traduit les offres publiées en quantités réservées pour le stock. */
@Component
public class ReservationsStockAdapter implements ReservationsStock {

    private final OffreRepository offres;

    public ReservationsStockAdapter(OffreRepository offres) {
        this.offres = offres;
    }

    @Override
    public Quantite quantiteReservee(TypeCulture typeCulture, Qualite qualite) {
        return offres.toutes().stream()
                .filter(o -> o.typeCulture() == typeCulture && o.qualite() == qualite)
                .map(Offre::quantiteReservee)
                .reduce(Quantite.zero(), Quantite::plus);
    }
}
