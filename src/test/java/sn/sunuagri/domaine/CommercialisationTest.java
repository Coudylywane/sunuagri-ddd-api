package sn.sunuagri.domaine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.StatutCommande;
import sn.sunuagri.domaine.commercialisation.StatutOffre;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.stock.Qualite;

class CommercialisationTest {

    private Offre offre() {
        return new Offre("o1", TypeCulture.ARACHIDE, Qualite.A, Quantite.de(1000), Montant.de(300));
    }

    @Test
    void une_offre_reserve_la_quantite_publiee_puis_la_libere_a_la_vente() {
        Offre offre = offre();
        assertThat(offre.quantiteReservee().enKg()).isEqualByComparingTo("1000");

        offre.commander(Quantite.de(600));
        assertThat(offre.restante().enKg()).isEqualByComparingTo("400");
        assertThat(offre.quantiteReservee().enKg()).isEqualByComparingTo("1000");

        offre.livrer(Quantite.de(600));
        assertThat(offre.quantiteVendue().enKg()).isEqualByComparingTo("600");
        assertThat(offre.quantiteReservee().enKg()).isEqualByComparingTo("400");
        assertThat(offre.statut()).isEqualTo(StatutOffre.OUVERTE);
    }

    @Test
    void une_offre_devient_epuisee_quand_plus_rien_ne_reste() {
        Offre offre = offre();
        offre.commander(Quantite.de(1000));
        assertThat(offre.statut()).isEqualTo(StatutOffre.EPUISEE);
        assertThatThrownBy(() -> offre.commander(Quantite.de(1)))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void annuler_une_commande_libere_la_quantite_de_l_offre() {
        Offre offre = offre();
        offre.commander(Quantite.de(1000));
        assertThat(offre.statut()).isEqualTo(StatutOffre.EPUISEE);

        offre.annuler(Quantite.de(400));
        assertThat(offre.restante().enKg()).isEqualByComparingTo("400");
        assertThat(offre.statut()).isEqualTo(StatutOffre.OUVERTE);
    }

    @Test
    void retirer_une_offre_libere_toute_la_quantite_non_vendue() {
        Offre offre = offre();
        offre.retirer();
        assertThat(offre.statut()).isEqualTo(StatutOffre.RETIREE);
        assertThat(offre.quantiteReservee().enKg()).isEqualByComparingTo("0");
    }

    @Test
    void refuse_de_retirer_une_offre_avec_des_commandes_en_attente() {
        Offre offre = offre();
        offre.commander(Quantite.de(100));
        assertThatThrownBy(offre::retirer).isInstanceOf(ConflitException.class);
    }

    @Test
    void commande_minimale_de_50_kg_et_montant_calcule() {
        Offre offre = offre();
        assertThatThrownBy(() -> Commande.creer("c1", offre, "a1", Quantite.de(49),
                Quantite.de(50), LocalDate.now()))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("50");

        Commande commande = Commande.creer("c2", offre, "a1", Quantite.de(50),
                Quantite.de(50), LocalDate.now());
        assertThat(commande.montant().francs()).isEqualTo(15_000);
        assertThat(commande.statut()).isEqualTo(StatutCommande.EN_ATTENTE);
    }

    @Test
    void une_commande_livree_ne_peut_pas_etre_annulee() {
        Commande commande = Commande.creer("c1", offre(), "a1", Quantite.de(50),
                Quantite.de(50), LocalDate.now());
        commande.livrer(LocalDate.now());
        assertThatThrownBy(commande::annuler)
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("livrée");
    }
}
