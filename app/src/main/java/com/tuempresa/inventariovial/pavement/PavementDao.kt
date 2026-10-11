package com.tuempresa.inventariovial.pavement

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PavementDao {
    @Query("SELECT * FROM pavement_sections") fun observeSections(): Flow<List<PavementSectionEntity>>
    @Upsert suspend fun saveSection(section:PavementSectionEntity)
    @Query("SELECT * FROM pavement_samples ORDER BY fecha DESC, createdAt DESC, muestra DESC, id ASC") fun observeSamples(): Flow<List<PavementSampleEntity>>
    @Query("SELECT * FROM pavement_samples WHERE id=:id") suspend fun sample(id: String): PavementSampleEntity?
    @Upsert suspend fun saveSample(sample: PavementSampleEntity)
    @Query("UPDATE pavement_samples SET status='CLOSED', updatedAt=:now WHERE id=:id") suspend fun closeSample(id: String, now: Long)
    @Query("SELECT * FROM pavement_damages WHERE sampleId=:sampleId ORDER BY correlativo") fun observeDamages(sampleId: String): Flow<List<PavementDamageEntity>>
    @Query("SELECT * FROM pavement_damages WHERE sampleId=:sampleId ORDER BY correlativo") suspend fun damages(sampleId: String): List<PavementDamageEntity>
    @Upsert suspend fun saveDamage(damage: PavementDamageEntity)
    @Query("SELECT * FROM pavement_damages WHERE id=:id") suspend fun damage(id: String): PavementDamageEntity?
    @Query("SELECT * FROM pavement_damage_photos WHERE id=:id") suspend fun photo(id: String): PavementDamagePhotoEntity?
    @Query("DELETE FROM pavement_damages WHERE id=:id") suspend fun deleteDamage(id: String)
    @Query("DELETE FROM pavement_damage_photos WHERE id=:id") suspend fun deletePhoto(id: String)
    @Query("SELECT COUNT(*) FROM pavement_damage_photos WHERE localPath=:path") suspend fun photoPathCount(path: String): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePhoto(photo: PavementDamagePhotoEntity)
    @Query("SELECT COUNT(*) FROM pavement_damages WHERE sampleId=:sampleId") suspend fun damageCount(sampleId: String): Int
    @Query("SELECT * FROM pavement_damage_photos WHERE damageId=:damageId") suspend fun photos(damageId: String): List<PavementDamagePhotoEntity>
    @Query("SELECT * FROM pavement_samples WHERE id=:id") fun observeSample(id: String): Flow<PavementSampleEntity?>
}
