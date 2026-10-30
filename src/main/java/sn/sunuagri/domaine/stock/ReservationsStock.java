package sn.sunuagri.domaine.stock;

import sn.sunuagri.domaine.commun.Quantite;
import sn.sunuagri.domaine.culture.TypeCulture;

/**
 * Port de lecture des quantités réservées par les offres publiées.
 * Il évite au contexte Stock de dépendre directement du contexte Commercialisation.
 */
public interface ReservationsStock {

    Quantite quantiteReservee(TypeCulture typeCulture, Qualite qualite);
}
