package sn.sunuagri.domaine.commercialisation;

import java.util.List;
import java.util.Optional;

/** Port sortant de persistance des paiements dus. */
public interface PaiementDuRepository {

    void sauvegarder(PaiementDu paiement);

    Optional<PaiementDu> parId(String id);

    List<PaiementDu> parCommande(String commandeId);

    List<PaiementDu> parProducteur(String producteurId);

    List<PaiementDu> tous();
}
