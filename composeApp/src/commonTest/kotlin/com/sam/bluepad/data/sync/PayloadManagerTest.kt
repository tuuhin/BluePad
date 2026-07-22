package com.sam.bluepad.data.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import com.sam.bluepad.data.repository.FakeSketchesRepoImpl
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.testModule
import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.models.FragmentedDataBlock
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.core.context.loadKoinModules
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

    private val syncSessionId = Uuid.random()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        // include the platform module
        modules(createPlatformModule() + commonAppModule)
        // load the test module here
        loadKoinModules(testModule)
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()


    @Test
    fun there_is_a_available_chunk_when_we_read_some_data() = runTest {

        fakeRepo.createSketchForTest(CreateSketchModel("New", "Conte"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        val hasChunk = syncOutManger.getHasMoreChunks()
        runCurrent()

        assertThat(hasChunk).isEqualTo(true)
    }


    @Test
    fun check_if_there_is_a_payload_data() = runTest {

        fakeRepo.createSketchForTest(CreateSketchModel("New", "Conte"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        while (syncOutManger.getHasMoreChunks()) {
            val chunk = syncOutManger.getNextChunk()
            assertThat(chunk).isSuccess()

            val chunkData = chunk.getOrThrow()

            syncInManager.addIncomingPayloadChunk(chunkData.seqNumber, chunkData.payload)
        }

        val processedResult = syncInManager.processData(syncSessionId)
        assertThat(processedResult).isSuccess()

        val metadataResult = processedResult.getOrThrow()

        // metadata processing should produce content ids
        assertThat(metadataResult).isInstanceOf(SyncDataPayload.ContentIdsQuery::class)
    }

    @Test
    fun getHasMoreChunks_returns_false_when_no_data_exists() = runTest {
        val hasChunk = syncOutManger.getHasMoreChunks()
        assertThat(hasChunk).isFalse()
    }

    @Test
    fun getHasMoreChunks_returns_false_after_consuming_all_chunks() = runTest {
        fakeRepo.createSketchForTest(CreateSketchModel("New", "Content"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        while (syncOutManger.getHasMoreChunks()) {
            val chunk = syncOutManger.getNextChunk()
            assertThat(chunk).isSuccess()
        }
        assertThat(syncOutManger.getHasMoreChunks()).isFalse()
    }

    @Test
    fun getNextChunk_fails_or_returns_null_when_no_chunks_prepared() = runTest {
        val chunk = syncOutManger.getNextChunk()
        assertThat(chunk).isFailure()
    }

    @Test
    fun syncInManager_fails_processing_when_chunk_sequence_is_missing() = runTest {
        fakeRepo.createSketchForTest(CreateSketchModel("Sketch 1", "Content 1"), Uuid.random())
        fakeRepo.createSketchForTest(CreateSketchModel("Sketch 2", "Content 2"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        val chunks = mutableListOf<FragmentedDataBlock>()
        while (syncOutManger.getHasMoreChunks()) {
            chunks.add(syncOutManger.getNextChunk().getOrThrow())
        }

        if (chunks.size > 1) {
            // Skip chunk 0 to corrupt the Protobuf polymorphic header
            chunks.drop(1).forEach { chunk ->
                syncInManager.addIncomingPayloadChunk(chunk.seqNumber, chunk.payload)
            }

            // If your manager returns Result.failure:
            val processedResult = syncInManager.processData(syncSessionId)
            assertThat(processedResult.isFailure).isTrue()
        }
    }

    @Test
    fun syncInManager_resets_state_correctly_between_sessions() = runTest {
        fakeRepo.createSketchForTest(CreateSketchModel("First", "Content"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        while (syncOutManger.getHasMoreChunks()) {
            val chunk = syncOutManger.getNextChunk().getOrThrow()
            syncInManager.addIncomingPayloadChunk(chunk.seqNumber, chunk.payload)
        }

        val session1Result = syncInManager.processData(syncSessionId)
        assertThat(session1Result).isSuccess()

        val nextSessionId = Uuid.random()
        fakeRepo.createSketchForTest(CreateSketchModel("Second", "Content"), Uuid.random())

        syncOutManger.prepareChunks(SyncDataPayload.Metadata)
        runCurrent()

        while (syncOutManger.getHasMoreChunks()) {
            val chunk = syncOutManger.getNextChunk().getOrThrow()
            syncInManager.addIncomingPayloadChunk(chunk.seqNumber, chunk.payload)
        }

        val session2Result = syncInManager.processData(nextSessionId)
        assertThat(session2Result).isSuccess()
    }
}
