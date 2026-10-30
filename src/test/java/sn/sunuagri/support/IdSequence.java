package sn.sunuagri.support;

import java.util.concurrent.atomic.AtomicInteger;
import sn.sunuagri.application.port.IdGeneratorPort;

public class IdSequence implements IdGeneratorPort {

    private final AtomicInteger compteur = new AtomicInteger();

    @Override
    public String nouvelId() {
        return String.format("id-%04d", compteur.incrementAndGet());
    }
}
