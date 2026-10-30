package sn.sunuagri.adapters.web;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfigurationWeb implements WebMvcConfigurer {

    private final UtilisateurCourantResolver utilisateurCourantResolver;

    public ConfigurationWeb(UtilisateurCourantResolver utilisateurCourantResolver) {
        this.utilisateurCourantResolver = utilisateurCourantResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(utilisateurCourantResolver);
    }
}
