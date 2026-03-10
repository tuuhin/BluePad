package com.sam.bluepad.data.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import com.sam.bluepad.data.repository.FakeSketchesRepoImpl
import com.sam.bluepad.data.serialization.SerializationProtocols
import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Rule
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.Test
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class PayloadManagerTest : KoinTest {

    private val syncOutManger by inject<OutPayloadManager>()
    private val syncInManager by inject<InPayloadManager>()
    private val fakeRepo by inject<FakeSketchesRepoImpl>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single { SerializationProtocols.protobuf } bind ProtoBuf::class
                singleOf(::BytesEncoder) bind BytesEncoder::class
                singleOf(::FakeSketchesRepoImpl) bind SketchesRepository::class
                factoryOf(::SyncManagerImpl) bind SyncManager::class
                factoryOf(::OutgoingPayloadManagerImpl) bind OutPayloadManager::class
                factoryOf(::IncomingPayloadManagerImpl) bind InPayloadManager::class
            },
        )
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()


    @Test
    fun `there is a available chunk when we read some data`() = runTest {

        fakeRepo.createSketchForTest(CreateSketchModel("New", "Conte"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        val hasChunk = syncOutManger.getHasMoreChunks()
        runCurrent()

        assertThat(hasChunk).isEqualTo(true)
    }


    @Test
    fun `check if there is a payload data`() = runTest {

        fakeRepo.createSketchForTest(CreateSketchModel("New", "Conte"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        while (syncOutManger.getHasMoreChunks()) {
            val chunk = syncOutManger.getNextChunk()
            assertThat(chunk).isSuccess()

            val chunkData = chunk.getOrThrow()

            syncInManager.addIncomingPayloadChunk(chunkData.seqNumber, chunkData.payload)
        }

        val processedResult = syncInManager.processData()
        assertThat(processedResult).isSuccess()

        val metadataResult = processedResult.getOrThrow()

        // metadata processing should produce content ids
        assertThat(metadataResult).isInstanceOf(SyncDataPayload.ContentIdsQuery::class)
    }
}