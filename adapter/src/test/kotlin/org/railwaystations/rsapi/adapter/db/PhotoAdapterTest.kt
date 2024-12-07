package org.railwaystations.rsapi.adapter.db

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.PhotoTable
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.PhotoTestFixtures
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.time.Instant

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    JooqCustomizerConfiguration::class,
    PhotoAdapter::class,
)
class PhotoAdapterTest : AbstractPostgreSqlTest() {

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var sut: PhotoAdapter

    @Test
    fun insert() {
        val photoCountBefore = sut.countPhotos()
        val photo =
            PhotoTestFixtures.createPhoto(Station.Key("de", "5081"), UserTestFixtures.createSomeUser())

        val photoId = sut.insert(photo)

        assertThat(sut.countPhotos()).isEqualTo(photoCountBefore + 1)
        val photoRecord = findPhotoRecordById(photoId)
        with(photoRecord!!) {
            assertThat(countrycode).isEqualTo(photo.stationKey.country)
            assertThat(stationid).isEqualTo(photo.stationKey.id)
            assertThat(primary).isEqualTo(photo.primary)
            assertThat(outdated).isEqualTo(photo.outdated)
            assertThat(urlpath).isEqualTo(photo.urlPath)
            assertThat(license).isEqualTo(photo.license.name)
            assertThat(photographerid).isEqualTo(photo.photographer.id)
            assertThat(createdat.epochSecond).isEqualTo(photo.createdAt.epochSecond)
        }
    }

    private fun findPhotoRecordById(photoId: Long) =
        dsl.selectFrom(PhotoTable).where(PhotoTable.id.eq(photoId)).fetchOne()

    @Test
    fun update() {
        val photoRecord = findPhotoRecordById(1)
        val updatedPhoto = Photo(
            id = photoRecord!!.id!!,
            stationKey = Station.Key(photoRecord.countrycode, photoRecord.stationid),
            primary = false,
            urlPath = "newUrlPath",
            photographer = UserTestFixtures.createSomeUser(),
            createdAt = Instant.now(),
            license = License.CC_BY_NC_SA_30_DE,
            outdated = true,
        )

        sut.update(updatedPhoto)

        val updatedPhotoRecord = findPhotoRecordById(photoRecord.id!!)
        with(updatedPhotoRecord!!) {
            assertThat(primary).isEqualTo(updatedPhoto.primary)
            assertThat(outdated).isEqualTo(updatedPhoto.outdated)
            assertThat(urlpath).isEqualTo(updatedPhoto.urlPath)
            assertThat(license).isEqualTo(updatedPhoto.license.name)
            assertThat(photographerid).isEqualTo(updatedPhoto.photographer.id)
            assertThat(createdat.epochSecond).isEqualTo(updatedPhoto.createdAt.epochSecond)
        }
    }

    @Test
    fun delete() {
        val photoRecord = findPhotoRecordById(1)

        sut.delete(photoRecord!!.id!!)

        assertThat(findPhotoRecordById(1)).isNull()
    }

    @Test
    fun updatePhotoOutdated() {
        val photoRecord = findPhotoRecordById(1)
        assertThat(photoRecord!!.outdated).isFalse

        sut.updatePhotoOutdated(photoRecord.id!!)

        assertThat(findPhotoRecordById(1)!!.outdated).isTrue()
    }

    @Test
    fun setAllPhotosForStationSecondary() {
        val key = Station.Key("ch", "8503007")
        val primaryCount = dsl.selectCount().from(PhotoTable).where(
            PhotoTable.countrycode.eq(key.country).and(PhotoTable.stationid.eq(key.id).and(PhotoTable.primary.eq(true)))
        )
        assertThat(primaryCount).isNotEqualTo(0)

        sut.setAllPhotosForStationSecondary(key)

        val photos = dsl.selectFrom(PhotoTable)
            .where(PhotoTable.countrycode.eq(key.country).and(PhotoTable.stationid.eq(key.id))).fetch()
        assertThat(photos.all { it.primary == false }).isTrue
    }

    @Test
    fun setPrimary() {
        val photoRecord = findPhotoRecordById(130)
        assertThat(photoRecord!!.primary).isFalse

        sut.setPrimary(photoRecord.id!!)

        assertThat(findPhotoRecordById(130)!!.primary).isTrue()
    }

    @Test
    fun countPhotos() {
        val photoCount = sut.countPhotos()

        assertThat(photoCount).isEqualTo(94)
    }

    @Test
    fun findNthPhotoId() {
        val photoId = sut.findNthPhotoId(10)

        assertThat(photoId).isEqualTo(11)
    }

}