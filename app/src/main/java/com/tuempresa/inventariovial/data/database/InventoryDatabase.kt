package com.tuempresa.inventariovial.data.database

import android.content.Context
import com.tuempresa.inventariovial.auth.*

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.tuempresa.inventariovial.data.dao.InventoryDao
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity
import com.tuempresa.inventariovial.data.entity.Sic23Entity
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.pavement.PavementDao
import com.tuempresa.inventariovial.pavement.PavementDamageEntity
import com.tuempresa.inventariovial.pavement.PavementDamagePhotoEntity
import com.tuempresa.inventariovial.pavement.PavementSampleEntity
import com.tuempresa.inventariovial.pavement.PavementSectionEntity


@Database(
    entities = [
        com.tuempresa.inventariovial.core.DriveSyncIntent::class, UserCacheEntity::class, UserSyncStateEntity::class, LocalLoginStateEntity::class, AuditLogEntity::class,
        InventoryRecordEntity::class,
        SicExtensionEntity::class, PhotoEntity::class, PhotoHistoryEntity::class, FieldPrEntity::class, DeliveryBatchEntity::class,
        Sic17Entity::class,
        Sic18Entity::class,
        Sic19Entity::class,
        Sic20Entity::class,
        Sic21Entity::class,
        Sic22Entity::class,
        Sic23Entity::class,
        Sic17AEntity::class, Sic17BEntity::class, Sic18AEntity::class,
        TrackPointEntity::class, FieldSession::class,
        ScapInspectionEntity::class, ScapFieldValueEntity::class, ScapSpanEntity::class,
        ScapSubstructureEntity::class, ScapSupportEntity::class, ScapElementEntity::class,
        ScapElementConditionEntity::class, ScapDefectEntity::class, ScapSketchEntity::class,
        ScapProfilePointEntity::class, ScapJointEntity::class,
        PavementSampleEntity::class, PavementDamageEntity::class, PavementDamagePhotoEntity::class, PavementSectionEntity::class
    ],

    version = 19,

    exportSchema = true
)
abstract class InventoryDatabase :
    RoomDatabase() {

    var syncContext: Context? = null
    abstract fun automaticSyncDao(): com.tuempresa.inventariovial.core.AutomaticSyncDao
    abstract fun userDao(): UserDao
    abstract fun auditDao(): AuditDao

    abstract fun scapDao(): ScapDao

    abstract fun inventoryDao():
            InventoryDao
    abstract fun pavementDao(): PavementDao


    companion object {
        val MIGRATION_18_19 = object : Migration(18,19) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pavement_damage_photos ADD COLUMN retiredAt INTEGER")
                db.execSQL("ALTER TABLE pavement_damage_photos ADD COLUMN replacedByPhotoId TEXT")
            }
        }
        val MIGRATION_17_18 = object : Migration(17,18) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS photo_history (id TEXT NOT NULL PRIMARY KEY, recordId TEXT NOT NULL, photoId TEXT NOT NULL, revision INTEGER NOT NULL, action TEXT NOT NULL, snapshotJson TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_history_recordId ON photo_history(recordId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_history_photoId ON photo_history(photoId)")
            }
        }
        /** Only nullability changes. Every existing value, UUID and foreign key is retained. */
        val MIGRATION_16_17 = object : Migration(16,17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `inventory_records_nullable` (`id` TEXT NOT NULL, `sicCode` TEXT NOT NULL, `assetType` TEXT NOT NULL, `routeCode` TEXT NOT NULL, `roadbedCode` TEXT NOT NULL, `startPrCode` TEXT NOT NULL, `startDistanceM` REAL NOT NULL, `endPrCode` TEXT, `endDistanceM` REAL, `sideCode` TEXT, `latitude` REAL, `longitude` REAL, `altitudeM` REAL, `gpsAccuracyM` REAL, `surveyDate` TEXT NOT NULL, `observations` TEXT, `status` TEXT NOT NULL, `photoSyncStatus` TEXT NOT NULL, `excelSyncStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `endLatitude` REAL, `endLongitude` REAL, `endGpsAccuracyM` REAL, `locationSource` TEXT NOT NULL DEFAULT 'MANUAL', `sideSource` TEXT NOT NULL DEFAULT 'MANUAL', `sessionId` TEXT, `gpsTimestamp` INTEGER, `endGpsTimestamp` INTEGER, `gnssProvider` TEXT NOT NULL DEFAULT 'TABLET', `gnssFixType` TEXT, `verticalAccuracyM` REAL, `satellites` INTEGER, `hdop` REAL, `correctionAge` REAL, `isRtkFixed` INTEGER NOT NULL DEFAULT 0, `serverSyncStatus` TEXT NOT NULL DEFAULT 'PENDING', `serverSyncError` TEXT, `segment` TEXT, `surveyDirection` TEXT, `axisMeasureM` REAL, `distanceToRoadAxisM` REAL, `roadMatchConfidence` REAL, `matchedSegmentId` TEXT, `projectedLatitude` REAL, `projectedLongitude` REAL, `endLocationSource` TEXT NOT NULL DEFAULT 'MANUAL', `routeSource` TEXT NOT NULL DEFAULT 'MANUAL', `suggestedRouteCode` TEXT, `suggestedPrCode` TEXT, `suggestedDistanceFromPrM` REAL, `routeMatchDistanceM` REAL, `routeMatchConfidence` REAL, `routeMatchAmbiguous` INTEGER NOT NULL DEFAULT 0, `endSuggestedPrCode` TEXT, `endSuggestedDistanceFromPrM` REAL, `endAxisMeasureM` REAL, `endDistanceToRoadAxisM` REAL, `endMatchedSegmentId` TEXT, `endProjectedLatitude` REAL, `endProjectedLongitude` REAL, PRIMARY KEY(`id`))""")
                db.execSQL("""INSERT INTO inventory_records_nullable (`id`,`sicCode`,`assetType`,`routeCode`,`roadbedCode`,`startPrCode`,`startDistanceM`,`endPrCode`,`endDistanceM`,`sideCode`,`latitude`,`longitude`,`altitudeM`,`gpsAccuracyM`,`surveyDate`,`observations`,`status`,`photoSyncStatus`,`excelSyncStatus`,`createdAt`,`updatedAt`,`endLatitude`,`endLongitude`,`endGpsAccuracyM`,`locationSource`,`sideSource`,`sessionId`,`gpsTimestamp`,`endGpsTimestamp`,`gnssProvider`,`gnssFixType`,`verticalAccuracyM`,`satellites`,`hdop`,`correctionAge`,`isRtkFixed`,`serverSyncStatus`,`serverSyncError`,`segment`,`surveyDirection`,`axisMeasureM`,`distanceToRoadAxisM`,`roadMatchConfidence`,`matchedSegmentId`,`projectedLatitude`,`projectedLongitude`,`endLocationSource`,`routeSource`,`suggestedRouteCode`,`suggestedPrCode`,`suggestedDistanceFromPrM`,`routeMatchDistanceM`,`routeMatchConfidence`,`routeMatchAmbiguous`,`endSuggestedPrCode`,`endSuggestedDistanceFromPrM`,`endAxisMeasureM`,`endDistanceToRoadAxisM`,`endMatchedSegmentId`,`endProjectedLatitude`,`endProjectedLongitude`) SELECT `id`,`sicCode`,`assetType`,`routeCode`,`roadbedCode`,`startPrCode`,`startDistanceM`,`endPrCode`,`endDistanceM`,`sideCode`,`latitude`,`longitude`,`altitudeM`,`gpsAccuracyM`,`surveyDate`,`observations`,`status`,`photoSyncStatus`,`excelSyncStatus`,`createdAt`,`updatedAt`,`endLatitude`,`endLongitude`,`endGpsAccuracyM`,`locationSource`,`sideSource`,`sessionId`,`gpsTimestamp`,`endGpsTimestamp`,`gnssProvider`,`gnssFixType`,`verticalAccuracyM`,`satellites`,`hdop`,`correctionAge`,`isRtkFixed`,`serverSyncStatus`,`serverSyncError`,`segment`,`surveyDirection`,`axisMeasureM`,`distanceToRoadAxisM`,`roadMatchConfidence`,`matchedSegmentId`,`projectedLatitude`,`projectedLongitude`,`endLocationSource`,`routeSource`,`suggestedRouteCode`,`suggestedPrCode`,`suggestedDistanceFromPrM`,`routeMatchDistanceM`,`routeMatchConfidence`,`routeMatchAmbiguous`,`endSuggestedPrCode`,`endSuggestedDistanceFromPrM`,`endAxisMeasureM`,`endDistanceToRoadAxisM`,`endMatchedSegmentId`,`endProjectedLatitude`,`endProjectedLongitude` FROM inventory_records""")
                db.execSQL("DROP TABLE inventory_records")
                db.execSQL("ALTER TABLE inventory_records_nullable RENAME TO inventory_records")
                listOf("routeCode","sicCode","surveyDate").forEach { column ->
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_records_$column ON inventory_records (`$column`)")
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15,16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS drive_sync_outbox (`key` TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, recordId TEXT NOT NULL,
                revision INTEGER NOT NULL, syncedRevision INTEGER NOT NULL, state TEXT NOT NULL, origin TEXT NOT NULL, warningsConfirmed INTEGER NOT NULL,
                lastSyncedAt INTEGER, error TEXT, receiptJson TEXT, preparedJson TEXT, attempts INTEGER NOT NULL)""")
            }
        }
        val MIGRATION_14_15 = object : Migration(14,15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS sic_extension_details (
                    recordId TEXT NOT NULL PRIMARY KEY, formatCode TEXT NOT NULL, valuesJson TEXT NOT NULL,
                    sourceRecordId TEXT, ownerUserId TEXT NOT NULL, status TEXT NOT NULL,
                    FOREIGN KEY(recordId) REFERENCES inventory_records(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(sourceRecordId) REFERENCES inventory_records(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sic_extension_details_sourceRecordId ON sic_extension_details(sourceRecordId)")
            }
        }

        /** Parallel B: provenance is separate from the contractual traffic-sign code. */
        val MIGRATION_13_14 = com.tuempresa.inventariovial.pavement.PavementMigration.MIGRATION_13_14

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sic22_details ADD COLUMN signalCodeSource TEXT NOT NULL DEFAULT 'CATALOG'")
                db.execSQL("ALTER TABLE sic22_details ADD COLUMN signalDescription TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sic22_details ADD COLUMN signalObservation TEXT NOT NULL DEFAULT ''")
            }
        }
        /** Pavimentos se incorpora sobre la base final v11 sin reutilizar nÃºmeros de migraciÃ³n previos. */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS pavement_samples (
                    id TEXT NOT NULL PRIMARY KEY, tramo TEXT NOT NULL, ruta TEXT NOT NULL,
                    muestra INTEGER NOT NULL, carril TEXT NOT NULL, calzada TEXT NOT NULL,
                    prInicial TEXT NOT NULL, prFinal TEXT NOT NULL, anchoM REAL NOT NULL,
                    largoM REAL NOT NULL, surface TEXT NOT NULL, fecha TEXT NOT NULL,
                    inspectorUserId TEXT NOT NULL, inspectorNombre TEXT NOT NULL,
                    status TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
                    slabRows INTEGER NOT NULL DEFAULT 0, slabColumns INTEGER NOT NULL DEFAULT 0,
                    laneWidthM REAL, calculationSnapshot TEXT
                )""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pavement_samples_ruta_tramo_surface_muestra ON pavement_samples(ruta, tramo, surface, muestra)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pavement_samples_inspectorUserId ON pavement_samples(inspectorUserId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pavement_samples_surface ON pavement_samples(surface)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS pavement_damages (
                    id TEXT NOT NULL PRIMARY KEY, sampleId TEXT NOT NULL, correlativo INTEGER NOT NULL,
                    surface TEXT NOT NULL, code INTEGER NOT NULL, name TEXT NOT NULL, severity TEXT,
                    geometry TEXT NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL, diameterM REAL,
                    widthM REAL, lengthM REAL, areaM2 REAL, quantity REAL, crackWidthMm REAL,
                    observation TEXT, createdAt INTEGER NOT NULL, slabs TEXT NOT NULL DEFAULT '',
                    inspectorUserId TEXT NOT NULL DEFAULT '', inspectorName TEXT NOT NULL DEFAULT '', depthMm REAL,
                    FOREIGN KEY(sampleId) REFERENCES pavement_samples(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pavement_damages_sampleId ON pavement_damages(sampleId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS pavement_damage_photos (
                    id TEXT NOT NULL PRIMARY KEY, damageId TEXT NOT NULL, localPath TEXT NOT NULL, createdAt INTEGER NOT NULL,
                    FOREIGN KEY(damageId) REFERENCES pavement_damages(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pavement_damage_photos_damageId ON pavement_damage_photos(damageId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS pavement_sections (
                    ruta TEXT NOT NULL, tramo TEXT NOT NULL, surface TEXT NOT NULL, areaM2 REAL NOT NULL,
                    additionalSampleIds TEXT NOT NULL, inspectorUserId TEXT NOT NULL,
                    PRIMARY KEY(ruta,tramo,surface)
                )""")
            }
        }
        val MIGRATION_10_11 = RoundTwoMigration.MIGRATION_10_11
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endLocationSource TEXT NOT NULL DEFAULT 'MANUAL'")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("axisMeasureM", "distanceToRoadAxisM", "roadMatchConfidence",
                    "projectedLatitude", "projectedLongitude").forEach {
                    db.execSQL("ALTER TABLE inventory_records ADD COLUMN $it REAL")
                }
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN matchedSegmentId TEXT")
            }
        }
        val MIGRATION_7_8 = InventoryMigrations.MIGRATION_7_8
        val MIGRATION_6_7 = EngineeringMigration.MIGRATION_6_7
        val MIGRATION_5_6 = ScapMigration.MIGRATION_5_6
        val MIGRATION_4_5 = InventoryMigrations.MIGRATION_4_5
        val MIGRATION_3_4 = InventoryMigrations.MIGRATION_3_4
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS sic23_details (
                    recordId TEXT NOT NULL, classCode TEXT NOT NULL, typeCode TEXT,
                    widthM REAL, description TEXT NOT NULL, PRIMARY KEY(recordId),
                    FOREIGN KEY(recordId) REFERENCES inventory_records(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent())
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endLatitude REAL")
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endLongitude REAL")
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endGpsAccuracyM REAL")
            }
        }


        @Volatile
        private var INSTANCE:
                InventoryDatabase? = null


        fun getInstance(
            context: Context
        ): InventoryDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            InventoryDatabase::class.java,
                            "inventario_vial.db"
                        )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                            .build()

                    instance.syncContext = context.applicationContext
                    INSTANCE = instance

                    instance
                }
        }
    }
}
