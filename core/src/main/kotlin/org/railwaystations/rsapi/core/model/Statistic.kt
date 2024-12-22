package org.railwaystations.rsapi.core.model

data class Statistic(
    val countryCode: String?,
    val total: Int,
    val withPhoto: Int,
    val photographers: Int
) {
    val withoutPhoto: Int
        get() = total - withPhoto

}
