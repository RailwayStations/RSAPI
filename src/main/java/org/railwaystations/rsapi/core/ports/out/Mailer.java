package org.railwaystations.rsapi.core.ports.out;

public interface Mailer {

    void send(final String to, final String subject, final String text);

}
