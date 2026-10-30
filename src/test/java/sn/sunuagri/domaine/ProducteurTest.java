package sn.sunuagri.domaine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Superficie;
import sn.sunuagri.domaine.commun.Telephone;
import sn.sunuagri.domaine.producteur.Producteur;

class ProducteurTest {

    private Producteur producteur() {
        return new Producteur("p1", "Diop", "Awa", new Telephone("771234567"), "Thiès",
                LocalDate.of(2026, 1, 10));
    }

    @Test
    void declare_une_parcelle_et_calcule_le_total() {
        Producteur producteur = producteur();
        producteur.declarerParcelle("pa1", "Nord", new Superficie(new BigDecimal("12.5")), "Thiès",
                new Superficie(new BigDecimal("50")));
        producteur.declarerParcelle("pa2", "Sud", new Superficie(new BigDecimal("7.5")), "Thiès",
                new Superficie(new BigDecimal("50")));

        assertThat(producteur.parcelles()).hasSize(2);
        assertThat(producteur.totalSuperficieEnHectares()).isEqualByComparingTo("20.0");
    }

    @Test
    void refuse_deux_parcelles_du_meme_nom() {
        Producteur producteur = producteur();
        producteur.declarerParcelle("pa1", "Nord", new Superficie(new BigDecimal("5")), "Thiès",
                new Superficie(new BigDecimal("50")));

        assertThatThrownBy(() -> producteur.declarerParcelle("pa2", " nord ", new Superficie(new BigDecimal("5")),
                "Thiès", new Superficie(new BigDecimal("50"))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("déjà une parcelle");
    }

    @Test
    void refuse_un_total_superieur_a_50_hectares() {
        Producteur producteur = producteur();
        producteur.declarerParcelle("pa1", "Nord", new Superficie(new BigDecimal("45")), "Thiès",
                new Superficie(new BigDecimal("50")));

        assertThatThrownBy(() -> producteur.declarerParcelle("pa2", "Sud",
                new Superficie(new BigDecimal("6")), "Thiès", new Superficie(new BigDecimal("50"))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("50");
    }

    @Test
    void refuse_une_superficie_non_positive() {
        assertThatThrownBy(() -> new Superficie(BigDecimal.ZERO))
                .isInstanceOf(RegleMetierException.class);
        assertThatThrownBy(() -> new Superficie(new BigDecimal("-1")))
                .isInstanceOf(RegleMetierException.class);
    }

    @Test
    void exige_nom_prenom_et_village() {
        assertThatThrownBy(() -> new Producteur("p1", " ", "Awa", new Telephone("771234567"),
                "Thiès", LocalDate.now()))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("nom");
        assertThatThrownBy(() -> new Producteur("p1", "Diop", "", new Telephone("771234567"),
                "Thiès", LocalDate.now()))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("prénom");
        assertThatThrownBy(() -> new Producteur("p1", "Diop", "Awa", new Telephone("771234567"),
                null, LocalDate.now()))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("village");
    }
}
