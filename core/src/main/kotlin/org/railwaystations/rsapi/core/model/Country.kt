package org.railwaystations.rsapi.core.model

data class Country(
    val code: String,
    val name: String,
    private val _email: String? = null,
    val timetableUrlTemplate: String? = null,
    val overrideLicense: License? = null,
    val active: Boolean = false,
    val providerApps: List<ProviderApp> = listOf(),
) {
    val email: String
        get() = _email ?: "info@railway-stations.org"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Country

        return code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}

