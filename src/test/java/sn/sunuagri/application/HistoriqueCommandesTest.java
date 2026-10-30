package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.StatutCommande;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

class HistoriqueCommandesTest {

    @Test
    void pagine_du_plus_recent_au_plus_ancien_et_filtre_par_statut() {
        ContexteTest ctx = new ContexteTest();
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "10");
        Recolte recolte = ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "2000");
        ctx.livrer(recolte, "2000", "8", Qualite.A);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "2000", 300);

        Identite acheteur = ContexteTest.acheteur("acheteur-1");
        List<Commande> creees = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            creees.add(ctx.commander(acheteur, offre, "50"));
        }

        CommercialisationService.PageCommandes page0 =
                ctx.commercialisation.historiqueCommandes(acheteur, null, 0, 10);
        assertThat(page0.total()).isEqualTo(25);
        assertThat(page0.commandes()).hasSize(10);
        assertThat(page0.commandes().get(0).id()).isEqualTo(creees.get(24).id());
        assertThat(page0.commandes().get(9).id()).isEqualTo(creees.get(15).id());

        CommercialisationService.PageCommandes page2 =
                ctx.commercialisation.historiqueCommandes(acheteur, null, 2, 10);
        assertThat(page2.commandes()).hasSize(5);
        assertThat(page2.commandes().get(4).id()).isEqualTo(creees.get(0).id());

        CommercialisationService.PageCommandes horsPage =
                ctx.commercialisation.historiqueCommandes(acheteur, null, 10, 10);
        assertThat(horsPage.commandes()).isEmpty();
        assertThat(horsPage.total()).isEqualTo(25);

        CommercialisationService.PageCommandes taillePlafonnee =
                ctx.commercialisation.historiqueCommandes(acheteur, null, 0, 500);
        assertThat(taillePlafonnee.taille()).isEqualTo(100);

        ctx.commercialisation.annulerCommande(acheteur, creees.get(3).id());
        CommercialisationService.PageCommandes annulees =
                ctx.commercialisation.historiqueCommandes(acheteur, StatutCommande.ANNULEE, 0, 20);
        assertThat(annulees.total()).isEqualTo(1);
        assertThat(annulees.commandes().get(0).id()).isEqualTo(creees.get(3).id());
        assertThat(annulees.commandes().get(0).montant().francs()).isEqualTo(15_000);
    }

    @Test
    void une_taille_ou_une_page_invalide_est_normalisee() {
        ContexteTest ctx = new ContexteTest();
        Identite acheteur = ContexteTest.acheteur("acheteur-1");

        CommercialisationService.PageCommandes page =
                ctx.commercialisation.historiqueCommandes(acheteur, null, -3, -1);
        assertThat(page.page()).isZero();
        assertThat(page.taille()).isEqualTo(20);
        assertThat(page.total()).isZero();
        assertThat(page.commandes()).isEmpty();
    }
}
