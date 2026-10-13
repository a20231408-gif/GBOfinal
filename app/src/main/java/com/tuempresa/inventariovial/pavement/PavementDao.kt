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
    // Keep the legacy row order used by prepared source hashes, including equal timestamps.
    @Query("SELECT * FROM pavement_damage_photos WHERE damageId=:damageId AND retiredAt IS NULL ORDER BY rowid") suspend fun photos(damageId: String): List<PavementDamagePhotoEntity>
    @Query("SELECT * FROM pavement_damage_photos WHERE damageId=:damageId AND retiredAt IS NULL ORDER BY createdAt,id") fun observePhotos(damageId: String): Flow<List<PavementDamagePhotoEntity>>
    @Query("SELECT * FROM pavement_damage_photos WHERE damageId=:damageId ORDER BY createdAt,id") fun observePhotoHistory(damageId: String): Flow<List<PavementDamagePhotoEntity>>
    @Query("SELECT * FROM pavement_damage_photos WHERE damageId=:damageId ORDER BY createdAt,id") suspend fun photoHistory(damageId: String): List<PavementDamagePhotoEntity>
    @Query("SELECT p.* FROM pavement_damage_photos p INNER JOIN pavement_damages d ON d.id=p.damageId WHERE d.sampleId=:sampleId AND p.retiredAt IS NULL ORDER BY p.createdAt,p.id") fun observeSamplePhotos(sampleId: String): Flow<List<PavementDamagePhotoEntity>>
    @Query("SELECT * FROM pavement_samples WHERE id=:id") fun observeSample(id: String): Flow<PavementSampleEntity?>
}
