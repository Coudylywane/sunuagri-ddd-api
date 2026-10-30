package sn.sunuagri.domaine.commercialisation;

import java.time.LocalDate;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.commun.RegleMetierException;

/** Paiement dû à un producteur après une livraison de commande. */
public class PaiementDu {

    private final String id;
    private final String commandeId;
    private final String producteurId;
    private final Montant montant;
    private final LocalDate dateCreation;
    private final StatutPaiement statut = StatutPaiement.A_PAYER;
    private StatutSms statutSms = StatutSms.NON_ENVOYE;

    public PaiementDu(String id, String commandeId, String producteurId, Montant montant,
                      LocalDate dateCreation) {
        if (id == null || id.isBlank()) {
            throw new RegleMetierException("L'identifiant du paiement est obligatoire");
        }
        if (commandeId == null || commandeId.isBlank()) {
            throw new RegleMetierException("La commande du paiement est obligatoire");
        }
        if (producteurId == null || producteurId.isBlank()) {
            throw new RegleMetierException("Le producteur du paiement est obligatoire");
        }
        if (montant == null) {
            throw new RegleMetierException("Le montant du paiement est obligatoire");
        }
        if (dateCreation == null) {
            throw new RegleMetierException("La date du paiement est obligatoire");
        }
        this.id = id;
        this.commandeId = commandeId;
        this.producteurId = producteurId;
        this.montant = montant;
        this.dateCreation = dateCreation;
    }

    public boolean smsDejaTraite() {
        return statutSms != StatutSms.NON_ENVOYE;
    }

    public void marquerSmsEnvoye() {
        if (statutSms == StatutSms.ENVOYE) {
            throw new RegleMetierException("Le SMS de ce paiement a déjà été envoyé");
        }
        statutSms = StatutSms.ENVOYE;
    }

    public void marquerSmsEchec() {
        statutSms = StatutSms.ECHEC;
    }

    public String id() {
        return id;
    }

    public String commandeId() {
        return commandeId;
    }

    public String producteurId() {
        return producteurId;
    }

    public Montant montant() {
        return montant;
    }

    public LocalDate dateCreation() {
        return dateCreation;
    }

    public StatutPaiement statut() {
        return statut;
    }

    public StatutSms statutSms() {
        return statutSms;
    }
}
