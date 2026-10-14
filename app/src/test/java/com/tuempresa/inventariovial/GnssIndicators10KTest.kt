package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.location.GeoLocation
import org.junit.Assert.*
import org.junit.Test

class GnssIndicators10KTest {
    private val now=100000L
    private val segment=RoadSegment("TEST","UC","one",listOf(RoadPolylinePoint("TEST","UC","one",0,-12.0,-77.0,0.0),RoadPolylinePoint("TEST","UC","one",1,-12.0,-76.99,1000.0)))
    private val fix=GeoLocation(-12.00005,-76.995,2f,timestamp=now)
    @Test fun validFreshFixReportsPerpendicularDistanceAndDeviceAccuracyWithoutChangingData() {
        val before=segment.copy(points=segment.points.toList());val out=GnssRouteIndicators.evaluate(fix,listOf(segment),MatchConfig(),now)
        assertEquals(2.0,out.accuracyM!!,0.0);assertEquals(5.56,out.distanceM!!,.1);assertEquals(before,segment)
    }
    @Test fun manualUnknownInvalidOldFutureAndImpreciseFixesNeverClaimDistance() {
        val variants=listOf(fix.copy(source="MANUAL_COORDINATES"),fix.copy(source=""),fix.copy(timestamp=0),fix.copy(timestamp=now-30001),fix.copy(timestamp=now+1),fix.copy(accuracyHorizontal=99f),fix.copy(accuracyHorizontal=Float.NaN),fix.copy(accuracyHorizontal=0f),fix.copy(latitude=Double.NaN))
        for(f in variants)assertNull(f.toString(),GnssRouteIndicators.evaluate(f,listOf(segment),MatchConfig(),now).distanceM)
        assertNull(GnssRouteIndicators.evaluate(fix,emptyList(),MatchConfig(),now).distanceM)
    }
    @Test fun distantAndAmbiguousCrossingDoNotPublishAMisleadingDistance() {
        assertNull(GnssRouteIndicators.evaluate(fix.copy(latitude=-12.01),listOf(segment),MatchConfig(),now).distanceM)
        val crossed=segment.copy(segmentId="cross",points=segment.points.map{it.copy(segmentId="cross",chainageM=it.chainageM+2000)})
        assertNull(GnssRouteIndicators.evaluate(fix,listOf(segment,crossed),MatchConfig(),now).distanceM)
    }
}
