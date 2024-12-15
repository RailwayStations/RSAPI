package org.railwaystations.rsapi.core.ports.inbound

interface LoadPhotographersUseCase {
    fun getPhotographersPhotocountMap(country: String?): Map<String, Int>
}