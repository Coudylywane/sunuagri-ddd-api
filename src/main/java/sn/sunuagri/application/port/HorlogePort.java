package sn.sunuagri.application.port;

import java.time.LocalDate;

/** Port sortant de lecture de la date du jour (permet des tests déterministes). */
public interface HorlogePort {

    LocalDate aujourdHui();
}
