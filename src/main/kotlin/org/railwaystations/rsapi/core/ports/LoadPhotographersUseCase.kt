package org.railwaystations.rsapi.core.ports

interface LoadPhotographersUseCase {
    fun getPhotographersPhotocountMap(country: String?): Map<String, Long>
}
