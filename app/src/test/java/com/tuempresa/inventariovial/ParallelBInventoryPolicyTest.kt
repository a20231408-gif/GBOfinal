package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.field.PhotoCategoryPolicy
import com.tuempresa.inventariovial.field.captureSummary
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.validation.*
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test

class ParallelBInventoryPolicyTest {
    private fun request(state: Sic22FormState) = InventorySaveRequest("SIC-22", "VERTICAL", "PE-3N", "CD", "12", "0",
        null, null, "D", -12.0, -77.0, 1f, "07/10/2026", "", "", SicFormDetail.Sic22(state), photoPaths=emptyList())

    @Test fun searchByNameReturnsExistingPareCode() {
        assertEquals(listOf("R-1"), TrafficSignCatalog.search("1", "pare").map { it.code })
        assertTrue(TrafficSignCatalog.search("2", "pare").any {it.code == "P-58"})
    }

    @Test fun searchByCodeIsCaseInsensitiveAndDoesNotCrossTypes() {
        assertEquals("R-1", TrafficSignCatalog.search("1", "r-1").first().code)
        assertTrue(TrafficSignCatalog.search("2", "R-1").isEmpty())
    }

    @Test fun accentsAndExistingDescriptionsAreSearchableWithoutNewCatalogueRows() {
        assertTrue(TrafficSignCatalog.search("1", "direccion obligada").any { it.code == "R-3" })
        assertTrue(TrafficSignCatalog.matches(TrafficSignCatalog.Sign("TEST", "Nombre", "Descripción específica"), "descripcion especifica"))
        assertEquals(TrafficSignCatalog.signsForType("3"), TrafficSignCatalog.search("3", "  "))
    }

    @Test fun manualCodeRemainsExplicitAndCannotImpersonateCatalogueCode() {
        val manual=Sic22FormState(typeCode="1",signalCode="x-local",signalCodeSource=SignalCodeSource.MANUAL,signalDescription="Placa no listada")
        assertTrue(CaptureValidation.errors(request(manual)).isEmpty())
        assertEquals("X-LOCAL",manual.persistedSignalCode)
        assertEquals(SignalCodeSource.MANUAL,manual.signalCodeSource)
        assertTrue(CaptureValidation.errors(request(manual.copy(signalCode="R-1"))).any {it.field == "signalCodeSource"})
        assertTrue(CaptureValidation.errors(request(manual.copy(signalDescription=""))).any {it.field == "signalDescription"})
    }

    @Test fun unidentifiedPlateNeverStoresAFakeCodeAndRetainsObservation() {
        val broken=Sic22FormState().selectSignalSource(SignalCodeSource.UNIDENTIFIABLE).copy(signalObservation="Placa desaparecida")
        assertNull(broken.persistedSignalCode)
        assertTrue(CaptureValidation.errors(request(broken)).isEmpty())
        assertEquals("SIN CÓDIGO IDENTIFICABLE",captureSummary(request(broken)).toMap()["Identificación de señal"])
        assertEquals("Placa desaparecida",captureSummary(request(broken)).toMap()["Observación de señal"])
        assertTrue(CaptureValidation.errors(request(broken.copy(signalCode="R-1"))).any {it.field == "signalCode"})
    }

    @Test fun kilometerMarkerHasNoFinalLocationAndNewCaptureIgnoresStaleEndValues() {
        val marker=Sic22FormState(typeCode="3").selectCatalogSignal("I-2A")
        assertTrue(marker.isKilometerMarker)
        val r=request(marker).copy(endPrCode="invalid historical",endDistanceM="-5",endLatitude=Double.NaN,endLongitude=0.0)
        assertFalse(requiresEndLocation(r))
        assertNull(pointCapture(r).endPrCode)
        assertTrue(CaptureValidation.errors(r).isEmpty())
        assertEquals("invalid historical",r.endPrCode)
        assertFalse(Sic22FormState(typeCode="3",signalCode="I-5").isKilometerMarker)
    }

    @Test fun savingAnExistingPointRetainsHiddenEndHistory() {
        val old=testRecord().copy(sicCode="SIC-22",endPrCode="legacy",endDistanceM=-5.0,endLatitude=-12.5,endLongitude=-77.5,
            endGpsTimestamp=123,endSuggestedPrCode="0010",endLocationSource="LEGACY")
        val fresh=old.copy(endPrCode=null,endDistanceM=null,endLatitude=null,endLongitude=null,endGpsTimestamp=null,
            endSuggestedPrCode=null,endLocationSource="MANUAL")
        val saved=preservePointHistory(fresh,old)
        assertEquals(old.endPrCode,saved.endPrCode)
        assertEquals(old.endLatitude,saved.endLatitude)
        assertEquals(old.endGpsTimestamp,saved.endGpsTimestamp)
        assertEquals(old.endSuggestedPrCode,saved.endSuggestedPrCode)
        assertEquals(old.endLocationSource,saved.endLocationSource)
        assertNull(preservePointHistory(fresh,null).endPrCode)
    }

