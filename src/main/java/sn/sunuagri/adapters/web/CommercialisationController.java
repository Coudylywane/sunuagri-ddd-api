package sn.sunuagri.adapters.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.sunuagri.adapters.web.dto.Reponses.CommandeVue;
import sn.sunuagri.adapters.web.dto.Reponses.LivraisonCommandeVue;
import sn.sunuagri.adapters.web.dto.Reponses.OffreVue;
import sn.sunuagri.adapters.web.dto.Reponses.PageCommandesVue;
import sn.sunuagri.adapters.web.dto.Reponses.PaiementVue;
import sn.sunuagri.adapters.web.dto.Reponses.PublicationOffreVue;
import sn.sunuagri.adapters.web.dto.Requetes;
import sn.sunuagri.application.CommercialisationService;
import sn.sunuagri.domaine.commercialisation.StatutCommande;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RegleMetierException;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
@Tag(name = "Offres, commandes et paiements", description = "US-10 à US-14, US-16, US-17")
public class CommercialisationController {

    private final CommercialisationService commercialisation;

    public CommercialisationController(CommercialisationService commercialisation) {
        this.commercialisation = commercialisation;
    }

    @PostMapping("/offres")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicationOffreVue publierOffre(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                            @RequestBody Requetes.PublierOffre requete) {
        CommercialisationService.ResultatPublication resultat =
                commercialisation.publierOffre(identite,
                        new CommercialisationService.PublierOffre(requete.culture(), requete.qualite(),
                                requete.quantiteKg(), requete.prixKg()));
        return new PublicationOffreVue(OffreVue.de(resultat.offre()), resultat.avertissementPrix());
    }

    @GetMapping("/offres")
    public List<OffreVue> offres(@Parameter(hidden = true) @UtilisateurCourant Identite identite) {
        return commercialisation.offresVisibles(identite).stream().map(OffreVue::de).toList();
    }

    @PostMapping("/offres/{offreId}/retrait")
    public OffreVue retirerOffre(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                 @PathVariable String offreId) {
        return OffreVue.de(commercialisation.retirerOffre(identite, offreId));
    }

    @PostMapping("/commandes")
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeVue passerCommande(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                      @RequestBody Requetes.PasserCommande requete) {
        return CommandeVue.de(commercialisation.passerCommande(identite,
                new CommercialisationService.PasserCommande(requete.offreId(), requete.quantiteKg())));
    }

    @PostMapping("/commandes/{commandeId}/annulation")
    public CommandeVue annulerCommande(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                       @PathVariable String commandeId) {
        return CommandeVue.de(commercialisation.annulerCommande(identite, commandeId));
    }

    @PostMapping("/commandes/{commandeId}/livraison")
    public LivraisonCommandeVue livrerCommande(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                               @PathVariable String commandeId) {
        CommercialisationService.ResultatLivraison resultat =
                commercialisation.livrerCommande(identite, commandeId);
        return new LivraisonCommandeVue(CommandeVue.de(resultat.commande()),
                resultat.commission().francs(), resultat.montantNet().francs(),
                resultat.paiements().stream().map(PaiementVue::de).toList());
    }

    @GetMapping("/commandes")
    public PageCommandesVue historique(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                       @RequestParam(required = false) String statut,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int taille) {
        CommercialisationService.PageCommandes resultat = commercialisation.historiqueCommandes(
                identite, statutDepuis(statut), page, taille);
        return new PageCommandesVue(resultat.commandes().stream().map(CommandeVue::de).toList(),
                resultat.total(), resultat.page(), resultat.taille());
    }

    @GetMapping("/commandes/{commandeId}/paiements")
    public List<PaiementVue> paiements(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                       @PathVariable String commandeId) {
        return commercialisation.paiementsDeLaCommande(identite, commandeId).stream()
                .map(PaiementVue::de).toList();
    }

    private StatutCommande statutDepuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return StatutCommande.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegleMetierException(
                    "Le statut doit être EN_ATTENTE, LIVREE ou ANNULEE");
        }
    }
}
