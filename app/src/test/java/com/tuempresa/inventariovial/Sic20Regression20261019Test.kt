package com.tuempresa.inventariovial

import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class Sic20Regression20261019Test {
    @Test fun sic22DraftFinalizationKeepsPrimaryKeyGuard(){HotfixIntegrity146Test().existingSic22DraftFinalizesWithoutPrimaryKeyCollision()}
    @Test fun anotherSicCannotReuseTheSameUuid(){HotfixIntegrity146Test().recoveredUuidCannotBecomeAnotherSic()}
    @Test fun verticalDraftNeverReopensAsFord(){HotfixIntegrity146Test().verticalDraftWithPhotoRoutesToVerticalAndNeverFord()}
    @Test fun existingPhotosKeepUuidIndexAndAck(){HotfixIntegrity146Test().existingPhotosKeepIdentityIndexAndAckWhileNewPhotosAreAdded()}
    @Test fun corruptRawFormCannotOverwriteEvidence(){HotfixIntegrity146Test().rawDraftWrongFormCannotOverwriteTextOrPhotos()}
    @Test fun allOtherEditableSicFamiliesKeepOneRecordAndPhoto(){HotfixIntegrity146Test().otherEditableSicDetailsUpdateInPlaceWithoutAbortOrPhotoCascade()}
    @Test fun wallAndTunnelLifecycleKeepTheirExport(){FieldLifecycleTest().fieldRecordsReopenEditAndExport()}
    @Test fun historicalAndMeasuredWallLengthKeepTheirBehavior(){TechnicalRules4Test().wallAutomaticModeUpdatesAfterProgressiveEditButHistoricalAndMeasuredRemain()}
    @Test fun historicalTunnelSideIsNotMigratedSilently(){TechnicalRules4Test().tunnelSidePersistsAndHistoricalSideIsNeverMigratedSilently()}
}
