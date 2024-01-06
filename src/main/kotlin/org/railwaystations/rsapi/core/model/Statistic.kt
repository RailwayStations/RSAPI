package org.railwaystations.rsapi.core.model

data class Statistic(
    val countryCode: String?,
    val total: Long,
    val withPhoto: Long,
    val photographers: Long
) {
    fun withoutPhoto(): Long {
        return total - withPhoto
    }
}
