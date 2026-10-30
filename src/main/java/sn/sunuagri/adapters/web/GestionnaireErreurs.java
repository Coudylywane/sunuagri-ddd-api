package sn.sunuagri.adapters.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sn.sunuagri.adapters.web.dto.Reponses.ErreurApiVue;
import sn.sunuagri.domaine.commun.AccesRefuseException;
import sn.sunuagri.domaine.commun.ConflitException;
import sn.sunuagri.domaine.commun.DomaineException;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.RessourceIntrouvableException;

/** Format unique d'erreur (E6) : aucun détail interne n'est exposé. */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurApiVue> introuvable(RessourceIntrouvableException e,
                                                    HttpServletRequest requete) {
        return reponse(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage(), null, requete);
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ErreurApiVue> conflit(ConflitException e, HttpServletRequest requete) {
        return reponse(HttpStatus.CONFLICT, e.getCode(), e.getMessage(), null, requete);
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ResponseEntity<ErreurApiVue> accesRefuse(AccesRefuseException e, HttpServletRequest requete) {
        return reponse(HttpStatus.FORBIDDEN, e.getCode(), e.getMessage(), null, requete);
    }

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<ErreurApiVue> regleMetier(RegleMetierException e,
                                                    HttpServletRequest requete) {
        return reponse(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage(), null, requete);
    }

    @ExceptionHandler(DomaineException.class)
    public ResponseEntity<ErreurApiVue> domaine(DomaineException e, HttpServletRequest requete) {
        return reponse(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage(), null, requete);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurApiVue> corpsIllisible(HttpMessageNotReadableException e,
                                                       HttpServletRequest requete) {
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE",
                "Le corps de la requête est illisible ou mal formé", null, requete);
    }

    @ExceptionHandler({MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErreurApiVue> requeteIncomplete(Exception e, HttpServletRequest requete) {
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE",
                "Un paramètre ou en-tête obligatoire est absent ou invalide", null, requete);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurApiVue> erreurInterne(Exception e, HttpServletRequest requete) {
        log.error("Erreur interne non gérée sur {}", requete.getRequestURI(), e);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur interne est survenue", null, requete);
    }

    private ResponseEntity<ErreurApiVue> reponse(HttpStatus statut, String code, String message,
                                                 List<String> details, HttpServletRequest requete) {
        ErreurApiVue erreur = new ErreurApiVue(code, message, details,
                OffsetDateTime.now(), requete.getRequestURI());
        return ResponseEntity.status(statut).body(erreur);
    }
}
