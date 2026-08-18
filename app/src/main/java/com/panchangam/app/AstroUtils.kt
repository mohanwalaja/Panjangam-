package com.panchangam.app

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Core astronomical routines needed for Panchangam calculation:
 *  - Julian Day conversion
 *  - Geocentric apparent longitude of the Sun (tropical)
 *  - Geocentric apparent longitude of the Moon (tropical)
 *  - Lahiri ayanamsa (to convert tropical -> sidereal, the standard used
 *    for Vedic Panchangam)
 *  - Approximate local sunrise time
 *
 * Sun/Moon formulas follow the well-known low-precision series published
 * by Jean Meeus ("Astronomical Algorithms"). These give sub-arcminute
 * accuracy for the Sun and a few arcminutes for the Moon, which is more
 * than sufficient for Panchangam (tithi/nakshatra/yoga/karana) purposes.
 * All constants below are standard published astronomical constants, not
 * copied from any single app's source code.
 */
object AstroUtils {

    private const val DEG2RAD = PI / 180.0
    private const val RAD2DEG = 180.0 / PI

    /** Julian Day Number (UT) for a given UTC calendar instant. */
    fun toJulianDay(cal: Calendar): Double {
        val utc = cal.clone() as Calendar
        utc.timeZone = TimeZone.getTimeZone("UTC")

        var year = utc.get(Calendar.YEAR)
        var month = utc.get(Calendar.MONTH) + 1
        val day = utc.get(Calendar.DAY_OF_MONTH)
        val hour = utc.get(Calendar.HOUR_OF_DAY)
        val minute = utc.get(Calendar.MINUTE)
        val second = utc.get(Calendar.SECOND)

        val dayFraction = (hour + minute / 60.0 + second / 3600.0) / 24.0

        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)

