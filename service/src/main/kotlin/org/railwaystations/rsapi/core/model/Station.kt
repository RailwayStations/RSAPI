package org.railwaystations.rsapi.core.model

data class Station(
    var key: Key = Key("", "0"),
    var title: String,
    var coordinates: Coordinates = Coordinates(),
    var ds100: String? = null,
    var photos: MutableList<Photo> = mutableListOf(),
    var active: Boolean = true,
) {

    fun hasPhoto(): Boolean {
        return photos.isNotEmpty()
    }

    fun appliesTo(photographer: String?): Boolean {
        if (photographer != null) {
            return photos.any { photo -> photo.photographer.displayName == photographer }
        }
        return true
    }

    val primaryPhoto: Photo?
        get() = photos.firstOrNull { obj: Photo -> obj.primary }

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
