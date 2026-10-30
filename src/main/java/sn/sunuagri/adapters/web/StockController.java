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
import sn.sunuagri.adapters.web.dto.Reponses.LivraisonVue;
import sn.sunuagri.adapters.web.dto.Reponses.StockVue;
import sn.sunuagri.adapters.web.dto.Requetes;
import sn.sunuagri.application.StockService;
import sn.sunuagri.domaine.commun.Identite;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
@Tag(name = "Récoltes et stock", description = "US-08, US-09")
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    @PostMapping("/recoltes/{recolteId}/livraisons")
    @ResponseStatus(HttpStatus.CREATED)
    public LivraisonVue livrer(@Parameter(hidden = true) @UtilisateurCourant Identite identite, @PathVariable String recolteId,
                               @RequestBody Requetes.Livraison requete) {
        return LivraisonVue.de(stock.livrer(identite, new StockService.Livrer(recolteId,
                requete.quantiteKg(), requete.tauxHumiditePct(), requete.qualite())));
    }

    @GetMapping("/stock")
    public List<StockVue> consulter(@Parameter(hidden = true) @UtilisateurCourant Identite identite) {
        return stock.consulterStock(identite).stream()
                .map(l -> new StockVue(l.culture().libelle(), l.qualite().name(), l.total().kg(),
                        l.reserve().kg(), l.disponible().kg()))
                .toList();
    }
}
