package org.railwaystations.rsapi.core.ports.in;

import java.util.Map;

public interface LoadPhotographersUseCase {
    Map<String, Long> getPhotographersPhotocountMap(String country);
}
