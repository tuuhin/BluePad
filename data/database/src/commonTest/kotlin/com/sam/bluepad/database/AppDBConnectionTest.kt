package com.sam.bluepad.database

import assertk.assertThat
import assertk.assertions.isNull
import com.sam.bluepad.database.data.AppDBBuilder
import com.sam.bluepad.database.data.BluePadDB
import com.sam.bluepad.database.data.dao.DevicesInfoDao
import com.sam.bluepad.database.di.DbTestModule
import com.sam.bluepad.testing.annotations.RunWithPlatform
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.uuid.Uuid

@RunWithPlatform
class AppDBConnectionTest : KoinTest {


    private val builder by inject<AppDBBuilder>()

    lateinit var db: BluePadDB
    lateinit var devicesDao: DevicesInfoDao

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        module<DbTestModule>()
    }

    @BeforeTest
    fun setup() {
        db = BluePadDB.prepareRoomDb(builder.getMemoryDbBuilder())
        devicesDao = db.devicesDao()
    }

    fun tearDown() = db.close()

    @Test
    fun run_and_check_if_we_can_read_a_dao_method() = runTest {
        val entry = devicesDao.readDeviceById(Uuid.random())
        assertThat(entry).isNull()
    }
}
