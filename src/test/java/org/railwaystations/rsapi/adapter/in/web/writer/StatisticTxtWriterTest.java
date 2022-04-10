package org.railwaystations.rsapi.adapter.in.web.writer;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.core.model.Statistic;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;


public class StatisticTxtWriterTest {

    @Test
    public void test() throws IOException {
        final var stat = new Statistic("de", 1500, 500, 20);


        final var outputMessage = new MockHttpOutputMessage();
        new StatisticTxtWriter().writeInternal(stat, outputMessage);

        final var txt = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
        final var lines = txt.split("\n");
        assertThat(lines[0]).isEqualTo("name\tvalue");
        assertThat(lines[1]).isEqualTo("total\t1500");
        assertThat(lines[2]).isEqualTo("withPhoto\t500");
        assertThat(lines[3]).isEqualTo("withoutPhoto\t1000");
        assertThat(lines[4]).isEqualTo("photographers\t20");
        assertThat(lines[5]).isEqualTo("countryCode\tde");
    }

}