    @Test fun matrixKeepsLinearAssetsAndPointAssetsDistinct() {
        assertEquals(InventoryLocationMode.LINEAR,locationMode("SIC-17","BRIDGE"))
        assertEquals(InventoryLocationMode.POINT,locationMode("SIC-18","CULVERT"))
        for(sic in listOf("SIC-19","SIC-21")) assertEquals(InventoryLocationMode.LINEAR,locationMode(sic))
        assertEquals(InventoryLocationMode.POINT,locationMode("SIC-20",sic20Class="12"))
        for(cls in listOf("13","14")) assertEquals(InventoryLocationMode.LINEAR,locationMode("SIC-20",sic20Class=cls))
        for(cls in listOf("21","22")) assertEquals(InventoryLocationMode.LINEAR,locationMode("SIC-23",sic23Class=cls))
        for(cls in listOf("23","24")) assertEquals(InventoryLocationMode.POINT,locationMode("SIC-23",sic23Class=cls))
    }

    @Test fun photoPolicyRetainsExistingCulvertKeysAndAllowsMultiplePerCategory() {
        val categories=PhotoCategoryPolicy.categories("SIC-18")
        assertEquals(listOf("CULVERT_PANORAMIC","CULVERT_INLET","CULVERT_OUTLET"),categories.map {it.key})
        assertEquals(listOf("PANORAMICA","ENTRADA","SALIDA"),categories.map {it.contractualName})
        assertTrue(categories.all {it.multipleAllowed})
        assertEquals("CULVERT_INLET",PhotoCategoryPolicy.category("SIC-18",stored="ENTRADA"))
        assertEquals("GENERAL",PhotoCategoryPolicy.category("SIC-18",stored=null))
        assertEquals("GENERAL",PhotoCategoryPolicy.categories("SIC-22").single().key)
    }

    @Test fun informativeSubtypeSelectsExistingMarkerCodeAndExportsPostNumber() {
        val state=Sic22FormState(typeCode="3",signalCode="I-5").selectInformativeElement(true).copy(kilometerPostNumber="0012")
        assertEquals("3",state.typeCode)
        assertEquals("I-2A",state.signalCode)
        assertTrue(state.usesKilometerPostNumber)
        assertFalse(requiresEndLocation(request(state)))
        assertTrue(SignalIdentificationPolicy.completionPending(state).isEmpty())
        val entity=Sic22Entity("r","20",state.typeCode,"2",state.signalCode,state.kilometerPostNumber,"1",null,null,null)
        val row=InventoryRecordForExport(testRecord().copy(sicCode="SIC-22"),sic22=entity)
        val values=SicExportFormat.SIC22.values(row)
        assertEquals("I-2A",values[8]);assertEquals("0012",values[9])
        val bytes=ByteArrayOutputStream().also {SicExcelWriter.write(it,listOf(row),SicExportFormat.SIC22,ExportHeading())}.toByteArray()
        var sheet=""
        ZipInputStream(bytes.inputStream()).use {zip->while(true) {
            val entry=zip.nextEntry ?: break
            if(entry.name=="xl/worksheets/sheet1.xml") sheet=zip.readBytes().toString(Charsets.UTF_8)
        }}
        assertTrue(sheet.contains("I-2A"));assertTrue(sheet.contains("0012"))
        val sign=state.selectInformativeElement(false)
        assertFalse(sign.isKilometerMarker)
        assertEquals("0012",sign.kilometerPostNumber) // Hidden historic number is retained.
    }

    @Test fun finalCompletenessAllowsUnidentifiedButFlagsMissingOrMislabeledCatalogueCode() {
        assertEquals(listOf("Código de señal"),SignalIdentificationPolicy.completionPending(Sic22FormState()))
        val unidentified=Sic22FormState(signalCodeSource=SignalCodeSource.UNIDENTIFIABLE)
        assertTrue(SignalIdentificationPolicy.completionPending(unidentified).isEmpty())
        assertTrue(SignalIdentificationPolicy.completionPending(unidentified.copy(signalCode="R-1")).isNotEmpty())
        val valid=Sic22FormState(typeCode="1",signalCode="R-1")
        assertTrue(SignalIdentificationPolicy.completionPending(valid).isEmpty())
        assertTrue(SignalIdentificationPolicy.completionPending(valid.copy(typeCode="2")).isNotEmpty())
        val marker=valid.selectInformativeElement(true)
        assertEquals(listOf("Número del poste kilométrico"),SignalIdentificationPolicy.completionPending(marker))
        val entity=Sic22Entity("r","20","1","2",null,null,"3",null,null,null,SignalCodeSource.UNIDENTIFIABLE,"","Ilegible")
        assertTrue(SignalIdentificationPolicy.completionPending(entity).isEmpty())
    }

    @Test fun choosingCurrentSourceDoesNotEraseManualCapture() {
        val manual=Sic22FormState(signalCodeSource=SignalCodeSource.MANUAL,signalCode="LOCAL",signalDescription="Placa local")
        assertEquals(manual,manual.selectSignalSource(SignalCodeSource.MANUAL))
    }
}
