/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.converters

import com.scape.pixscape.TestUtils.ROUTE_SECTION_ONE
import com.scape.pixscape.TestUtils.location_beginning
import com.scape.pixscape.TestUtils.location_end
import com.scape.pixscape.TestUtils.routeSectionOffice
import com.scape.pixscape.models.dto.RouteSection
import junit.framework.Assert.assertEquals
import org.junit.Test

class DataConverterTest {
    private val dataConverter = DataConverter()

    @Test
    fun fromEmptyRouteSectionDTOList() {
        val emptyRouteSection = ArrayList<RouteSection>(0)

        val routeString =  dataConverter.fromRouteSectionDTOList(emptyRouteSection)
        assertEquals("[]", routeString)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun toEmptyRouteSectionStringDTOList() {
         dataConverter.toRouteSectionDTOList("")
    }

    @Test
    fun fromRouteSectionDTOList() {
        val routeSections = ArrayList<RouteSection>(1)
        val section = RouteSection(location_beginning, location_end)
        routeSections.add(section)

        val routeString = dataConverter.fromRouteSectionDTOList(routeSections)
        // distance will be 0 because Location depends on Android platform.
        assertEquals(ROUTE_SECTION_ONE, routeString)
    }

    @Test
    fun toRouteSectionDTOList() {
        val routeSectionList = dataConverter.toRouteSectionDTOList(routeSectionOffice)
        val routeSections = ArrayList<RouteSection>(1)
        routeSections.add(RouteSection(location_beginning, location_end))

        assertEquals(routeSections, routeSectionList)
    }

}