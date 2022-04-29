package org.railwaystations.rsapi.adapter.in.web.writer;

import org.railwaystations.rsapi.adapter.in.web.model.StatisticDto;
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

public class StatisticTxtWriter extends AbstractHttpMessageConverter<StatisticDto> {

    public StatisticTxtWriter() {
        super(MediaType.TEXT_PLAIN);
    }

    @Override
    protected boolean supports(@NonNull final Class<?> clazz) {
        return StatisticDto.class.isAssignableFrom(clazz);
    }

    @Override
    @NonNull
    protected StatisticDto readInternal(@NonNull final Class<? extends StatisticDto> clazz, @NonNull final HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("read not supported", inputMessage);
    }

    @Override
    protected void writeInternal(final StatisticDto statistic, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8))) {
            pw.println("name\tvalue");
            statisticToCsv(pw, "total", statistic.getTotal());
            statisticToCsv(pw, "withPhoto", statistic.getWithPhoto());
            statisticToCsv(pw, "withoutPhoto", statistic.getWithoutPhoto());
            statisticToCsv(pw, "photographers", statistic.getPhotographers());
            pw.println(String.format("countryCode\t%s", statistic.getCountryCode()));
            pw.flush();
        }
    }

    private static void statisticToCsv(final PrintWriter pw, final String name, final long value) {
        pw.println(String.format("%s\t%s", name, value));
    }

}
