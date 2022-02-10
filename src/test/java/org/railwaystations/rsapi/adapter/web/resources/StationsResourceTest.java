package org.railwaystations.rsapi.adapter.web.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.rsapi.domain.model.Coordinates;
import org.railwaystations.rsapi.domain.model.Photo;
import org.railwaystations.rsapi.domain.model.Station;
import org.railwaystations.rsapi.domain.model.User;
import org.railwaystations.rsapi.services.PhotoStationsService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class StationsResourceTest {

    private StationsResource resource;

    @BeforeEach
    public void setUp() {
        final Station.Key key5 = new Station.Key("xy", "5");
        final Station stationXY = new Station(key5, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(key5, "/fotos/xy/5.jpg", createTestPhotographer("Jim Knopf", "photographerUrl", "CC0"), null, "CC0"), false);

        final Station.Key key3 = new Station.Key("ab", "3");
        final Station stationAB = new Station(key3, "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(key3, "/fotos/ab/3.jpg", createTestPhotographer("Peter Pan", "photographerUrl2", "CC0 by SA"), null, "CC0 by SA"), true);

        final List<Station> stationsAll = List.of(stationAB, stationXY);

        final PhotoStationsService repository = Mockito.mock(PhotoStationsService.class);
        when(repository.findStationsBy(Collections.singleton("xy"), null, null, null, null, null, null)).thenReturn(List.of(stationXY));
        when(repository.findStationsBy(Collections.singleton("ab"), null, null, null, null, null, null)).thenReturn(List.of(stationAB));
        when(repository.findStationsBy(null, null, null, null, null, null, null)).thenReturn(stationsAll);
        when(repository.findStationsBy(allCountries(), null, null, null, null, null, null)).thenReturn(stationsAll);
        when(repository.findByCountryAndId("ab", "3")).thenReturn(Optional.of(stationAB));

        resource = new StationsResource(repository);
    }

    private Set<String> allCountries() {
        return Set.of("ab", "xy");
    }

    @Test
    public void testGetXY() {
        final List<Station> resultXY = resource.get(Collections.singleton("xy"), null, null, null, null, null, null);
        final Station stationXY = resultXY.get(0);
        assertThat(stationXY, notNullValue());
        assertThat(stationXY.getKey(), equalTo(new Station.Key("xy", "5")));
        assertThat(stationXY.getTitle(), equalTo("Lummerland"));
        assertThat(stationXY.getCoordinates().getLat(), equalTo(50.0));
        assertThat(stationXY.getCoordinates().getLon(), equalTo(9.0));
        assertThat(stationXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(stationXY.getDS100(), equalTo("XYZ"));
        assertThat(stationXY.getPhotoUrl(), equalTo("/fotos/xy/5.jpg"));
        assertThat(stationXY.getLicense(), equalTo("CC0"));
        assertThat(stationXY.getPhotographerUrl(), equalTo("photographerUrl"));
        assertThat(stationXY.isActive(), equalTo(false));
    }

    @Test
    public void testGetXYWithFilterActive() {
        final List<Station> resultXY = resource.get(Collections.singleton("xy"), null, null, null, null, null, true);
        assertThat(resultXY.isEmpty(), equalTo(true));
    }

    @Test
    public void testGetAB() {
        final List<Station> resultAB = resource.get(Collections.singleton("ab"), null, null, null, null, null, null);
        final Station station = resultAB.get(0);
        assertNimmerland(station);
    }

    @Test
    public void testGetABXY() {
        final List<Station> resultAB = resource.get(allCountries(), null, null, null, null, null, null);
        assertThat(resultAB.size(), equalTo(2));
    }

    private void assertNimmerland(final Station station) {
        assertThat(station, notNullValue());
        assertThat(station.getKey(), equalTo(new Station.Key("ab", "3")));
        assertThat(station.getTitle(), equalTo("Nimmerland"));
        assertThat(station.getCoordinates().getLat(), equalTo(40.0));
        assertThat(station.getCoordinates().getLon(), equalTo(6.0));
        assertThat(station.getPhotographer(), equalTo("Peter Pan"));
        assertThat(station.getPhotoUrl(), equalTo("/fotos/ab/3.jpg"));
        assertThat(station.getDS100(), equalTo("ABC"));
        assertThat(station.getLicense(), equalTo("CC0 by SA"));
        assertThat(station.getPhotographerUrl(), equalTo("photographerUrl2"));
        assertThat(station.isActive(), equalTo(true));
    }

    @Test
    public void testGetById() {
        final Station station = resource.getById("ab", "3");
        assertNimmerland(station);
    }

    @Test
    public void testGetAll() {
        final List<Station> resultAll = resource.get(null, null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }

    private User createTestPhotographer(final String name, final String url, final String license) {
        return new User(name, url, license, 0, null, true, false, null, null, false, null, true);
    }
}
