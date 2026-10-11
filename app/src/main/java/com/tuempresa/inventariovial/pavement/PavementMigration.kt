package com.tuempresa.inventariovial.pavement

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PavementMigration {
    /** Final chain after the SIC-22 migration. */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE pavement_samples ADD COLUMN slabWidthM REAL")
            db.execSQL("ALTER TABLE pavement_samples ADD COLUMN slabLengthM REAL")
            db.execSQL("ALTER TABLE pavement_samples ADD COLUMN sicDataJson TEXT")
            db.execSQL("UPDATE pavement_samples SET slabWidthM = anchoM / slabColumns, slabLengthM = largoM / slabRows WHERE surface = 'RIGID' AND slabRows > 0 AND slabColumns > 0")
            db.execSQL("DROP INDEX IF EXISTS index_pavement_samples_ruta_tramo_surface_muestra")
            db.execSQL("CREATE UNIQUE INDEX index_pavement_samples_ruta_tramo_calzada_surface_muestra ON pavement_samples(ruta, tramo, calzada, surface, muestra)")
            db.execSQL("UPDATE pavement_damages SET severity = CASE severity WHEN 'B' THEN 'L' WHEN 'A' THEN 'H' ELSE severity END")
            // Closed snapshots are immutable evidence; legacy severities are normalized only on read.
        }
    }
}
