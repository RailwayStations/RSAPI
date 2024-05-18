package org.railwaystations.rsapi.core.model

object CountryTestFixtures {
    fun createCountryList(countryCodes: Set<String>): Set<Country> {
        return countryCodes.map { createCountry(it) }.toSet()
    }

    fun createCountry(code: String): Country {
        val country = Country(
            code = code,
            name = "name-$code",
            email = "email-$code",
            timetableUrlTemplate = "timetable-$code",
            overrideLicense = License.CC_BY_NC_40_INT,
        )
        country.providerApps.add(createProviderApp("android", code))
        country.providerApps.add(createProviderApp("ios", code))
        country.providerApps.add(createProviderApp("web", code))
        return country
    }

    private fun createProviderApp(type: String, code: String): ProviderApp {
        return ProviderApp(type, "Provider-$code", type + "App-" + code)
    }
}