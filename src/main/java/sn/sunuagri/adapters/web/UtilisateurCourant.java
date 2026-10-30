package sn.sunuagri.adapters.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Injecte l'identité simulée lue dans les en-têtes X-Utilisateur-Id et X-Role. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UtilisateurCourant {
}
