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
import sn.sunuagri.domaine.commercialisation.StatutCommande;
import sn.sunuagri.domaine.commercialisation.StatutPaiement;
import sn.sunuagri.domaine.commercialisation.StatutSms;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.culture.Culture;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.producteur.Parcelle;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.stock.Livraison;
import sn.sunuagri.domaine.stock.Qualite;
import sn.sunuagri.support.ContexteTest;

/** Démonstration complète : du producteur enregistré au paiement dû. */
class ScenarioCompletTest {

    @Test
    void du_producteur_au_paiement_du() {
        ContexteTest ctx = new ContexteTest();
        Identite acheteur = ContexteTest.acheteur("acheteur-1");

        Producteur producteur = ctx.enregistrerProducteur("Awa", "Diop", "77 123 45 67", "Thiès");
        Parcelle parcelle = ctx.declarerParcelle(producteur, "Nord", "10", "Thiès");
        Campagne campagne = ctx.ouvrirCampagne(Saison.HIVERNAGE, 2026,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
        Culture culture = ctx.declarerCulture(campagne, parcelle, TypeCulture.ARACHIDE, "10");
        Recolte recolte = ctx.recolter(culture, LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 10, 5), "11000");

        Livraison premiere = ctx.livrer(recolte, "8000", "8", Qualite.A);
        ctx.livrer(recolte, "2000", "8", Qualite.A);
        assertThat(premiere.lotId()).isNotBlank();

        StockService.LigneStock stockInitial = ligne(ctx, TypeCulture.ARACHIDE, Qualite.A);
        assertThat(stockInitial.total().enKg()).isEqualByComparingTo("10000");
        assertThat(stockInitial.reserve().enKg()).isEqualByComparingTo("0");
        assertThat(stockInitial.disponible().enKg()).isEqualByComparingTo("10000");

        CommercialisationService.ResultatPublication publication =
                ctx.commercialisation.publierOffre(ContexteTest.AGENT,
                        new CommercialisationService.PublierOffre("Arachide", "A",
                                new BigDecimal("6000"), 350L));
        Offre offre = publication.offre();
        assertThat(publication.avertissementPrix()).isNull();

        StockService.LigneStock stockApresOffre = ligne(ctx, TypeCulture.ARACHIDE, Qualite.A);
        assertThat(stockApresOffre.reserve().enKg()).isEqualByComparingTo("6000");
        assertThat(stockApresOffre.disponible().enKg()).isEqualByComparingTo("4000");

        Commande commande = ctx.commander(acheteur, offre, "3000");
        assertThat(commande.montant().francs()).isEqualTo(1_050_000);
        assertThat(commande.statut()).isEqualTo(StatutCommande.EN_ATTENTE);

        CommercialisationService.ResultatLivraison resultat =
                ctx.commercialisation.livrerCommande(ContexteTest.AGENT, commande.id());

        assertThat(resultat.commission().francs()).isEqualTo(52_500);
        assertThat(resultat.montantNet().francs()).isEqualTo(997_500);
        assertThat(resultat.paiements()).hasSize(1);
        long sommePaiements = resultat.paiements().stream()
                .mapToLong(p -> p.montant().francs()).sum();
        assertThat(sommePaiements + resultat.commission().francs())
                .isEqualTo(commande.montant().francs());

        PaiementDu paiement = resultat.paiements().get(0);
        assertThat(paiement.producteurId()).isEqualTo(producteur.id());
        assertThat(paiement.montant().francs()).isEqualTo(997_500);
        assertThat(paiement.statut()).isEqualTo(StatutPaiement.A_PAYER);
        assertThat(paiement.statutSms()).isEqualTo(StatutSms.ENVOYE);
        assertThat(commande.statut()).isEqualTo(StatutCommande.LIVREE);
        assertThat(ctx.sms.messagesEnvoyes()).hasSize(1);
        assertThat(ctx.sms.messagesEnvoyes().get(0).contenu())
                .contains("997 500 FCFA")
                .contains("Awa Diop");

        StockService.LigneStock stockFinal = ligne(ctx, TypeCulture.ARACHIDE, Qualite.A);
        assertThat(stockFinal.total().enKg()).isEqualByComparingTo("7000");
        assertThat(stockFinal.reserve().enKg()).isEqualByComparingTo("3000");
        assertThat(stockFinal.disponible().enKg()).isEqualByComparingTo("4000");

        // FIFO : le lot de 8 000 kg est consommé en premier, il en reste 5 000.
        assertThat(ctx.lots.tous()).hasSize(2);
        assertThat(ctx.lots.tous().stream()
                .filter(l -> l.quantiteInitiale().enKg().compareTo(new BigDecimal("8000")) == 0)
                .findFirst().orElseThrow().quantiteRestante().enKg())
                .isEqualByComparingTo("5000");

        List<PaiementDu> paiementsProducteur = ctx.commercialisation
                .paiementsDuProducteur(ContexteTest.producteur(producteur), producteur.id());
        assertThat(paiementsProducteur).hasSize(1);

        // Un paiement n'est notifié qu'une seule fois.
        ctx.notifications.notifier(resultat.paiements());
        assertThat(ctx.sms.messagesEnvoyes()).hasSize(1);
    }

    private StockService.LigneStock ligne(ContexteTest ctx, TypeCulture type, Qualite qualite) {
        return ctx.stockService.consulterStock(ContexteTest.AGENT).stream()
                .filter(l -> l.culture() == type && l.qualite() == qualite)
                .findFirst()
                .orElseThrow();
    }
}
