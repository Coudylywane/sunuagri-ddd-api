package sn.sunuagri.adapters.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sunuagri.adapters.web.dto.Reponses.ParametreCultureVue;
import sn.sunuagri.adapters.web.dto.Reponses.PrixMarcheVue;
import sn.sunuagri.adapters.web.dto.Requetes;
import sn.sunuagri.application.ParametreService;
import sn.sunuagri.domaine.commun.Identite;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
@Tag(name = "Paramètres et prix du marché", description = "US-18, US-19")
public class ParametreController {

    private final ParametreService parametres;

    public ParametreController(ParametreService parametres) {
        this.parametres = parametres;
    }

    @GetMapping("/parametres/cultures")
    public List<ParametreCultureVue> lister(@Parameter(hidden = true) @UtilisateurCourant Identite identite) {
        return parametres.lister(identite).stream().map(ParametreCultureVue::de).toList();
    }

    @PutMapping("/parametres/cultures/{culture}")
    public ParametreCultureVue modifier(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                        @PathVariable String culture,
                                        @RequestBody Requetes.ModifierParametre requete) {
        return ParametreCultureVue.de(parametres.modifier(identite, culture,
                new ParametreService.ModifierParametre(requete.rendementReferenceKgHa(),
                        requete.humiditeMaxPct(), requete.cycleMinJours())));
    }

    @GetMapping("/prix-marche/{culture}")
    public PrixMarcheVue prix(@Parameter(hidden = true) @UtilisateurCourant Identite identite, @PathVariable String culture) {
        return PrixMarcheVue.de(culture, parametres.prixDeReference(identite, culture));
    }
}
