package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commun.AccesRefuseException;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.marche.PrixMarche;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

class PrixMarcheTest {

    @Test
    void affiche_le_dernier_prix_connu_quand_le_service_externe_est_indisponible() {
        ContexteTest ctx = new ContexteTest();

        PrixMarche prix = ctx.parametreService.prixDeReference(ContexteTest.AGENT, "Arachide");
        assertThat(prix.prixKg().francs()).isEqualTo(300);
        assertThat(prix.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(prix.issuDuCache()).isFalse();

        ctx.prixMarche.simulerPanne(true);
        PrixMarche enCache = ctx.parametreService.prixDeReference(ContexteTest.AGENT, "Arachide");
        assertThat(enCache.prixKg().francs()).isEqualTo(300);
        assertThat(enCache.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(enCache.issuDuCache()).isTrue();

        assertThatThrownBy(() -> ctx.parametreService.prixDeReference(
                ContexteTest.acheteur("acheteur-1"), "Arachide"))
                .isInstanceOf(AccesRefuseException.class);
    }

    @Test
    void une_offre_trop_eloignee_du_prix_est_publiee_avec_un_avertissement() {
        ContexteTest ctx = new ContexteTest();
        produireEtStocker(ctx);

        CommercialisationService.ResultatPublication loin =
                ctx.commercialisation.publierOffre(ContexteTest.AGENT,
                        new CommercialisationService.PublierOffre("Arachide", "A",
                                new BigDecimal("1000"), 500L));
        assertThat(loin.avertissementPrix()).isNotNull();
        assertThat(loin.avertissementPrix()).contains("s'écarte");

        CommercialisationService.ResultatPublication proche =
                ctx.commercialisation.publierOffre(ContexteTest.AGENT,
                        new CommercialisationService.PublierOffre("Arachide", "A",
                                new BigDecimal("1000"), 350L));
        assertThat(proche.avertissementPrix()).isNull();

        CommercialisationService.ResultatPublication limite =
                ctx.commercialisation.publierOffre(ContexteTest.AGENT,
                        new CommercialisationService.PublierOffre("Arachide", "A",
                                new BigDecimal("1000"), 390L));
        assertThat(limite.avertissementPrix()).isNull();
    }

    private void produireEtStocker(ContexteTest ctx) {
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "10");
        Recolte recolte = ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "5000");
        ctx.livrer(recolte, "5000", "8", Qualite.A);
    }
}
