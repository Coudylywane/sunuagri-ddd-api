package sn.sunuagri.adapters.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrat OpenAPI (E5, E6) : identité simulée et format d'erreur unique
 * documentés de façon identique sur toutes les opérations.
 */
@Configuration
public class ConfigurationOpenApi {

    private static final Map<String, String> ERREURS = Map.of(
            "400", "Requête invalide (règle métier, paramètre ou corps mal formé)",
            "403", "Rôle insuffisant pour l'opération",
            "404", "Ressource introuvable, ou hors du périmètre de l'utilisateur",
            "409", "Conflit avec l'état actuel (doublon, transition interdite)",
            "500", "Erreur interne (aucun détail technique exposé)");

    @Bean
    public OpenAPI sunuAgriOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SunuAgri API")
                        .version("1.0.0")
                        .description("""
                                API de la coopérative agricole SunuAgri.
                                L'authentification est simulée : chaque requête porte les en-têtes
                                X-Utilisateur-Id (identifiant du producteur ou de l'acheteur selon le rôle)
                                et X-Role (PRODUCTEUR, ACHETEUR, AGENT, ADMINISTRATEUR).
                                Toutes les erreurs suivent le même format : code, message, details,
                                horodatage et chemin.
                                """))
                .components(new Components()
                        .addSchemas("ErreurApi", schemaErreur())
                        .addResponses("Erreur400", erreur("Requête invalide (règle métier)"))
                        .addResponses("Erreur403", erreur("Rôle insuffisant"))
                        .addResponses("Erreur404", erreur("Ressource introuvable"))
                        .addResponses("Erreur409", erreur("Conflit avec l'état actuel"))
                        .addResponses("Erreur500", erreur("Erreur interne")));
    }

    @Bean
    public OperationCustomizer contratsTransverses() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            if (operation.getParameters() == null) {
                operation.setParameters(new ArrayList<>());
            }
            operation.getParameters().add(new Parameter()
                    .in("header")
                    .name(UtilisateurCourantResolver.ENTETE_UTILISATEUR)
                    .required(true)
                    .description("Identifiant de l'utilisateur simulé")
                    .schema(new StringSchema().example("771234567")));
            operation.getParameters().add(new Parameter()
                    .in("header")
                    .name(UtilisateurCourantResolver.ENTETE_ROLE)
                    .required(true)
                    .description("Rôle simulé : PRODUCTEUR, ACHETEUR, AGENT ou ADMINISTRATEUR")
                    .schema(new StringSchema().example("AGENT")));
            ERREURS.forEach((code, description) -> operation.getResponses()
                    .addApiResponse(code, new ApiResponse()
                            .description(description)
                            .content(new Content().addMediaType("application/json",
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErreurApi"))))));
            return operation;
        };
    }

    private static Schema<?> schemaErreur() {
        return new ObjectSchema()
                .description("Format unique des erreurs de l'API")
                .addProperty("code", new StringSchema().example("REGLE_METIER"))
                .addProperty("message", new StringSchema()
                        .example("La quantité minimale d'une commande est de 50 kg"))
                .addProperty("details", new ArraySchema().items(new StringSchema()))
                .addProperty("horodatage", new StringSchema().format("date-time"))
                .addProperty("chemin", new StringSchema().example("/api/v1/commandes"))
                .required(java.util.List.of("code", "message", "horodatage", "chemin"));
    }

    private static ApiResponse erreur(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErreurApi"))));
    }
}
