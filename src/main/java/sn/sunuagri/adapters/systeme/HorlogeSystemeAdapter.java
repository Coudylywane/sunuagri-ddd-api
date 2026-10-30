package sn.sunuagri.adapters.systeme;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import sn.sunuagri.application.port.HorlogePort;

@Component
public class HorlogeSystemeAdapter implements HorlogePort {

    @Override
    public LocalDate aujourdHui() {
        return LocalDate.now();
    }
}
