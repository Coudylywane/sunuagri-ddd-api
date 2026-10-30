package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

class ReglesProductionTest {

    @Test
    void refuse_un_telephone_deja_utilise() {
        ContexteTest ctx = new ContexteTest();
        ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");

        assertThatThrownBy(() -> ctx.enregistrerProducteur("Moussa", "Fall", "77 123 45 67", "Kaolack"))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("déjà enregistré");
    }

    @Test
    void une_saison_et_une_annee_n_ont_qu_une_campagne() {
        ContexteTest ctx = new ContexteTest();
        ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 11, 30));

        assertThatThrownBy(() -> ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 2), LocalDate.of(2026, 11, 29)))
                .isInstanceOf(ConflitException.class);

        // Une autre saison de la même année reste possible.
        Campagne contreSaison = ctx.ouvrirCampagne(Saison.CONTRE_SAISON, 2026,
                LocalDate.of(2026, 12, 1), LocalDate.of(2027, 3, 31));
        assertThat(contreSaison.estOuverte()).isTrue();
    }

    @Test
    void refuse_une_date_de_fin_anterieure_au_debut() {
        ContexteTest ctx = new ContexteTest();
        assertThatThrownBy(() -> ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("postérieure");
    }

    @Test
    void une_campagne_cloturee_refuse_cultures_et_recoltes() {
        ContexteTest ctx = new ContexteTest();
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "5");
        ctx.campagneService.semer(ContexteTest.AGENT, culture.id(), LocalDate.of(2026, 6, 5));
        ctx.campagneService.cloturer(ContexteTest.AGENT, campagne.id());

        assertThatThrownBy(() -> ctx.declarerCulture(campagne, parcelle, TypeCulture.MIL, "2"))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("clôturée");
        assertThatThrownBy(() -> ctx.campagneService.recolter(ContexteTest.AGENT, culture.id(),
                new CampagneService.Recolter(LocalDate.of(2026, 10, 5), new BigDecimal("3000"))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("clôturée");
    }

    @Test
    void refuse_une_livraison_trop_humide_et_ne_modifie_rien() {
        ContexteTest ctx = new ContexteTest();
        Recolte recolte = productionArachide(ctx);

        assertThatThrownBy(() -> ctx.livrer(recolte, "1000", "10", Qualite.A))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("humidité");
        assertThat(ctx.lots.tous()).isEmpty();
        assertThat(ctx.livraisons.toutes()).isEmpty();

        ctx.livrer(recolte, "1000", "9", Qualite.A);
        assertThat(ctx.lots.tous()).hasSize(1);
    }

    @Test
    void refuse_de_livrer_plus_que_la_quantite_recoltee() {
        ContexteTest ctx = new ContexteTest();
        Recolte recolte = productionArachide(ctx);

        ctx.livrer(recolte, "1500", "8", Qualite.A);
        assertThatThrownBy(() -> ctx.livrer(recolte, "600", "8", Qualite.B))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("dépasse la quantité récoltée");
        assertThat(ctx.lots.tous()).hasSize(1);
    }

    @Test
    void une_modification_de_parametre_ne_touche_que_les_declarations_futures() {
        ContexteTest ctx = new ContexteTest();
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture ancienne = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "2");

        ctx.parametreService.modifier(ContexteTest.ADMIN, "Arachide",
                new ParametreService.ModifierParametre(new BigDecimal("1000"), new BigDecimal("7"), 60));

        Culture nouvelle = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "2");
        assertThat(ancienne.reference().humiditeMaxPct()).isEqualByComparingTo("9");
        assertThat(ancienne.reference().rendementReferenceKgHa()).isEqualByComparingTo("1200");
        assertThat(ancienne.reference().cycleMinJours()).isEqualTo(90);
        assertThat(nouvelle.reference().humiditeMaxPct()).isEqualByComparingTo("7");
        assertThat(nouvelle.reference().rendementReferenceKgHa()).isEqualByComparingTo("1000");
        assertThat(nouvelle.reference().cycleMinJours()).isEqualTo(60);

        Recolte recolteAncienne = ctx.recolter(ancienne, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "2000");
        ctx.campagneService.semer(ContexteTest.AGENT, nouvelle.id(), LocalDate.of(2026, 6, 5));
        assertThatThrownBy(() -> ctx.campagneService.recolter(ContexteTest.AGENT, nouvelle.id(),
                new CampagneService.Recolter(LocalDate.of(2026, 8, 1), new BigDecimal("1500"))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("60");
        Recolte recolteNouvelle = ctx.campagneService.recolter(ContexteTest.AGENT, nouvelle.id(),
                new CampagneService.Recolter(LocalDate.of(2026, 9, 5), new BigDecimal("1500")));

        ctx.livrer(recolteAncienne, "1000", "8", Qualite.A);
        assertThatThrownBy(() -> ctx.livrer(recolteNouvelle, "1000", "8", Qualite.A))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("7");
    }

    @Test
    void refuse_des_parametres_non_positifs() {
        ContexteTest ctx = new ContexteTest();
        assertThatThrownBy(() -> ctx.parametreService.modifier(ContexteTest.ADMIN, "Mil",
                new ParametreService.ModifierParametre(BigDecimal.ZERO, new BigDecimal("12"), 75)))
                .isInstanceOf(RegleMetierException.class);
        assertThatThrownBy(() -> ctx.parametreService.modifier(ContexteTest.AGENT, "Mil",
                new ParametreService.ModifierParametre(new BigDecimal("800"), new BigDecimal("12"), 75)))
                .isInstanceOf(sn.sunuagri.domaine.commun.AccesRefuseException.class);
    }

    private Recolte productionArachide(ContexteTest ctx) {
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "2");
        return ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "2000");
    }
}
