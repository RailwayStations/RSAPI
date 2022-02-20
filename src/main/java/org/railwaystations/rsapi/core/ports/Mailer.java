package org.railwaystations.rsapi.core.ports;

public interface Mailer {

    void send(final String to, final String subject, final String text);

}
