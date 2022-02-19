package org.railwaystations.rsapi.adapter.web.resources;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.rsapi.adapter.db.CountryDao;
import org.railwaystations.rsapi.domain.model.Country;
import org.railwaystations.rsapi.domain.model.ProviderApp;
import org.railwaystations.rsapi.services.CountryService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class CountriesResourceTest {

    @Test
    public void testList() {
        final CountryDao countryDao = Mockito.mock(CountryDao.class);
        final Set<Country> countryList = new HashSet<>();

        final Country xy = new Country("xy", "nameXY", "emailXY", "twitterXY", "timetableXY", "overrideLicenseXY", true);
        xy.getProviderApps().add(new ProviderApp("android", "Provider XY", "providerAndroidAppXY"));
        xy.getProviderApps().add(new ProviderApp("ios", "Provider XY", "providerIosAppXY"));
        xy.getProviderApps().add(new ProviderApp("web", "Provider XY", "providerWebAppXY"));
        countryList.add(xy);

        final Country ab = new Country("ab", "nameAB", "emailAB", "twitterAB", "timetableAB", "overrideLicenseAB", true);
        ab.getProviderApps().add(new ProviderApp("android", "Provider AB", "providerAndroidAppAB"));
        ab.getProviderApps().add(new ProviderApp("ios", "Provider AB", "providerIosAppAB"));
        ab.getProviderApps().add(new ProviderApp("web", "Provider AB", "providerWebAppAB"));
        countryList.add(ab);
        Mockito.when(countryDao.list(true)).thenReturn(countryList);

        final CountriesResource resource = new CountriesResource(new CountryService(countryDao));

        final Collection<Country> countries = resource.list(null);
        assertThat(countries.size(), equalTo(2));
        countries.forEach(this::assertCountry);
    }

    private void assertCountry(final Country country) {
        assertThat(country.getName(), equalTo("name" + country.getCode().toUpperCase()));
        assertThat(country.getEmail(), equalTo("email" + country.getCode().toUpperCase()));
        assertThat(country.getTwitterTags(), equalTo("twitter" + country.getCode().toUpperCase()));
        assertThat(country.getTimetableUrlTemplate(), equalTo("timetable" + country.getCode().toUpperCase()));
        assertThat(country.getOverrideLicense(), equalTo("overrideLicense" + country.getCode().toUpperCase()));
        assertThat(country.getProviderApps().size(), equalTo(3));
        country.getProviderApps().forEach(app -> {
            switch (app.getType()) {
                case "android" -> assertThat(app.getUrl(), equalTo("providerAndroidApp" + country.getCode().toUpperCase()));
                case "ios" -> assertThat(app.getUrl(), equalTo("providerIosApp" + country.getCode().toUpperCase()));
                case "web" -> assertThat(app.getUrl(), equalTo("providerWebApp" + country.getCode().toUpperCase()));
                default -> fail("unknown app type");
            }
        });
    }

}
