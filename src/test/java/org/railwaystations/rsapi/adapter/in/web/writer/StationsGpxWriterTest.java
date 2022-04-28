package org.railwaystations.rsapi.adapter.in.web.writer;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.model.StationDto;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class StationsGpxWriterTest {

	@Test
	public void testWriteTo() throws IOException {
		final var stations = new ArrayList<StationDto>();
		stations.add(new StationDto().country("de").idStr("4711").title("Test").lat(50d).lon(9d));
		stations.add(new StationDto().country("de").idStr("4712").title("Foo").lat(51d).lon(8d));

		final var outputMessage = new MockHttpOutputMessage();
		new StationsGpxWriter().writeInternal(stations, outputMessage);

		final var gpx = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(gpx)
				.isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\">\n<wpt lat=\"50.0\" lon=\"9.0\"><name>Test</name></wpt>\n<wpt lat=\"51.0\" lon=\"8.0\"><name>Foo</name></wpt>\n</gpx>");
	}

}
