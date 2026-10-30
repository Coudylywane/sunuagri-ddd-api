package sn.sunuagri.domaine;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.commercialisation.RepartitionPaiement;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.marche.AnalysePrix;
import sn.sunuagri.domaine.marche.PrixMarche;

class RepartitionPaiementTest {

    @Test
    void repartit_proportionnellement_et_conserve_exactement_le_montant() {
        Map<String, BigDecimal> kg = new LinkedHashMap<>();
        kg.put("p1", new BigDecimal("100"));
        kg.put("p2", new BigDecimal("200"));

        List<RepartitionPaiement.Part> parts = RepartitionPaiement.calculer(Montant.de(950_000), kg);

        long total = parts.stream().mapToLong(p -> p.montant().francs()).sum();
        assertThat(total).isEqualTo(950_000);
        assertThat(parts).extracting(RepartitionPaiement.Part::producteurId)
                .containsExactly("p1", "p2");
        assertThat(parts.get(0).montant().francs()).isEqualTo(316_667);
        assertThat(parts.get(1).montant().francs()).isEqualTo(633_333);
    }

    @Test
    void gere_les_petits_montants_sans_perdre_de_franc() {
        Map<String, BigDecimal> kg = Map.of("p1", BigDecimal.ONE, "p2", BigDecimal.ONE,
                "p3", BigDecimal.ONE);
        List<RepartitionPaiement.Part> parts = RepartitionPaiement.calculer(Montant.de(100), kg);

        assertThat(parts.stream().mapToLong(p -> p.montant().francs()).sum()).isEqualTo(100);
    }

    @Test
    void calcule_la_commission_avec_arrondi_inferieur() {
        // 1 050 000 x 5 % = 52 500
        assertThat(Montant.de(1_050_000).pourcentageArrondiInferieur(new BigDecimal("5")).francs())
                .isEqualTo(52_500);
        // 333 x 5 % = 16,65 -> 16 (floor)
        assertThat(Montant.de(333).pourcentageArrondiInferieur(new BigDecimal("5")).francs())
                .isEqualTo(16);
    }

    @Test
    void avertit_au_dela_de_30_pour_cent_d_ecart_sans_refuser() {
        PrixMarche reference = new PrixMarche(Montant.de(300), LocalDate.of(2026, 6, 1), false);

        assertThat(AnalysePrix.avertissementSiEcart(Montant.de(400), reference, new BigDecimal("30")))
                .isPresent();
        assertThat(AnalysePrix.avertissementSiEcart(Montant.de(350), reference, new BigDecimal("30")))
                .isEmpty();
        assertThat(AnalysePrix.avertissementSiEcart(Montant.de(390), reference, new BigDecimal("30")))
                .isEmpty();
        assertThat(AnalysePrix.avertissementSiEcart(Montant.de(391), reference, new BigDecimal("30")))
                .isPresent();
    }

    @Test
    void un_prix_de_reference_doit_etre_positif() {
        assertThat(new PrixMarche(Montant.de(300), LocalDate.now(), true).prixKg().francs())
                .isEqualTo(300);
        assertThat(TypeCulture.ARACHIDE.libelle()).isEqualTo("Arachide");
    }
}
