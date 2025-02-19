package org.railwaystations.rsapi.core.model

import org.railwaystations.rsapi.core.model.License.UNKNOWN
import org.railwaystations.rsapi.core.model.License.entries

enum class License(@JvmField val displayName: String, val url: String?) {
    CC_BY_30("CC BY 3.0", "https://creativecommons.org/licenses/by/3.0/"),
    CC_BY_NC_40_INT("CC BY-NC 4.0 International", "https://creativecommons.org/licenses/by-nc/4.0/"),
    CC_BY_NC_SA_30_DE("CC BY-NC-SA 3.0 DE", "https://creativecommons.org/licenses/by-nc-sa/3.0/de/"),
    CC_BY_SA_40("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"),
    CC0_10("CC0 1.0 Universell (CC0 1.0)", "https://creativecommons.org/publicdomain/zero/1.0/"),
    UNKNOWN("Unknown License", null);

}

fun String?.nameToLicense() = entries.toTypedArray().firstOrNull { license -> license.name == this } ?: UNKNOWN

fun String?.displayNameToLicense() = entries.toTypedArray()
    .firstOrNull { license -> license.displayName == this } ?: UNKNOWN
