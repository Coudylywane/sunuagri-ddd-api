package sn.sunuagri.domaine.stock;

import java.util.List;
import java.util.Optional;

/** Port sortant de persistance des livraisons. */
public interface LivraisonRepository {

    void sauvegarder(Livraison livraison);

    Optional<Livraison> parId(String id);

    List<Livraison> parRecolte(String recolteId);

    List<Livraison> toutes();
}
