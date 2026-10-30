package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.StatutOffre;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

class ReglesCommercialisationTest {

    private final ContexteTest ctx = new ContexteTest();
    private final Identite acheteur = ContexteTest.acheteur("acheteur-1");

    @Test
    void une_offre_ne_peut_pas_depasser_le_stock_disponible() {
        produireEtStocker(10_000);

        assertThatThrownBy(() -> ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "10001", 300))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("stock disponible");

        ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);
        assertThatThrownBy(() -> ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "5000", 300))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("4000");
    }

    @Test
    void retirer_une_offre_libere_toute_la_quantite_non_vendue() {
        produireEtStocker(10_000);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);

        ctx.commercialisation.retirerOffre(ContexteTest.AGENT, offre.id());

        assertThat(offre.statut()).isEqualTo(StatutOffre.RETIREE);
        assertThat(stock().disponible().enKg()).isEqualByComparingTo("10000");
        Offre nouvelle = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "10000", 300);
        assertThat(nouvelle.id()).isNotEqualTo(offre.id());
    }

    @Test
    void respecte_la_quantite_minimale_et_la_quantite_restante() {
        produireEtStocker(10_000);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "1000", 300);

        assertThatThrownBy(() -> ctx.commander(acheteur, offre, "49"))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("50");
        assertThatThrownBy(() -> ctx.commander(acheteur, offre, "1001"))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("dépasse");
        assertThat(offre.restante().enKg()).isEqualByComparingTo("1000");
    }

    @Test
    void annuler_une_commande_en_attente_libere_la_quantite() {
        produireEtStocker(10_000);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);
        Commande commande = ctx.commander(acheteur, offre, "2000");

        StockService.LigneStock avant = stock();
        assertThat(avant.total().enKg()).isEqualByComparingTo("10000");
        assertThat(avant.reserve().enKg()).isEqualByComparingTo("6000");
        assertThat(avant.disponible().enKg()).isEqualByComparingTo("4000");

        ctx.commercialisation.annulerCommande(acheteur, commande.id());
        assertThat(offre.restante().enKg()).isEqualByComparingTo("6000");

        StockService.LigneStock apres = stock();
        assertThat(apres.total().enKg()).isEqualByComparingTo("10000");
        assertThat(apres.reserve().enKg()).isEqualByComparingTo("6000");
        assertThat(apres.disponible().enKg()).isEqualByComparingTo("4000");

        ctx.commercialisation.retirerOffre(ContexteTest.AGENT, offre.id());
        assertThat(stock().disponible().enKg()).isEqualByComparingTo("10000");
    }

    @Test
    void une_commande_livree_ne_peut_ni_etre_annulee_ni_etre_livree_deux_fois() {
        produireEtStocker(10_000);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);
        Commande commande = ctx.commander(acheteur, offre, "1000");

        ctx.commercialisation.livrerCommande(ContexteTest.AGENT, commande.id());

        assertThatThrownBy(() -> ctx.commercialisation.livrerCommande(ContexteTest.AGENT, commande.id()))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("en attente");
        assertThatThrownBy(() -> ctx.commercialisation.annulerCommande(acheteur, commande.id()))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("livrée");
    }

    @Test
    void le_stock_reste_coherent_apres_une_vente() {
        produireEtStocker(10_000);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);
        Commande commande = ctx.commander(acheteur, offre, "3000");

        ctx.commercialisation.livrerCommande(ContexteTest.AGENT, commande.id());

        StockService.LigneStock stock = stock();
        assertThat(stock.total().enKg()).isEqualByComparingTo("7000");
        assertThat(stock.reserve().enKg()).isEqualByComparingTo("3000");
        assertThat(stock.disponible().enKg()).isEqualByComparingTo("4000");
        assertThat(offre.statut()).isEqualTo(StatutOffre.OUVERTE);
        assertThat(offre.restante().enKg()).isEqualByComparingTo("3000");
    }

    @Test
    void refuse_de_commander_ou_livrer_sans_le_bon_role() {
        produireEtStocker(10_000);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);

        assertThatThrownBy(() -> ctx.commercialisation.passerCommande(ContexteTest.AGENT,
                new CommercialisationService.PasserCommande(offre.id(), new BigDecimal("100"))))
                .isInstanceOf(sn.sunuagri.domaine.commun.AccesRefuseException.class);
    }

    private void produireEtStocker(int quantite) {
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "10");
        Recolte recolte = ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), String.valueOf(quantite));
        ctx.livrer(recolte, String.valueOf(quantite), "8", Qualite.A);
    }

    private StockService.LigneStock stock() {
        return ctx.stockService.consulterStock(ContexteTest.AGENT).stream()
                .filter(l -> l.culture() == TypeCulture.ARACHIDE && l.qualite() == Qualite.A)
                .findFirst().orElseThrow();
    }
}
