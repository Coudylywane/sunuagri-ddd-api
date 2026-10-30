package sn.sunuagri.domaine.commercialisation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sn.sunuagri.domaine.commun.Montant;
import sn.sunuagri.domaine.commun.RegleMetierException;

/**
 * Répartition d'un montant net entre producteurs, au prorata des kilogrammes
 * apportés dans les lots consommés.
 *
 * Règle d'arrondi (ADR-004) : chaque part est d'abord arrondie au franc inférieur,
 * puis les francs restants sont distribués un par un aux plus grands restes
 * (méthode des plus forts restes). La somme des parts est donc exactement égale
 * au montant net, sans perte ni création de franc.
 */
public final class RepartitionPaiement {

    private RepartitionPaiement() {
    }

    public record Part(String producteurId, BigDecimal quantiteKg, Montant montant) {
    }

    public static List<Part> calculer(Montant montantNet, Map<String, BigDecimal> kgParProducteur) {
        if (montantNet == null) {
            throw new RegleMetierException("Le montant à répartir est obligatoire");
        }
        if (kgParProducteur == null || kgParProducteur.isEmpty()) {
            throw new RegleMetierException("Aucun producteur à rémunérer");
        }
        BigDecimal totalKg = kgParProducteur.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalKg.signum() <= 0) {
            throw new RegleMetierException("Le total des quantités doit être strictement positif");
        }

        Map<String, BigDecimal> entrees = new LinkedHashMap<>();
        kgParProducteur.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> entrees.put(e.getKey(), e.getValue()));

        long net = montantNet.francs();
        List<Calcul> calculs = new ArrayList<>();
        long sommeBases = 0;
        for (Map.Entry<String, BigDecimal> entree : entrees.entrySet()) {
            BigDecimal numerateur = BigDecimal.valueOf(net).multiply(entree.getValue());
            long base = numerateur.divide(totalKg, 0, RoundingMode.FLOOR).longValueExact();
            BigDecimal reste = numerateur.subtract(BigDecimal.valueOf(base).multiply(totalKg));
            calculs.add(new Calcul(entree.getKey(), entree.getValue(), base, reste));
            sommeBases += base;
        }

        long aDistribuer = net - sommeBases;
        List<Calcul> parReste = new ArrayList<>(calculs);
        parReste.sort(Comparator
                .comparing(Calcul::reste).reversed()
                .thenComparing(Calcul::producteurId));
        for (int i = 0; i < aDistribuer; i++) {
            Calcul calcul = parReste.get(i);
            int index = calculs.indexOf(calcul);
            calculs.set(index, new Calcul(calcul.producteurId(), calcul.quantiteKg(),
                    calcul.base() + 1, calcul.reste()));
        }

        List<Part> parts = new ArrayList<>();
        for (Calcul calcul : calculs) {
            parts.add(new Part(calcul.producteurId(), calcul.quantiteKg(),
                    Montant.de(calcul.base())));
        }
        long controle = parts.stream().mapToLong(p -> p.montant().francs()).sum();
        if (controle != net) {
            throw new IllegalStateException("La répartition ne conserve pas le montant total");
        }
        return parts;
    }

    private record Calcul(String producteurId, BigDecimal quantiteKg, long base, BigDecimal reste) {
    }
}
