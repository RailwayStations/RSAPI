package org.railwaystations.rsapi.adapter.in.web.writer;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PhotographersTxtWriterTest {

    @Test
    public void test() throws IOException {
        final Map<String, Long> photographers = new HashMap<>();
        photographers.put("@foo", 10L);
        photographers.put("@bar", 5L);

        final var outputMessage = new MockHttpOutputMessage();
        new PhotographersTxtWriter().writeInternal(photographers, outputMessage);

        final var txt = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
        final var lines = txt.split("\n");
        assertThat(lines[0]).isEqualTo("count\tphotographer");
        assertThat(lines[1]).isEqualTo("10\t@foo");
        assertThat(lines[2]).isEqualTo("5\t@bar");
    }

}
