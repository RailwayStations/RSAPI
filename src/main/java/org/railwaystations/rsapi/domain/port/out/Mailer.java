package org.railwaystations.rsapi.domain.port.out;

public interface Mailer {

    void send(final String to, final String subject, final String text);

}
