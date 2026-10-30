package sn.sunuagri.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.Saison;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.PaiementDu;
import sn.sunuagri.domaine.commercialisation.StatutSms;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

class PaiementsTest {

    @Test
    void repartit_entre_plusieurs_producteurs_selon_les_lots_consommes_en_fifo() {
        ContexteTest ctx = new ContexteTest();

        // Premier producteur, premier lot (le plus ancien).
        Producteur awa = ctx.enregistrerProducteur("Awa", "Diop", "771111111", "Thiès");
        Parcelle parcelleAwa = ctx.declarerParcelle(awa, "Nord", "10", "Thiès");
        Campagne campagne2025 = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2025,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 11, 30));
        Culture cultureAwa = ctx.declarerCulture(campagne2025, parcelleAwa, TypeCulture.ARACHIDE, "10");
        Recolte recolteAwa = ctx.recolter(cultureAwa, LocalDate.of(2025, 6, 5),
                LocalDate.of(2025, 10, 5), "4000");
        ctx.horloge.changer(LocalDate.of(2025, 10, 10));
        ctx.livrer(recolteAwa, "4000", "8", Qualite.A);

        // Second producteur, lot plus récent.
        ctx.horloge.changer(LocalDate.of(2026, 6, 2));
        Producteur moussa = ctx.enregistrerProducteur("Moussa", "Fall", "772222222", "Kaolack");
        Parcelle parcelleMoussa = ctx.declarerParcelle(moussa, "Sud", "10", "Kaolack");
        Campagne campagne2026 = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture cultureMoussa = ctx.declarerCulture(campagne2026, parcelleMoussa,
                TypeCulture.ARACHIDE, "10");
        Recolte recolteMoussa = ctx.recolter(cultureMoussa, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "4000");
        ctx.livrer(recolteMoussa, "4000", "8", Qualite.A);

        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "6000", 300);
        Commande commande = ctx.commander(ContexteTest.acheteur("acheteur-1"), offre, "5000");
        CommercialisationService.ResultatLivraison resultat =
                ctx.commercialisation.livrerCommande(ContexteTest.AGENT, commande.id());

        // 5 000 kg consommés : 4 000 chez Awa, 1 000 chez Moussa.
        assertThat(resultat.commission().francs()).isEqualTo(75_000);
        assertThat(resultat.montantNet().francs()).isEqualTo(1_425_000);
        assertThat(resultat.paiements()).hasSize(2);

        PaiementDu paiementAwa = resultat.paiements().stream()
                .filter(p -> p.producteurId().equals(awa.id())).findFirst().orElseThrow();
        PaiementDu paiementMoussa = resultat.paiements().stream()
                .filter(p -> p.producteurId().equals(moussa.id())).findFirst().orElseThrow();
        assertThat(paiementAwa.montant().francs()).isEqualTo(1_140_000);
        assertThat(paiementMoussa.montant().francs()).isEqualTo(285_000);
        assertThat(paiementAwa.montant().francs() + paiementMoussa.montant().francs()
                + resultat.commission().francs()).isEqualTo(commande.montant().francs());

        assertThat(ctx.lots.tous().stream()
                .filter(l -> l.producteurId().equals(awa.id())).findFirst().orElseThrow()
                .quantiteRestante().enKg()).isEqualByComparingTo("0");
        assertThat(ctx.lots.tous().stream()
                .filter(l -> l.producteurId().equals(moussa.id())).findFirst().orElseThrow()
                .quantiteRestante().enKg()).isEqualByComparingTo("3000");

        assertThat(ctx.sms.messagesEnvoyes()).hasSize(2);
        assertThat(ctx.sms.messagesEnvoyes()).extracting(m -> m.destinataire())
                .containsExactlyInAnyOrder("771111111", "772222222");

        StockService.LigneStock stock = ctx.stockService.consulterStock(ContexteTest.AGENT).get(0);
        assertThat(stock.total().enKg()).isEqualByComparingTo("3000");
        assertThat(stock.reserve().enKg()).isEqualByComparingTo("1000");
        assertThat(stock.disponible().enKg()).isEqualByComparingTo("2000");
    }

    @Test
    void un_echec_de_sms_est_trace_sans_invalider_la_livraison() {
        ContexteTest ctx = new ContexteTest();
        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "771234567", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "2");
        Recolte recolte = ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "2000");
        ctx.livrer(recolte, "2000", "8", Qualite.A);
        Offre offre = ctx.publierOffre(TypeCulture.ARACHIDE, Qualite.A, "1000", 300);
        Commande commande = ctx.commander(ContexteTest.acheteur("acheteur-1"), offre, "1000");

        ctx.sms.simulerPanne(true);
        CommercialisationService.ResultatLivraison resultat =
                ctx.commercialisation.livrerCommande(ContexteTest.AGENT, commande.id());

        assertThat(commande.statut()).isEqualTo(sn.sunuagri.domaine.commercialisation.StatutCommande.LIVREE);
        assertThat(resultat.paiements()).hasSize(1);
        PaiementDu paiement = resultat.paiements().get(0);
        assertThat(paiement.statutSms()).isEqualTo(StatutSms.ECHEC);
        assertThat(ctx.sms.messagesEnvoyes()).isEmpty();

        // Un paiement déjà traité (même en échec) n'est jamais renvoyé automatiquement.
        ctx.sms.simulerPanne(false);
        ctx.notifications.notifier(List.of(paiement));
        assertThat(paiement.statutSms()).isEqualTo(StatutSms.ECHEC);
        assertThat(ctx.sms.messagesEnvoyes()).isEmpty();
    }
}
