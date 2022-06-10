package org.railwaystations.rsapi.core.ports.out;

public interface Mailer {

    void send(String to, String subject, String text);

}
