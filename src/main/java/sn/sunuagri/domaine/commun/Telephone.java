package sn.sunuagri.domaine.commun;

import java.util.regex.Pattern;

/**
 * Numéro de téléphone sénégalais, normalisé sur 9 chiffres (7XXXXXXXX).
 * Les formats 77 123 45 67, +221771234567 et 00221771234567 sont acceptés.
 * L'unicité est garantie par le ProducteurRepository.
 */
public record Telephone(String valeur) {

    private static final Pattern FORMAT = Pattern.compile("7\\d{8}");

    public Telephone {
        String chiffres = valeur == null ? "" : valeur.replaceAll("\\D", "");
        if (chiffres.startsWith("00221")) {
            chiffres = chiffres.substring(5);
        } else if (chiffres.startsWith("221") && chiffres.length() == 12) {
            chiffres = chiffres.substring(3);
        }
        if (!FORMAT.matcher(chiffres).matches()) {
            throw new RegleMetierException(
                    "Le téléphone doit être un numéro sénégalais valide (ex. 771234567)");
        }
        valeur = chiffres;
    }

    @Override
    public String toString() {
        return valeur;
    }
}
