/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.model.geofence

/** The Świętokrzyskie voivodeship, shared by the warnings that only make sense inside our region. */
object SwietokrzyskieRegion {
    /**
     * Simplified border polygon for Świętokrzyskie voivodeship (52 vertices, Douglas-Peucker simplified from the
     * ppatrzyk/polska-geojson dataset) — (lat, lon) pairs, connects back to the first vertex.
     */
    @Suppress("MagicNumber")
    val polygon =
        GeofencePolygon(
            listOf(
                50.8660 to 19.7471,
                50.9357 to 19.8488,
                51.0469 to 19.8752,
                50.9726 to 20.0356,
                51.0577 to 20.0562,
                51.0731 to 19.9818,
                51.1647 to 20.0258,
                51.1840 to 19.9941,
                51.2013 to 20.2324,
                51.2588 to 20.2605,
                51.2366 to 20.3711,
                51.3105 to 20.3644,
                51.3394 to 20.4328,
                51.3314 to 20.5072,
                51.2304 to 20.5460,
                51.1960 to 20.7003,
                51.1490 to 20.6974,
                51.1544 to 20.8798,
                51.1953 to 20.9207,
                51.1445 to 20.9985,
                51.1571 to 21.0566,
                51.2114 to 21.0700,
                51.1887 to 21.1199,
                51.1551 to 21.0912,
                51.0803 to 21.1528,
                51.0853 to 21.3555,
                51.0131 to 21.4636,
                51.0590 to 21.5291,
                51.0780 to 21.6763,
                51.0374 to 21.7521,
                51.0721 to 21.8030,
                50.8136 to 21.8690,
                50.6451 to 21.7801,
                50.6454 to 21.7226,
                50.5212 to 21.6040,
                50.4941 to 21.4541,
                50.3298 to 21.1766,
                50.2896 to 20.7899,
                50.1866 to 20.5724,
                50.2160 to 20.3746,
                50.2415 to 20.3975,
                50.3280 to 20.2925,
                50.3623 to 20.3372,
                50.4894 to 20.2205,
                50.5545 to 19.7887,
                50.6394 to 19.9222,
                50.6514 to 19.8352,
                50.6930 to 19.8717,
                50.7254 to 19.8108,
                50.7292 to 19.7129,
                50.8250 to 19.8257,
                50.8660 to 19.7471,
            ),
        )
}
