package com.sam.bluepad.presentation.utils

import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.domain.sync_diff.SyncSnapshotModel
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

object PreviewFakes {

    val FAKE_LOCAL_DEVICE_MODEL = LocalDeviceInfoModel(deviceId = Uuid.random(), name = "Some_Name")

    val FAKE_EXTERNAL_MODEL = ExternalDeviceModel(
        id = Uuid.random(),
        displayName = "Android device",
        pairedAt = LocalDateTime(2025, 1, 10, 4, 32),
        lastSeenAt = LocalDateTime(2025, 1, 10, 4, 32),
        deviceOs = DevicePlatformOS.ANDROID,
    )

    val FAKE_EXTERNAL_MODEL_2 = ExternalDeviceModel(
        id = Uuid.random(),
        displayName = "Windows device",
        pairedAt = LocalDateTime(2025, 1, 10, 4, 32),
        lastSeenAt = LocalDateTime(2025, 1, 10, 4, 32),
        deviceOs = DevicePlatformOS.WINDOWS,
    )

    val FAKE_SKETCH_MODEL = SketchModel(
        id = Uuid.random(),
        createdAt = LocalDateTime(2025, 1, 10, 4, 32),
        modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
        title = "How to play outswing",
        content = "Trent boult swinging the ball outside off stump and he hits a bouncer",
        contentHash = "9306d63f963638711dd2e78b17259abdb45df3ca8fb6063b4f51cdcce93cb16b",
    )

    val FAKE_BLE_PEER_MODEL = BLEPeerData(
        deviceId = Uuid.random(),
        deviceName = "Test device",
        deviceOs = DevicePlatformOS.ANDROID,
    )

    // fake sync diffs
    val FAKE_SYNC_CHANGE_INSERT = SyncChanges.Insert(
        incoming = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 1,
            title = "New entry",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = false,
            contentHash = "fkdnfdsncsn",
        ),
    )

    val FAKE_SYNC_CHANGE_DELETE = SyncChanges.Delete(
        local = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 1,
            title = "Old entry",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = false,
            contentHash = "fkdnfdsncsn",
        ),
        incoming = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 2,
            title = "Old entry",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = false,
            contentHash = "fkdnfdsncsn",
        ),
    )

    val FAKE_SYNC_CHANGE_DELETE_WITH_UPDATED_CONTENT = SyncChanges.Delete(
        local = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 1,
            title = "Old entry previous",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = false,
            contentHash = "fkdnfdsncsn",
        ),
        incoming = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 2,
            title = "Old entry new",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = true,
            contentHash = "fkdnfdsncsn",
        ),
    )

    val FAKE_SYNC_CHANGE_UPDATE = SyncChanges.Update(
        local = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 1,
            title = "Old entry previous",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = false,
            contentHash = "fkdnfdsncsn",
        ),
        incoming = SyncSnapshotModel(
            id = Uuid.parse("cef8d608-151d-4972-ac8d-a3c5db3572aa"),
            version = 12,
            title = "Old entry updated",
            content = "Content for the newest entry",
            modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
            isDeleted = false,
            contentHash = "fkdnfdsncsn",
        ),
    )
}
