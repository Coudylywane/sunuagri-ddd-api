package sn.sunuagri.application.port;

import sn.sunuagri.domaine.commun.Telephone;

/** Port sortant d'envoi de SMS. Une simulation suffit pour ce projet. */
public interface SmsPort {

    void envoyer(Telephone destinataire, String message);
}
