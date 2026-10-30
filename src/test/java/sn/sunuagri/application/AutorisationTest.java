package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commun.AccesRefuseException;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

class AutorisationTest {

    private final ContexteTest ctx = new ContexteTest();
    private Producteur awa;
    private Producteur moussa;

    @Test
    void un_producteur_ne_voit_que_ses_propres_donnees() {
        preparerDeuxProducteurs();
        Identite identiteAwa = ContexteTest.producteur(awa);

        assertThat(ctx.producteurService.parcelles(identiteAwa, awa.id())).hasSize(1);
        assertThatThrownBy(() -> ctx.producteurService.parcelles(identiteAwa, moussa.id()))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessage("Producteur introuvable");
        assertThatThrownBy(() -> ctx.producteurService.parcelle(identiteAwa, awa.id(),
                moussa.parcelles().get(0).id()))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessage("Parcelle introuvable");

        ctx.commercialisation.paiementsDuProducteur(identiteAwa, awa.id());
        assertThatThrownBy(() -> ctx.commercialisation.paiementsDuProducteur(identiteAwa, moussa.id()))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void un_acheteur_ne_voit_pas_les_donnees_des_producteurs() {
        preparerDeuxProducteurs();
        Identite acheteur = ContexteTest.acheteur("acheteur-1");

        assertThatThrownBy(() -> ctx.producteurService.parcelles(acheteur, awa.id()))
                .isInstanceOf(RessourceIntrouvableException.class);
        assertThatThrownBy(() -> ctx.stockService.consulterStock(acheteur))
                .isInstanceOf(AccesRefuseException.class);
        assertThatThrownBy(() -> ctx.commercialisation.livrerCommande(acheteur, "inconnue"))
                .isInstanceOf(AccesRefuseException.class);
    }

    @Test
    void un_agent_accede_aux_donnees_de_la_cooperative() {
        preparerDeuxProducteurs();
        assertThat(ctx.producteurService.parcelles(ContexteTest.AGENT, moussa.id())).hasSize(1);
        assertThat(ctx.producteurService.tous(ContexteTest.AGENT)).hasSize(2);
    }

    @Test
    void un_acheteur_ne_voit_que_ses_propres_commandes() {
        preparerDeuxProducteurs();
        produireEtStocker();
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "5000", 300);

        Identite acheteur1 = ContexteTest.acheteur("acheteur-1");
        Identite acheteur2 = ContexteTest.acheteur("acheteur-2");
        Commande commande1 = ctx.commander(acheteur1, offre, "100");
        Commande commande2 = ctx.commander(acheteur2, offre, "100");

        assertThat(ctx.commercialisation.historiqueCommandes(acheteur1, null, 0, 20).total())
                .isEqualTo(1);
        assertThat(ctx.commercialisation.historiqueCommandes(acheteur1, null, 0, 20)
                .commandes().get(0).id()).isEqualTo(commande1.id());
        assertThat(ctx.commercialisation.historiqueCommandes(acheteur2, null, 0, 20)
                .commandes().get(0).id()).isEqualTo(commande2.id());

        assertThatThrownBy(() -> ctx.commercialisation.annulerCommande(acheteur1, commande2.id()))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessage("Commande introuvable");
    }

    @Test
    void un_producteur_ne_voit_que_les_offres_ouvertes() {
        preparerDeuxProducteurs();
        produireEtStocker();
        Offre ouverte = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "3000", 300);
        Offre retiree = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "1000", 300);
        ctx.commercialisation.retirerOffre(ContexteTest.AGENT, retiree.id());

        Identite identiteAwa = ContexteTest.producteur(awa);
        assertThat(ctx.commercialisation.offresVisibles(identiteAwa))
                .extracting(Offre::id)
                .contains(ouverte.id())
                .doesNotContain(retiree.id());
    }

    private void preparerDeuxProducteurs() {
        awa = ctx.enregistrerProducteur("Awa", "Diop", "771111111", "Thiès");
        ctx.declarerParcelle(awa, "Nord", "10", "Thiès");
        moussa = ctx.enregistrerProducteur("Moussa", "Fall", "772222222", "Kaolack");
        ctx.declarerParcelle(moussa, "Sud", "8", "Kaolack");
    }

    private void produireEtStocker() {
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Parcelle parcelle = ctx.producteurService.parcelles(ContexteTest.AGENT, awa.id()).get(0);
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "10");
        Recolte recolte = ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "6000");
        ctx.livrer(recolte, "6000", "8", Qualite.A);
    }
}
