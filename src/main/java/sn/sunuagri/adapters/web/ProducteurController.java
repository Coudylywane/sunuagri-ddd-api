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
import sn.sunuagri.adapters.web.dto.Reponses.CultureVue;
import sn.sunuagri.adapters.web.dto.Reponses.ImportVue;
import sn.sunuagri.adapters.web.dto.Reponses.PaiementVue;
import sn.sunuagri.adapters.web.dto.Reponses.ParcelleVue;
import sn.sunuagri.adapters.web.dto.Reponses.ProducteurVue;
import sn.sunuagri.adapters.web.dto.Reponses.RecolteVue;
import sn.sunuagri.adapters.web.dto.Requetes;
import sn.sunuagri.application.CampagneService;
import sn.sunuagri.application.CommercialisationService;
import sn.sunuagri.application.ProducteurService;
import sn.sunuagri.domaine.commun.Identite;

@RestController
@RequestMapping(value = "/api/v1/producteurs", produces = "application/json")
@Tag(name = "Producteurs et parcelles", description = "US-01, US-02, US-03, US-15, US-17")
public class ProducteurController {

    private final ProducteurService producteurs;
    private final CampagneService campagnes;
    private final CommercialisationService commercialisation;

    public ProducteurController(ProducteurService producteurs, CampagneService campagnes,
                                CommercialisationService commercialisation) {
        this.producteurs = producteurs;
        this.campagnes = campagnes;
        this.commercialisation = commercialisation;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProducteurVue enregistrer(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                     @RequestBody Requetes.EnregistrerProducteur requete) {
        return ProducteurVue.de(producteurs.enregistrer(identite,
                new ProducteurService.EnregistrerProducteur(requete.nom(), requete.prenom(),
                        requete.telephone(), requete.village())));
    }

    @GetMapping
    public List<ProducteurVue> lister(@Parameter(hidden = true) @UtilisateurCourant Identite identite) {
        return producteurs.tous(identite).stream().map(ProducteurVue::de).toList();
    }

    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain"})
    public ImportVue importer(@Parameter(hidden = true) @UtilisateurCourant Identite identite, @RequestBody String contenu) {
        ProducteurService.ResultatImport resultat = producteurs.importerCsv(identite, contenu);
        return new ImportVue(resultat.acceptes(), resultat.refuses(),
                resultat.erreurs().stream()
                        .map(e -> new sn.sunuagri.adapters.web.dto.Reponses.ErreurImportVue(
                                e.ligne(), e.motif()))
                        .toList());
    }

    @PostMapping("/{producteurId}/parcelles")
    @ResponseStatus(HttpStatus.CREATED)
    public ParcelleVue declarerParcelle(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                        @PathVariable String producteurId,
                                        @RequestBody Requetes.DeclarerParcelle requete) {
        return ParcelleVue.de(producteurs.declarerParcelle(identite, producteurId,
                new ProducteurService.DeclarerParcelle(requete.nom(), requete.superficieHa(),
                        requete.commune())));
    }

    @GetMapping("/{producteurId}/parcelles")
    public List<ParcelleVue> parcelles(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                       @PathVariable String producteurId) {
        return producteurs.parcelles(identite, producteurId).stream().map(ParcelleVue::de).toList();
    }

    @GetMapping("/{producteurId}/parcelles/{parcelleId}")
    public ParcelleVue parcelle(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                @PathVariable String producteurId,
                                @PathVariable String parcelleId) {
        return ParcelleVue.de(producteurs.parcelle(identite, producteurId, parcelleId));
    }

    @GetMapping("/{producteurId}/cultures")
    public List<CultureVue> cultures(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                     @PathVariable String producteurId) {
        return campagnes.culturesDuProducteur(identite, producteurId).stream()
                .map(CultureVue::de).toList();
    }

    @GetMapping("/{producteurId}/recoltes")
    public List<RecolteVue> recoltes(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                     @PathVariable String producteurId) {
        return campagnes.recoltesDuProducteur(identite, producteurId).stream()
                .map(RecolteVue::de).toList();
    }

    @GetMapping("/{producteurId}/paiements")
    public List<PaiementVue> paiements(@Parameter(hidden = true) @UtilisateurCourant Identite identite,
                                       @PathVariable String producteurId) {
        return commercialisation.paiementsDuProducteur(identite, producteurId).stream()
                .map(PaiementVue::de).toList();
    }
}
