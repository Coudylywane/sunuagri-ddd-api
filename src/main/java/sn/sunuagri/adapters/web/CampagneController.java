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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.sunuagri.adapters.web.dto.Reponses.CampagneVue;
import sn.sunuagri.adapters.web.dto.Reponses.CultureVue;
import sn.sunuagri.adapters.web.dto.Reponses.RecolteVue;
import sn.sunuagri.adapters.web.dto.Requetes;
import sn.sunuagri.application.CampagneService;
import sn.sunuagri.domaine.commun.Identite;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
@Tag(name = "Campagnes, cultures et récoltes", description = "US-04, US-05, US-06, US-07")
public class CampagneController {

    private final CampagneService campagnes;

    public CampagneController(CampagneService campagnes) {
        this.campagnes = campagnes;
    }

    @PostMapping("/campagnes")
    @ResponseStatus(HttpStatus.CREATED)
    public CampagneVue ouvrir(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                              @RequestBody Requetes.OuvrirCampagne requete) {
        return CampagneVue.de(campagnes.ouvrir(identite, new CampagneService.OuvrirCampagne(
                requete.saison(), requete.annee(), requete.dateDebut(), requete.dateFin())));
    }

    @GetMapping("/campagnes")
    public List<CampagneVue> lister(@Parameter(hidden = true) @UtilisateurCourant Identite identite) {
        return campagnes.toutes(identite).stream().map(CampagneVue::de).toList();
    }

    @PostMapping("/campagnes/{campagneId}/cloture")
    public CampagneVue cloturer(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                @PathVariable String campagneId) {
        return CampagneVue.de(campagnes.cloturer(identite, campagneId));
    }

    @GetMapping("/campagnes/{campagneId}/cultures")
    public List<CultureVue> culturesDeLaCampagne(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                                 @PathVariable String campagneId) {
        return campagnes.culturesDeLaCampagne(identite, campagneId).stream()
                .map(CultureVue::de).toList();
    }

    @PostMapping("/cultures")
    @ResponseStatus(HttpStatus.CREATED)
    public CultureVue declarerCulture(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                      @RequestBody Requetes.DeclarerCulture requete) {
        return CultureVue.de(campagnes.declarerCulture(identite,
                new CampagneService.DeclarerCulture(requete.campagneId(), requete.parcelleId(),
                        requete.culture(), requete.superficieHa())));
    }

    @PostMapping("/cultures/{cultureId}/semis")
    public CultureVue semer(@Parameter(hidden = true) @UtilisateurCourant Identite identite, @PathVariable String cultureId,
                            @RequestBody Requetes.Semis requete) {
        return CultureVue.de(campagnes.semer(identite, cultureId, requete.dateSemis()));
    }

    @PostMapping("/cultures/{cultureId}/recolte")
    @ResponseStatus(HttpStatus.CREATED)
    public RecolteVue recolter(@Parameter(hidden = true) @UtilisateurCourant Identite identite, @PathVariable String cultureId,
                               @RequestBody Requetes.Recolte requete) {
        return RecolteVue.de(campagnes.recolter(identite, cultureId,
                new CampagneService.Recolter(requete.dateRecolte(), requete.quantiteKg())));
    }
}
