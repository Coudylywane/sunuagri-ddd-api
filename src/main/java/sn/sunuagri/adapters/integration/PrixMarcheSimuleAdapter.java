package sn.sunuagri.adapters.integration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import sn.sunuagri.application.port.HorlogePort;
import sn.sunuagri.application.port.PrixMarchePort;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.marche.PrixMarche;

/**
 * Simulation d'un service externe de prix. En cas de panne, le dernier prix
 * connu est renvoyé avec sa date et un indicateur de cache (US-18).
 */
@Component
public class PrixMarcheSimuleAdapter implements PrixMarchePort {

    private static final Map<TypeCulture, Montant> PRIX_SIMULES = Map.of(
            TypeCulture.ARACHIDE, Montant.de(300),
            TypeCulture.MIL, Montant.de(200),
            TypeCulture.RIZ, Montant.de(400),
            TypeCulture.MAIS, Montant.de(250));

    private final HorlogePort horloge;
    private final Map<TypeCulture, PrixMarche> derniersPrix = new ConcurrentHashMap<>();
    private volatile boolean enPanne = false;

    public PrixMarcheSimuleAdapter(HorlogePort horloge) {
        this.horloge = horloge;
    }

    @Override
    public Optional<PrixMarche> prixActuel(TypeCulture typeCulture) {
        if (typeCulture == null) {
            return Optional.empty();
        }
        if (!enPanne) {
            Montant prix = PRIX_SIMULES.get(typeCulture);
            if (prix == null) {
                return Optional.empty();
            }
            PrixMarche prixMarche = new PrixMarche(prix, horloge.aujourdHui(), false);
            derniersPrix.put(typeCulture, prixMarche);
            return Optional.of(prixMarche);
        }
        PrixMarche dernier = derniersPrix.get(typeCulture);
        if (dernier != null) {
            return Optional.of(new PrixMarche(dernier.prixKg(), dernier.date(), true));
        }
        Montant prix = PRIX_SIMULES.get(typeCulture);
        if (prix == null) {
            return Optional.empty();
        }
        return Optional.of(new PrixMarche(prix, horloge.aujourdHui(), true));
    }

    public void simulerPanne(boolean panne) {
        this.enPanne = panne;
    }
}
