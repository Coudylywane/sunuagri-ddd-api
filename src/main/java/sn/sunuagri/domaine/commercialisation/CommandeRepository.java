package sn.sunuagri.domaine.commercialisation;

import java.util.List;
import java.util.Optional;

/** Port sortant de persistance des commandes. */
public interface CommandeRepository {

    void sauvegarder(Commande commande);

    Optional<Commande> parId(String id);

    List<Commande> parAcheteur(String acheteurId);

    List<Commande> toutes();
}
