package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import sn.sunuagri.support.ContexteTest;

class ImportCsvTest {

    @Test
    void enregistre_les_lignes_valides_et_rapporte_les_invalides() {
        ContexteTest ctx = new ContexteTest();
        String csv = """
                nom;prenom;telephone;village
                Diop;Awa;771111111;Thiès
                Fall;Moussa;771111111;Kaolack
                Ndiaye;;772222222;Dakar
                Sarr;Fatou;773333333;
                """;

        ProducteurService.ResultatImport resultat = ctx.producteurService.importerCsv(ContexteTest.AGENT, csv);

        assertThat(resultat.acceptes()).isEqualTo(1);
        assertThat(resultat.refuses()).isEqualTo(3);
        assertThat(resultat.erreurs()).extracting(ProducteurService.ErreurImport::ligne)
                .containsExactly(3, 4, 5);
        assertThat(resultat.erreurs().get(0).motif()).contains("déjà enregistré");
        assertThat(resultat.erreurs().get(1).motif()).contains("prénom");
        assertThat(resultat.erreurs().get(2).motif()).contains("village");
        assertThat(ctx.producteurs.tous()).hasSize(1);
    }

    @Test
    void accepte_un_fichier_sans_entete_et_les_numeros_bom() {
        ContexteTest ctx = new ContexteTest();
        String csv = "\uFEFFDiop;Awa;771111111;Thiès\r\nFall;Moussa;772222222;Kaolack\r\n";

        ProducteurService.ResultatImport resultat = ctx.producteurService.importerCsv(ContexteTest.AGENT, csv);

        assertThat(resultat.acceptes()).isEqualTo(2);
        assertThat(resultat.refuses()).isZero();
        assertThat(ctx.producteurs.tous()).hasSize(2);
    }

    @Test
    void une_ligne_mal_formee_est_signalee_sans_bloquer_le_reste() {
        ContexteTest ctx = new ContexteTest();
        String csv = "Diop;Awa;771111111\nFall;Moussa;772222222;Kaolack\n";

        ProducteurService.ResultatImport resultat = ctx.producteurService.importerCsv(ContexteTest.AGENT, csv);

        assertThat(resultat.acceptes()).isEqualTo(1);
        assertThat(resultat.refuses()).isEqualTo(1);
        assertThat(resultat.erreurs().get(0).ligne()).isEqualTo(1);
        assertThat(resultat.erreurs().get(0).motif()).contains("4 champs");
    }

    @Test
    void refuser_l_import_pour_un_role_non_agent() {
        ContexteTest ctx = new ContexteTest();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ctx.producteurService.importerCsv(
                        ContexteTest.acheteur("acheteur-1"), "Diop;Awa;771111111;Thiès"))
                .isInstanceOf(sn.sunuagri.domaine.commun.AccesRefuseException.class);
        assertThat(BigDecimal.ONE).isPositive();
    }
}
