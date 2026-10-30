package sn.sunuagri.domaine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Telephone;

class TelephoneTest {

    @Test
    void normalise_les_formats_courants() {
        assertThat(new Telephone("77 123 45 67").valeur()).isEqualTo("771234567");
        assertThat(new Telephone("+221771234567").valeur()).isEqualTo("771234567");
        assertThat(new Telephone("00221771234567").valeur()).isEqualTo("771234567");
    }

    @Test
    void refuse_un_numero_invalide() {
        assertThatThrownBy(() -> new Telephone("12345"))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("téléphone");
        assertThatThrownBy(() -> new Telephone(" "))
                .isInstanceOf(RegleMetierException.class);
    }
}
