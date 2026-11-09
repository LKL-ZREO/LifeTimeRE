package com.lifetimere.app.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifetimere.app.data.local.LifeTimeDatabase
import com.lifetimere.app.domain.CharacterAttribute
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifeTimeMigrationTest {
    @Test fun upgradeFromV4KeepsSettledGrowthAndMetadata() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-4-to-5-test.db"
        context.deleteDatabase(name)
        try {
            val schema = instrumentation.context.assets.open(
                "com.lifetimere.app.data.local.LifeTimeDatabase/4.json",
            ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            val old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
            try {
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    entity.optJSONArray("indices")?.let { indices ->
                        for (j in 0 until indices.length()) {
                            old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                        }
                    }
                }
                old.execSQL("INSERT INTO daily_entries VALUES (1, '2026-11-01', 'Asia/Shanghai', '跑了三公里', 10, 20, 2)")
                old.execSQL("INSERT INTO reviews (entry_id, revision, status, summary, rule_version, updated_at_epoch_millis, analysis_model, prompt_version, schema_version, analyzed_at_epoch_millis) VALUES (1, 2, 'COMPLETE', '完成跑步', 1, 20, 'old-model', 'daily_review_v4', 'daily_review_output_v1', 19)")
                old.execSQL("INSERT INTO evidence VALUES (1, 'run', 2, 'STRENGTH', 1, '跑了三公里', '运动')")
                old.execSQL("INSERT INTO growth_ledger VALUES (1, 'run', 2, 'STRENGTH', 1)")
                old.version = 4
            } finally { old.close() }
            val upgraded = Room.databaseBuilder(context, LifeTimeDatabase::class.java, name)
                .addMigrations(LifeTimeDatabase.MIGRATION_4_5, LifeTimeDatabase.MIGRATION_5_6).build()
            try {
                val repository = LifeTimeRepository(upgraded)
                val entry = upgraded.dao().getEntry(1)!!
                val note = upgraded.dao().getNotes(1).single()
                assertEquals(entry.body, note.body)
                assertEquals(10L, note.createdAt)
                assertEquals(20L, note.updatedAt)
                assertEquals(2, entry.revision)
                assertEquals(mapOf(CharacterAttribute.STRENGTH to 1), repository.observeAttributeTotals().first())
                val review = repository.observeReview(1).first()!!
                assertEquals("old-model", review.metadata.model)
                assertEquals("daily_review_v4", review.metadata.promptVersion)
                assertEquals("完成跑步", review.summary)
                assertEquals(1, review.evidence.size)
                assertEquals(emptyList<Long>(), repository.pendingJournalIds())
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun upgradeKeepsExistingJournalAndDirection() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "migration-1-to-4-test.db"
            context.deleteDatabase(name)
            try {
                val old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
                try {
                    old.execSQL("CREATE TABLE IF NOT EXISTS `daily_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `local_date` TEXT NOT NULL, `time_zone_id` TEXT NOT NULL, `body` TEXT NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `updated_at_epoch_millis` INTEGER NOT NULL, `revision` INTEGER NOT NULL)")
                    old.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_entries_local_date` ON `daily_entries` (`local_date`)")
                    old.execSQL("CREATE TABLE IF NOT EXISTS `journey_direction` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `updated_at_epoch_millis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    old.execSQL("INSERT INTO daily_entries (local_date, time_zone_id, body, created_at_epoch_millis, updated_at_epoch_millis, revision) VALUES ('2026-10-31', 'Asia/Shanghai', '旧版旅志', 1, 1, 1)")
                    old.execSQL("INSERT INTO journey_direction (id, title, updated_at_epoch_millis) VALUES (1, '持续学习', 1)")
                    old.version = 1
                } finally {
                    old.close()
                }
                val upgraded = Room.databaseBuilder(context, LifeTimeDatabase::class.java, name)
                    .addMigrations(
                        LifeTimeDatabase.MIGRATION_1_2,
                        LifeTimeDatabase.MIGRATION_2_3,
                        LifeTimeDatabase.MIGRATION_3_4,
                        LifeTimeDatabase.MIGRATION_4_5,
                        LifeTimeDatabase.MIGRATION_5_6,
                    )
                    .build()
                try {
                    val entry = upgraded.dao().getEntry("2026-10-31")!!
                    assertEquals("旧版旅志", entry.body)
                    assertEquals(1, entry.revision)
                    assertEquals("持续学习", LifeTimeRepository(upgraded).observeJourneyDirection().first())
                    assertEquals("LEGACY_NEUTRAL", upgraded.dao().getTravelerProfile()?.status)
                    assertEquals(3, upgraded.dao().getTravelerProfile()?.strengthTendency)
                    assertEquals(0, upgraded.dao().getReviewFeedback(entry.id).size)
                    assertEquals("旧版旅志", upgraded.dao().getNotes(entry.id).single().body)
                } finally {
                    upgraded.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
    }
}