        val jd = floor(365.25 * (year + 4716)) +
                floor(30.6001 * (month + 1)) +
                day + dayFraction + b - 1524.5
        return jd
    }

    private fun normalizeDeg(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    /** Geocentric apparent tropical longitude of the Sun, in degrees. */
    fun sunApparentLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0

        val l0 = normalizeDeg(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        val m = normalizeDeg(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mRad = m * DEG2RAD

        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2 * mRad) +
                0.000289 * sin(3 * mRad)

        val trueLong = l0 + c
        val omega = 125.04 - 1934.136 * t
        val apparentLong = trueLong - 0.00569 - 0.00478 * sin(omega * DEG2RAD)

        return normalizeDeg(apparentLong)
    }

    /**
     * Geocentric apparent tropical longitude of the Moon, in degrees.
     * Truncated Meeus/ELP2000 series (major periodic terms) — accurate
     * to within a few arcminutes, which resolves tithi/nakshatra/yoga
     * boundaries to within roughly a minute or two of real time.
     */
    fun moonApparentLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0

        val lp = normalizeDeg(218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841.0)
        val d = normalizeDeg(297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0)
        val m = normalizeDeg(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0)
        val mp = normalizeDeg(134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0)
        val f = normalizeDeg(93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000.0)

        val dR = d * DEG2RAD
        val mR = m * DEG2RAD
        val mpR = mp * DEG2RAD
        val fR = f * DEG2RAD

        // Major periodic terms for longitude (coefficients in 1e-6 degrees,
        // arguments as multiples of D, M, M', F). This is a truncated but
        // representative subset of the full ~60-term series.
        var sumL = 0.0
        sumL += 6288774 * sin(mpR)
        sumL += 1274027 * sin(2 * dR - mpR)
        sumL += 658314 * sin(2 * dR)
        sumL += 213618 * sin(2 * mpR)
        sumL += -185116 * sin(mR)
        sumL += -114332 * sin(2 * fR)
        sumL += 58793 * sin(2 * dR - 2 * mpR)
        sumL += 57066 * sin(2 * dR - mR - mpR)
        sumL += 53322 * sin(2 * dR + mpR)
        sumL += 45758 * sin(2 * dR - mR)
        sumL += -40923 * sin(mR - mpR)
        sumL += -34720 * sin(dR)
        sumL += -30383 * sin(mR + mpR)
        sumL += 15327 * sin(2 * dR - 2 * fR)
        sumL += -12528 * sin(mpR + 2 * fR)
        sumL += 10980 * sin(mpR - 2 * fR)
        sumL += 10675 * sin(4 * dR - mpR)
        sumL += 10034 * sin(3 * mpR)
        sumL += 8548 * sin(4 * dR - 2 * mpR)
        sumL += -7888 * sin(2 * dR + mR - mpR)
        sumL += -6766 * sin(2 * dR + mR)
        sumL += -5163 * sin(dR - mpR)
        sumL += 4987 * sin(dR + mR)
        sumL += 4036 * sin(2 * dR - mR + mpR)
        sumL += 3994 * sin(4 * dR)
        sumL += 3861 * sin(4 * dR - mR - mpR)
        sumL += 3665 * sin(4 * dR - mR)
        sumL += -2689 * sin(mR - 2 * mpR)
        sumL += -2602 * sin(2 * dR - 2 * fR)

        val longitude = lp + sumL / 1_000_000.0

        // Nutation correction (small)
        val omega = normalizeDeg(125.04452 - 1934.136261 * t)
        val nutation = -17.20 / 3600.0 * sin(omega * DEG2RAD)

        return normalizeDeg(longitude + nutation)
    }

    /**
     * Lahiri (Chitrapaksha) ayanamsa in degrees for the given Julian Day.
     * Standard reference used for almost all Vedic Panchangam software.
     */
    fun lahiriAyanamsa(jd: Double): Double {
        // Linear fit calibrated to the official Lahiri ayanamsa value of
        // 23.85333 degrees at J2000.0, precessing at ~50.29"/year.
        // Accurate to a few arcseconds over 1900-2100, which is far
        // smaller than one Panchangam segment (12-13 degrees).
        val yearsSince2000 = (jd - 2451545.0) / 365.2422
        return 23.85333 + 0.013972 * yearsSince2000
    }

    fun sunSiderealLongitude(jd: Double): Double =
        normalizeDeg(sunApparentLongitude(jd) - lahiriAyanamsa(jd))

    fun moonSiderealLongitude(jd: Double): Double =
        normalizeDeg(moonApparentLongitude(jd) - lahiriAyanamsa(jd))

    /**
     * Approximate local mean sunrise time (returns hour-of-day, 0-24, in
     * local civil time for the given UTC-offset-hours) using the standard
     * sunrise equation. Accurate to within a minute or two, which is
     * sufficient for determining which sunrise-to-sunrise day a Panchangam
     * reading applies to.
     */
    fun sunriseHourLocal(jd: Double, latitudeDeg: Double, longitudeDeg: Double, utcOffsetHours: Double): Double {
        // Standard NOAA/Wikipedia sunrise-equation convention: "lw" is
        // longitude measured POSITIVE WEST (so east longitudes, like
        // India's, must be negated here). Using east-positive directly
        // without negating was the root cause of large time errors.
        val lw = -longitudeDeg

        // n must be a whole number of days — the integer count of solar
        // transits since J2000, rounded to the nearest day. Because this
        // is rounded, the exact hour-of-day used to build `jd` no longer
        // matters (any time within the same UTC calendar day rounds to
        // the same n), which also eliminates the earlier bug where
        // feeding in a non-midnight reference time shifted every result.
        val n = Math.round(jd - 2451545.0009 - lw / 360.0).toDouble()

        val jMean = 2451545.0009 + lw / 360.0 + n
        val mDeg = normalizeDeg(357.5291 + 0.98560028 * jMean)
        val mRad = mDeg * DEG2RAD
        val center = 1.9148 * sin(mRad) + 0.0200 * sin(2 * mRad) + 0.0003 * sin(3 * mRad)
        val lambdaDeg = normalizeDeg(mDeg + 102.9372 + center + 180.0)
        val lambdaRad = lambdaDeg * DEG2RAD

        val jTransit = jMean + 0.0053 * sin(mRad) - 0.0069 * sin(2 * lambdaRad)

        val sinDec = sin(lambdaRad) * sin(23.4397 * DEG2RAD)
        val decRad = asin(sinDec)
        val latRad = latitudeDeg * DEG2RAD

        val cosOmega = (sin((-0.833) * DEG2RAD) - sin(latRad) * sinDec) / (cos(latRad) * cos(decRad))
        val cosOmegaClamped = cosOmega.coerceIn(-1.0, 1.0)
        val omegaDeg = acos(cosOmegaClamped) * RAD2DEG

        val jRise = jTransit - omegaDeg / 360.0

        val fractionalDay = jRise - floor(jRise) + 0.5
        var hourUtc = (fractionalDay % 1.0) * 24.0
        if (hourUtc < 0) hourUtc += 24.0

        var hourLocal = hourUtc + utcOffsetHours
        if (hourLocal < 0) hourLocal += 24.0
        if (hourLocal >= 24) hourLocal -= 24.0
        return hourLocal
    }

    /** Converts a Julian Day to a UTC epoch millisecond timestamp. */
    fun jdToEpochMillis(jd: Double): Long = ((jd - 2440587.5) * 86400000.0).toLong()

    /** Wraps a degree difference into the range (-180, 180]. */
    private fun signedDeg(deg: Double): Double {
        var d = (deg + 180.0) % 360.0
        if (d < 0) d += 360.0
        return d - 180.0
    }

    /**
     * Finds the Julian Day at which `valueFn` (a slowly-changing degree
     * quantity, e.g. Moon's longitude) crosses `boundaryDeg`, searching
     * from `startJd` in the given direction (-1 = backward in time,
     * +1 = forward). Used to find exact Tithi/Nakshatra start and end
     * times. Coarse-steps to bracket the crossing, then refines with
     * bisection to sub-minute precision.
     */
    fun findCrossingJd(
        startJd: Double, boundaryDeg: Double, direction: Int, valueFn: (Double) -> Double
    ): Double {
        val stepDays = 0.02 // ~29 minutes
        var t0 = startJd
        var f0 = signedDeg(valueFn(t0) - boundaryDeg)
        var t1 = t0
        var f1 = f0
        var iterations = 0
        while (iterations < 400) {
            t1 = t0 + direction * stepDays
            f1 = signedDeg(valueFn(t1) - boundaryDeg)
            if (f0 == 0.0 || (f0 < 0) != (f1 < 0)) break
            t0 = t1
            f0 = f1
            iterations++
        }

        var lo = t0
        var hi = t1
        var fLo = f0
        repeat(60) {
            val mid = (lo + hi) / 2.0
            val fMid = signedDeg(valueFn(mid) - boundaryDeg)
            if ((fLo < 0) == (fMid < 0)) {
                lo = mid
                fLo = fMid
            } else {
                hi = mid
            }
        }
        return (lo + hi) / 2.0
    }
}
