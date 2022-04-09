package org.railwaystations.rsapi.adapter.in.web.writer;

import org.railwaystations.rsapi.core.model.Station;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StationsTxtWriter extends AbstractHttpMessageConverter<List<Station>> {

    public StationsTxtWriter() {
        super(MediaType.TEXT_PLAIN);
    }

    private static void stationToTxt(final PrintWriter pw, final Station station) {
        pw.println(String.join("\t", Double.toString(station.getCoordinates().lat()),
                Double.toString(station.getCoordinates().lon()), station.getTitle(), station.getTitle(),
                station.hasPhoto() ? "gruenpunkt.png" : "rotpunkt.png", "10,10", "0,-10"));
    }

    @Override
    protected boolean supports(@NonNull final Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected @NonNull List<Station> readInternal(@NonNull final Class<? extends List<Station>> clazz, @NonNull final HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("read not supported", inputMessage);
    }

    @Override
    protected void writeInternal(final List<Station> stations, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8))) {
            pw.println("lat	lon	title	description	icon	iconSize	iconOffset");
            stations.forEach(station -> stationToTxt(pw, station));
            pw.flush();
        }
    }

}
