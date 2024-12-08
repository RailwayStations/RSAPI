package org.railwaystations.rsapi.core.model

object CountryTestFixtures {
    fun createCountryList(countryCodes: Set<String>): Set<Country> {
        return countryCodes.map { createCountry(it) }.toSet()
    }

    fun createCountry(code: String): Country {
        val country = Country(
            code = code,
            name = "name-$code",
            _email = "email-$code",
            timetableUrlTemplate = "timetable-$code",
            overrideLicense = License.CC_BY_NC_40_INT,
            providerApps = listOf(
                createProviderApp("android", code),
                createProviderApp("ios", code),
                createProviderApp("web", code),
            )
        )
        return country
    }

    private fun createProviderApp(type: String, code: String): ProviderApp {
        return ProviderApp(type, "Provider-$code", type + "App-" + code)
    }

    val countryDe = Country(
        code = "de",
        name = "Deutschland",
        _email = "info@railway-stations.org",
        timetableUrlTemplate = "https://mobile.bahn.de/bin/mobil/bhftafel.exe/dox?bt=dep&max=10&rt=1&use_realtime_filter=1&start=yes&input={title}",
        overrideLicense = null,
        active = true,
        providerApps = listOf(
            ProviderApp(
                type = "android",
                name = "DB Navigator",
                url = "https://play.google.com/store/apps/details?id=de.hafas.android.db"
            ),
            ProviderApp(
                type = "android",
                name = "FlixTrain",
                url = "https://play.google.com/store/apps/details?id=de.meinfernbus"
            ),
            ProviderApp(
                type = "ios",
                name = "DB Navigator",
                url = "https://apps.apple.com/app/db-navigator/id343555245"
            )
        )
    )
    
}