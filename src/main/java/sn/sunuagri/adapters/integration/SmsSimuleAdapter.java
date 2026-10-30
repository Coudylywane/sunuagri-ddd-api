package sn.sunuagri.adapters.integration;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sn.sunuagri.application.port.SmsPort;
import sn.sunuagri.domaine.commun.Telephone;

/**
 * Simulation du service SMS : les messages sont conservés en mémoire et
 * journalisés. Aucun envoi réel n'est effectué (hors périmètre).
 */
@Component
public class SmsSimuleAdapter implements SmsPort {

    private static final Logger log = LoggerFactory.getLogger(SmsSimuleAdapter.class);

    public record MessageSms(LocalDateTime horodatage, String destinataire, String contenu) {
    }

    private final List<MessageSms> envoyes = new CopyOnWriteArrayList<>();
    private volatile boolean enPanne = false;

    @Override
    public void envoyer(Telephone destinataire, String message) {
        if (enPanne) {
            throw new IllegalStateException("Service SMS indisponible (simulation)");
        }
        MessageSms sms = new MessageSms(LocalDateTime.now(), destinataire.valeur(), message);
        envoyes.add(sms);
        log.info("[SMS SIMULE] {} -> {}", destinataire.valeur(), message);
    }

    public List<MessageSms> messagesEnvoyes() {
        return Collections.unmodifiableList(envoyes);
    }

    /** Utilisé par les tests et la démonstration pour simuler une panne. */
    public void simulerPanne(boolean panne) {
        this.enPanne = panne;
    }
}
