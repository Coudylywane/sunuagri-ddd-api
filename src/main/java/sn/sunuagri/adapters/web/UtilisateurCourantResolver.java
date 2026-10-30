package sn.sunuagri.adapters.web;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import sn.sunuagri.domaine.commun.Identite;
import sn.sunuagri.domaine.commun.RegleMetierException;
import sn.sunuagri.domaine.commun.Role;

@Component
public class UtilisateurCourantResolver implements HandlerMethodArgumentResolver {

    public static final String ENTETE_UTILISATEUR = "X-Utilisateur-Id";
    public static final String ENTETE_ROLE = "X-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Identite.class)
                && parameter.hasParameterAnnotation(UtilisateurCourant.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String utilisateurId = webRequest.getHeader(ENTETE_UTILISATEUR);
        String role = webRequest.getHeader(ENTETE_ROLE);
        if (utilisateurId == null || utilisateurId.isBlank() || role == null || role.isBlank()) {
            throw new RegleMetierException(
                    "Les en-têtes " + ENTETE_UTILISATEUR + " et " + ENTETE_ROLE + " sont obligatoires");
        }
        return new Identite(utilisateurId, Role.depuis(role));
    }
}
