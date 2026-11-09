package com.lifetimere.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyEntryEntity::class, JourneyDirectionEntity::class, ReviewEntity::class, EvidenceEntity::class, GrowthLedgerEntity::class, TravelerProfileEntity::class, TravelerAnswerEntity::class, ReviewFeedbackEntity::class, JournalNoteEntity::class, KeepsakeEntity::class, ProjectEntity::class, ProjectMilestoneEntity::class, ProjectEntryEntity::class, ActionFollowupEntity::class, GrowthRewardEntity::class],
    version = 10,
    exportSchema = true,
)
abstract class LifeTimeDatabase : RoomDatabase() {
    abstract fun dao(): LifeTimeDao

    companion object {
        fun create(context: Context): LifeTimeDatabase = Room.databaseBuilder(
            context.applicationContext,
            LifeTimeDatabase::class.java,
            "lifetimere.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10).build()

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `growth_rewards` (`rewardId` TEXT NOT NULL PRIMARY KEY, `sourceEntryId` INTEGER, FOREIGN KEY(`sourceEntryId`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_growth_rewards_sourceEntryId` ON `growth_rewards` (`sourceEntryId`)")
                db.execSQL("UPDATE traveler_profiles SET resilienceTendency = 3 WHERE questionnaireVersion < 2")
                val legacyTracks = mapOf(
                    "STRENGTH" to listOf("bracer", "backpack"),
                    "KNOWLEDGE" to listOf("notebook", "star_map"),
                    "SKILL" to listOf("sketch", "tool_board"),
                )
                for ((attribute, rewards) in legacyTracks) {
                    db.query(
                        """
                        SELECT COALESCE(SUM(g.points), 0) FROM growth_ledger g
                        JOIN reviews r ON r.entry_id = g.entry_id AND r.revision = g.revision
                        JOIN daily_entries e ON e.id = r.entry_id AND e.revision = r.revision
                        WHERE r.status = 'COMPLETE' AND g.attribute_id = ?
                        """.trimIndent(),
                        arrayOf(attribute),
                    ).use { cursor ->
                        if (cursor.moveToFirst()) {
                            val points = cursor.getLong(0)
                            if (points >= 1) db.execSQL("INSERT INTO growth_rewards (rewardId) VALUES (?)", arrayOf(rewards[0]))
                            if (points >= 5) db.execSQL("INSERT INTO growth_rewards (rewardId) VALUES (?)", arrayOf(rewards[1]))
                        }
                    }
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `action_followups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `action` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `status` TEXT NOT NULL, `outcome` TEXT NOT NULL, `completedAt` INTEGER, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_action_followups_projectId` ON `action_followups` (`projectId`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `nextStep` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_milestones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `completedAt` INTEGER, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_milestones_projectId` ON `project_milestones` (`projectId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_entries` (`projectId` INTEGER NOT NULL, `entryId` INTEGER NOT NULL, PRIMARY KEY(`projectId`, `entryId`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`entryId`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_entries_entryId` ON `project_entries` (`entryId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `keepsakes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, `quote` TEXT NOT NULL, `title` TEXT NOT NULL, `material` TEXT NOT NULL, `shelfSlot` INTEGER, `chapterSlot` INTEGER, FOREIGN KEY(`entryId`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_keepsakes_entryId` ON `keepsakes` (`entryId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_keepsakes_shelfSlot` ON `keepsakes` (`shelfSlot`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_keepsakes_chapterSlot` ON `keepsakes` (`chapterSlot`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `last_attempt_status` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `last_error_kind` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `last_error_message` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `last_attempt_at_epoch_millis` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `journal_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`entryId`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_notes_entryId` ON `journal_notes` (`entryId`)")
                db.execSQL("INSERT INTO journal_notes (entryId, body, createdAt, updatedAt) SELECT id, body, created_at_epoch_millis, updated_at_epoch_millis FROM daily_entries")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reviews` (`entry_id` INTEGER NOT NULL, `revision` INTEGER NOT NULL, `status` TEXT NOT NULL, `summary` TEXT NOT NULL, `rule_version` INTEGER NOT NULL, `updated_at_epoch_millis` INTEGER NOT NULL, PRIMARY KEY(`entry_id`), FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reviews_entry_id` ON `reviews` (`entry_id`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `evidence` (`entry_id` INTEGER NOT NULL, `action_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, `attribute_id` TEXT NOT NULL, `intensity` INTEGER NOT NULL, `quote` TEXT NOT NULL, `explanation` TEXT NOT NULL, PRIMARY KEY(`entry_id`, `action_id`), FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_evidence_entry_id` ON `evidence` (`entry_id`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `growth_ledger` (`entry_id` INTEGER NOT NULL, `action_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, `attribute_id` TEXT NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`entry_id`, `action_id`), FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_growth_ledger_entry_id` ON `growth_ledger` (`entry_id`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `traveler_profiles` (`id` INTEGER NOT NULL, `status` TEXT NOT NULL, `returnStatus` TEXT, `questionnaireVersion` INTEGER NOT NULL, `questionIndex` INTEGER NOT NULL, `strengthTendency` INTEGER NOT NULL, `enduranceTendency` INTEGER NOT NULL, `knowledgeTendency` INTEGER NOT NULL, `skillTendency` INTEGER NOT NULL, `resilienceTendency` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `traveler_answers` (`questionId` TEXT NOT NULL, `profileId` INTEGER NOT NULL, `optionId` TEXT NOT NULL, `questionnaireVersion` INTEGER NOT NULL, PRIMARY KEY(`questionId`), FOREIGN KEY(`profileId`) REFERENCES `traveler_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_traveler_answers_profileId` ON `traveler_answers` (`profileId`)")
                db.execSQL("INSERT INTO `traveler_profiles` (`id`, `status`, `returnStatus`, `questionnaireVersion`, `questionIndex`, `strengthTendency`, `enduranceTendency`, `knowledgeTendency`, `skillTendency`, `resilienceTendency`, `createdAtEpochMillis`, `updatedAtEpochMillis`) VALUES (1, 'LEGACY_NEUTRAL', NULL, 1, 0, 3, 3, 3, 3, 3, 0, 0)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `analysis_model` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `prompt_version` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `schema_version` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `reviews` ADD COLUMN `analyzed_at_epoch_millis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `review_feedback` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entry_id` INTEGER NOT NULL, `revision` INTEGER NOT NULL, `reason` TEXT NOT NULL, `analysis_model` TEXT NOT NULL, `prompt_version` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_feedback_entry_id` ON `review_feedback` (`entry_id`)")
            }
        }
    }
}
