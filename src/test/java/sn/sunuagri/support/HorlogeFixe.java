package sn.sunuagri.support;

import java.time.LocalDate;
import sn.sunuagri.application.port.HorlogePort;

public class HorlogeFixe implements HorlogePort {

    private LocalDate date;

    public HorlogeFixe(LocalDate date) {
        this.date = date;
    }

    public void changer(LocalDate nouvelleDate) {
        this.date = nouvelleDate;
    }

    @Override
    public LocalDate aujourdHui() {
        return date;
    }
}
