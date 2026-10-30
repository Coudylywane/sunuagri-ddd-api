package sn.sunuagri.application;

import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.sunuagri.application.port.SmsPort;
import sn.sunuagri.domaine.commercialisation.PaiementDu;
import sn.sunuagri.domaine.commercialisation.PaiementDuRepository;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.producteur.ProducteurRepository;

/**
 * US-14 : notifie chaque paiement dû par SMS, au plus une fois.
 * Un échec d'envoi ne remet pas en cause la livraison ; il est tracé sur le
 * paiement (statut ECHEC) et dans les journaux.
 */
@Service
public class NotificationPaiementService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPaiementService.class);

    private final SmsPort sms;
    private final ProducteurRepository producteurs;
    private final PaiementDuRepository paiements;

    public NotificationPaiementService(SmsPort sms, ProducteurRepository producteurs,
                                       PaiementDuRepository paiements) {
        this.sms = sms;
        this.producteurs = producteurs;
        this.paiements = paiements;
    }

    public void notifier(List<PaiementDu> aNotifier) {
        for (PaiementDu paiement : aNotifier) {
            if (paiement.smsDejaTraite()) {
                continue;
            }
            try {
                Producteur producteur = producteurs.parId(paiement.producteurId())
                        .orElseThrow(() -> new IllegalStateException("Producteur du paiement introuvable"));
                String message = "Bonjour " + producteur.nomComplet()
                        + ", un paiement de " + formaterFrancs(paiement.montant().francs())
                        + " FCFA vous est dû par la coopérative SunuAgri pour la commande "
                        + paiement.commandeId() + ".";
                sms.envoyer(producteur.telephone(), message);
                paiement.marquerSmsEnvoye();
            } catch (RuntimeException e) {
                paiement.marquerSmsEchec();
                log.warn("Échec de l'envoi du SMS pour le paiement {} : {}",
                        paiement.id(), e.getMessage());
            }
            paiements.sauvegarder(paiement);
        }
    }

    private static String formaterFrancs(long montant) {
        return String.format(Locale.FRANCE, "%,d", montant).replace('\u202F', ' ').replace('\u00A0', ' ');
    }
}
