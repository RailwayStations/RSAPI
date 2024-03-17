package org.railwaystations.rsapi.core.model

data class Station(
    val key: Key = Key("", "0"),
    val title: String,
    val coordinates: Coordinates = Coordinates(),
    val ds100: String? = null,
    val photos: List<Photo> = emptyList(),
    val active: Boolean = true,
) {

    val hasPhoto: Boolean
        get() = photos.isNotEmpty()

    fun appliesTo(photographer: String?): Boolean {
        if (photographer != null) {
            return photos.any { it.photographer.displayName == photographer }
        }
        return true
    }

    val primaryPhoto: Photo?
        get() = photos.firstOrNull { it.primary }

    data class Key(
        var country: String,
        var id: String,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Station) {
            return false
        }
        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

}
