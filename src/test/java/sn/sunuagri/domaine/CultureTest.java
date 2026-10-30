package sn.sunuagri.domaine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.campagne.StatutCulture;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Superficie;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.ReferencesParDefaut;

class CultureTest {

    private Campagne campagne() {
        return new Campagne("c1", Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
    }

    private Culture culture(Campagne campagne, String superficie) {
        return campagne.declarerCulture("cu1", "pa1", "p1", ReferencesParDefaut.ARACHIDE,
                new Superficie(new BigDecimal(superficie)), new Superficie(new BigDecimal("10")));
    }

    @Test
    void suit_l_ordre_planifiee_semee_recoltee() {
        Campagne campagne = campagne();
        Culture culture = culture(campagne, "2");
        assertThat(culture.statut()).isEqualTo(StatutCulture.PLANIFIEE);

        culture.semer(LocalDate.of(2026, 6, 10));
        assertThat(culture.statut()).isEqualTo(StatutCulture.SEMEE);

        culture.recolter(LocalDate.of(2026, 9, 10), Quantite.de(4000));
        assertThat(culture.statut()).isEqualTo(StatutCulture.RECOLTEE);
        assertThat(culture.quantiteRecoltee().enKg()).isEqualByComparingTo("4000");
    }

    @Test
    void refuse_une_recolte_avant_le_cycle_minimal() {
        Campagne campagne = campagne();
        Culture culture = culture(campagne, "2");
        culture.semer(LocalDate.of(2026, 6, 10));

        assertThatThrownBy(() -> culture.recolter(LocalDate.of(2026, 9, 7), Quantite.de(4000)))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("90");
    }

    @Test
    void refuse_un_rendement_superieur_au_double_de_la_reference() {
        Campagne campagne = campagne();
        Culture culture = culture(campagne, "1");
        culture.semer(LocalDate.of(2026, 6, 10));

        // 1 ha x 1200 kg/ha x 2 = 2400 kg maximum
        assertThatThrownBy(() -> culture.recolter(LocalDate.of(2026, 10, 1), Quantite.de(2401)))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("suspecte");
    }

    @Test
    void une_culture_ne_peut_etre_recoltee_qu_une_fois() {
        Campagne campagne = campagne();
        Culture culture = culture(campagne, "1");
        culture.semer(LocalDate.of(2026, 6, 10));
        culture.recolter(LocalDate.of(2026, 10, 1), Quantite.de(1000));

        assertThatThrownBy(() -> culture.recolter(LocalDate.of(2026, 10, 2), Quantite.de(1000)))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("semée");
    }

    @Test
    void refuse_de_depasser_la_superficie_de_la_parcelle() {
        Campagne campagne = campagne();
        campagne.declarerCulture("cu1", "pa1", "p1", ReferencesParDefaut.ARACHIDE,
                new Superficie(new BigDecimal("6")), new Superficie(new BigDecimal("10")));

        assertThatThrownBy(() -> campagne.declarerCulture("cu2", "pa1", "p1", ReferencesParDefaut.MIL,
                new Superficie(new BigDecimal("5")), new Superficie(new BigDecimal("10"))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("dépasse sa superficie");
    }

    @Test
    void une_campagne_cloturee_refuse_toute_nouvelle_culture() {
        Campagne campagne = campagne();
        culture(campagne, "1");
        campagne.cloturer();

        assertThatThrownBy(() -> culture(campagne, "2"))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("clôturée");
    }
}
