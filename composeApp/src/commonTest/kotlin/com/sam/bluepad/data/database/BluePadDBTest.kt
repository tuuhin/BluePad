package com.sam.bluepad.data.database

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.extracting
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import com.sam.bluepad.data.database.entities.SketchAuditLogEntity
import com.sam.bluepad.data.database.entities.SketchContentEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.createPlatformTestModule
import com.sam.bluepad.di.testModule
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.SketchChangeType
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.core.context.loadKoinModules
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class BluePadDBTest : KoinTest {

    private val db by inject<BluePadDB>()

    @get:Rule(1)
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        // include the platform module
        modules(createPlatformModule() + commonAppModule)
        // load the test module here
        loadKoinModules(createPlatformTestModule() + testModule)
    }

    @get:Rule(2)
    val testDispatcher = TestDispatcherRule()

    @Test
    fun test_devices_dao_crud_and_queries() = runTest {
        val dao = db.devicesDao()
        val deviceId = Uuid.random()
        val now = Clock.System.now()

        val entity = DeviceInfoEntity(
            id = deviceId,
            displayName = "Test Device Windows",
            pairedAt = now,
            lastSeenAt = now,
            isRevoked = false,
            deviceOs = DevicePlatformOS.WINDOWS,
        )

        // Insert or update
        dao.insertOrUpdateDevice(entity)
        advanceUntilIdle()

        // Check exists
        assertThat(dao.checkIfDeviceExists(deviceId)).isTrue()

        // Read by id
        val fetched = dao.readDeviceById(deviceId)
        assertThat(fetched).isNotNull()
        assertThat(fetched?.displayName).isEqualTo("Test Device Windows")
        assertThat(fetched?.deviceOs).isEqualTo(DevicePlatformOS.WINDOWS)
        assertThat(fetched?.isRevoked).isNotNull().isFalse()

        // Revoke device
        dao.setRevokeStatusOnDeviceByID(true, deviceId)
        val revoked = dao.readDeviceById(deviceId)
        assertThat(revoked).isNotNull()
        assertThat(revoked?.isRevoked).isNotNull().isTrue()

        // Re-enroll device
        dao.reEnrollDeviceByID(deviceId)
        val reEnrolled = dao.readDeviceById(deviceId)
        assertThat(reEnrolled?.isRevoked).isNotNull().isFalse()

        // Delete device
        dao.deleteDevice(reEnrolled!!)
        assertThat(dao.checkIfDeviceExists(deviceId)).isFalse()
        assertThat(dao.readDeviceById(deviceId)).isNull()
    }

    @Test
    fun test_sketches_dao_and_relations() = runTest {
        val sketchesDao = db.sketchesDao()
        val sketchId = Uuid.random()
        val deviceId = Uuid.random()
        val now = Clock.System.now()

        val metadata = SketchMetadataEntity(
            id = sketchId,
            title = "My First Sketch",
            createdAt = now,
            modifiedAt = now,
            version = 1,
            isDeleted = false,
            createdByDeviceId = deviceId,
            lastModifiedByDevice = deviceId,
        )

        val content = SketchContentEntity(
            id = sketchId,
            content = "sample binary/json stroke data",
        )

        val auditLog = SketchAuditLogEntity(
            id = Uuid.random(),
            sketchId = sketchId,
            changeType = SketchChangeType.CREATE,
            prevVersion = 0,
            newVersion = 1,
            modifiedAt = now,
            deviceId = deviceId,
        )

        // Insert using transaction helper
        sketchesDao.insertSketchMetaDataAndContent(metadata, content, auditLog)

        // Read sketch from id
        val sketchRelation = sketchesDao.getSketchFromId(sketchId)
        assertThat(sketchRelation).isNotNull()
        assertThat(sketchRelation?.metaData?.title).isEqualTo("My First Sketch")
        assertThat(sketchRelation?.content).isNotNull()
        assertThat(sketchRelation?.content?.id).isEqualTo(sketchId)

        val allSketches = sketchesDao.readAllSketchesFlow(isDeleted = false).first()
        assertThat(allSketches).extracting { it.metaData.id }.contains(sketchId)

        val sketchesList = sketchesDao.readAllSketches(includeDeleted = true)
        assertThat(sketchesList).extracting { it.metaData.id }.contains(sketchId)
    }
}
