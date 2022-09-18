package org.railwaystations.rsapi.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Value
@Builder
public class Station {

    private static final int EARTH_RADIUS = 6371;

    @Builder.Default
    Key key = new Key("", "0");

    String title;

    @Builder.Default
    Coordinates coordinates = new Coordinates(0.0, 0.0);

    String ds100;

    @Getter
    List<Photo> photos = new ArrayList<>();

    @Builder.Default
    boolean active = true;

    public boolean hasPhoto() {
        return this.photos.size() > 0;
    }

    public boolean appliesTo(String photographer) {
        if (photographer != null) {
            return hasPhoto() && photos.stream().anyMatch(photo -> photo.getPhotographer().getDisplayName().equals(photographer));
        }
        return true;
    }

    @Value
    @AllArgsConstructor
    public static class Key {
        String country;
        String id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Station other)) {
            return false;
        }
        return Objects.equals(key, other.getKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

}
