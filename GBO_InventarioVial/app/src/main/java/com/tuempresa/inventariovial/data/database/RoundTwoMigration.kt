package com.tuempresa.inventariovial.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoundTwoMigration {
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN routeSource TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN routeMatchAmbiguous INTEGER NOT NULL DEFAULT 0")
            listOf("suggestedRouteCode", "suggestedPrCode", "endSuggestedPrCode", "endMatchedSegmentId").forEach {
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN $it TEXT")
            }
            listOf("suggestedDistanceFromPrM", "routeMatchDistanceM", "routeMatchConfidence", "endSuggestedDistanceFromPrM",
                "endAxisMeasureM", "endDistanceToRoadAxisM", "endProjectedLatitude", "endProjectedLongitude").forEach {
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN $it REAL")
            }
            listOf("photoLatitude", "photoLongitude", "photoAltitudeM", "photoGpsAccuracyM", "photoHdop").forEach {
                db.execSQL("ALTER TABLE photos ADD COLUMN $it REAL")
            }
            listOf("photoGpsTimestamp", "photoSatellites", "photoIsRtkFixed", "capturedAt").forEach {
                db.execSQL("ALTER TABLE photos ADD COLUMN $it INTEGER")
            }
            listOf("photoGnssProvider", "sha256").forEach { db.execSQL("ALTER TABLE photos ADD COLUMN $it TEXT") }
            db.execSQL("ALTER TABLE photos ADD COLUMN photoLocationSource TEXT NOT NULL DEFAULT 'UNAVAILABLE_LEGACY'")
            db.execSQL("""CREATE TABLE IF NOT EXISTS field_prs (id TEXT NOT NULL PRIMARY KEY, routeCode TEXT NOT NULL,
                roadbedCode TEXT NOT NULL, prCode TEXT NOT NULL, type TEXT NOT NULL, description TEXT,
                latitude REAL NOT NULL, longitude REAL NOT NULL, altitudeM REAL, accuracyM REAL NOT NULL,
                gpsTimestamp INTEGER NOT NULL, prAxisMeasureM REAL NOT NULL, segmentId TEXT NOT NULL,
                source TEXT NOT NULL, createdAt INTEGER NOT NULL)""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS delivery_batches (id TEXT NOT NULL PRIMARY KEY, routeCode TEXT NOT NULL,
                fingerprint TEXT NOT NULL, manifestPath TEXT NOT NULL, namingPolicy TEXT NOT NULL, status TEXT NOT NULL,
                createdAt INTEGER NOT NULL, error TEXT)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_delivery_batches_routeCode ON delivery_batches(routeCode)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_field_prs_routeCode_roadbedCode_prCode ON field_prs(routeCode,roadbedCode,prCode)")
        }
    }
}
