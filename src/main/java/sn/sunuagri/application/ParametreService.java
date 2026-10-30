package sn.sunuagri.application;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import sn.sunuagri.application.port.PrixMarchePort;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.culture.CultureReference;
import sn.sunuagri.domaine.culture.ReferentielCultures;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.marche.PrixMarche;

/** Cas d'usage des paramètres de cultures et du prix du marché (US-18, US-19). */
@Service
public class ParametreService {

    private final ReferentielCultures referentiel;
    private final PrixMarchePort prixMarche;

    public ParametreService(ReferentielCultures referentiel, PrixMarchePort prixMarche) {
        this.referentiel = referentiel;
        this.prixMarche = prixMarche;
    }

    public record ModifierParametre(BigDecimal rendementReferenceKgHa, BigDecimal humiditeMaxPct,
                                    Integer cycleMinJours) {
    }

    public List<CultureReference> lister(Identite identite) {
        Autorisations.exigerAdministrateur(identite);
        return referentiel.tous();
    }

    /**
     * US-19 : la modification s'applique aux déclarations futures. Les cultures
     * déjà déclarées conservent leur copie des paramètres.
     */
    public CultureReference modifier(Identite identite, String culture, ModifierParametre commande) {
        Autorisations.exigerAdministrateur(identite);
        TypeCulture type = TypeCulture.depuis(culture);
        if (commande == null || commande.cycleMinJours() == null
                || commande.rendementReferenceKgHa() == null || commande.humiditeMaxPct() == null) {
            throw new sn.sunuagri.domaine.commun.RegleMetierException(
                    "Le rendement, l'humidité maximale et le cycle minimal sont obligatoires");
        }
        CultureReference reference = new CultureReference(type,
                commande.rendementReferenceKgHa(), commande.humiditeMaxPct(),
                commande.cycleMinJours());
        referentiel.sauvegarder(reference);
        return reference;
    }

    /** US-18 : prix de référence, avec repli sur le dernier prix connu côté adaptateur. */
    public PrixMarche prixDeReference(Identite identite, String culture) {
        Autorisations.exigerAgent(identite);
        TypeCulture type = TypeCulture.depuis(culture);
        return prixMarche.prixActuel(type)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun prix de référence disponible pour " + type.libelle()));
    }
}
