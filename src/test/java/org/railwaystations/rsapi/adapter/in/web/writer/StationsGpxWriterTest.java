package org.railwaystations.rsapi.adapter.in.web.writer;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.Station;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class StationsGpxWriterTest {

	@Test
	public void testWriteTo() throws IOException {
		final var stations = new ArrayList<Station>();
		stations.add(new Station(new Station.Key("de", "4711"), "Test", new Coordinates(50d, 9d), null, null, true));
		stations.add(new Station(new Station.Key("de", "4712"), "Foo", new Coordinates(51d, 8d), null, null, true));

		final var outputMessage = new MockHttpOutputMessage();
		new StationsGpxWriter().writeInternal(stations, outputMessage);

		final var gpx = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(gpx)
				.isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\">\n<wpt lat=\"50.0\" lon=\"9.0\"><name>Test</name></wpt>\n<wpt lat=\"51.0\" lon=\"8.0\"><name>Foo</name></wpt>\n</gpx>");
	}

}
