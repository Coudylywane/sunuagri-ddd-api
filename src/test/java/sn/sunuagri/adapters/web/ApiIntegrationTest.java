package sn.sunuagri.adapters.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger(770_000_001);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enregistre_un_producteur_et_expose_le_contrat_openapi() throws Exception {
        String telephone = String.valueOf(COMPTEUR.getAndIncrement());
        mockMvc.perform(post("/api/v1/producteurs")
                        .header("X-Utilisateur-Id", "agent-1")
                        .header("X-Role", "AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Diop","prenom":"Awa","telephone":"%s","village":"Thiès"}
                                """.formatted(telephone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.telephone").value(telephone));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/v1/producteurs']").exists());
    }

    @Test
    void une_erreur_suit_le_format_unique() throws Exception {
        mockMvc.perform(get("/api/v1/producteurs/inconnu/parcelles")
                        .header("X-Utilisateur-Id", "agent-1")
                        .header("X-Role", "AGENT"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"))
                .andExpect(jsonPath("$.message").value("Producteur introuvable"))
                .andExpect(jsonPath("$.horodatage").exists())
                .andExpect(jsonPath("$.chemin").value("/api/v1/producteurs/inconnu/parcelles"));
    }

    @Test
    void refuse_une_operation_sans_le_bon_role() throws Exception {
        mockMvc.perform(post("/api/v1/producteurs")
                        .header("X-Utilisateur-Id", "acheteur-1")
                        .header("X-Role", "ACHETEUR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Diop","prenom":"Awa","telephone":"779999999","village":"Thiès"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void exige_les_entetes_d_identite() throws Exception {
        mockMvc.perform(get("/api/v1/stock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REGLE_METIER"))
                .andExpect(jsonPath("$.message").value(
                        "Les en-têtes X-Utilisateur-Id et X-Role sont obligatoires"));
    }

    @Test
    void ne_revele_pas_la_ressource_d_un_autre_producteur() throws Exception {
        String telephone = String.valueOf(COMPTEUR.getAndIncrement());
        String corps = mockMvc.perform(post("/api/v1/producteurs")
                        .header("X-Utilisateur-Id", "agent-1")
                        .header("X-Role", "AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Fall","prenom":"Moussa","telephone":"%s","village":"Kaolack"}
                                """.formatted(telephone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String producteurId = com.jayway.jsonpath.JsonPath.read(corps, "$.id");

        mockMvc.perform(get("/api/v1/producteurs/" + producteurId + "/parcelles")
                        .header("X-Utilisateur-Id", "un-autre-producteur")
                        .header("X-Role", "PRODUCTEUR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"))
                .andExpect(jsonPath("$.message").value("Producteur introuvable"));
    }
}
