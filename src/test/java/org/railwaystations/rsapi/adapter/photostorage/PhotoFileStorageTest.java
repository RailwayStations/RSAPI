package org.railwaystations.rsapi.adapter.photostorage;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PhotoFileStorageTest {

    @Test
    void sanitizeFilename() {
        assertThat(PhotoFileStorage.sanitizeFilename("../../../some<evil>*/file:name?"), is(".._.._.._some_evil___file_name_"));
    }

}