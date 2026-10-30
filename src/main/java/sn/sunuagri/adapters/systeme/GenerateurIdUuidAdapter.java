package sn.sunuagri.adapters.systeme;

import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.sunuagri.application.port.IdGeneratorPort;

@Component
public class GenerateurIdUuidAdapter implements IdGeneratorPort {

    @Override
    public String nouvelId() {
        return UUID.randomUUID().toString();
    }
}
