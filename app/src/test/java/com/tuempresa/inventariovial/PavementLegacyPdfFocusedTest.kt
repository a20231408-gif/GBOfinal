package com.tuempresa.inventariovial

import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Executes only existing directly related assertions on the available offline SDK 34. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementLegacyPdfFocusedTest {
    @Test fun existingSharedBoundaryAssignment()=PavementParallelCTest().pdfSegmentsKeepFullScaleAndClipDamageAtSharedBoundaries()
    @Test fun existingDenseReportAndSurfaceRecipes()=PavementParallelCTest().denseReportPaginatesRowsAndRendersEveryVisualRecipe()
    @Test fun existingMeasuredSlabExcelAndPdf()=PavementObservations10ITest().exportedWorkbookAndPdfUseSavedCoordinatesAndLocalizedRenderer()
    @Test fun existingAsphaltReportAndExcel()=PavementReportTest().rendersSampleReportAndExcelWithoutCurves()
}
