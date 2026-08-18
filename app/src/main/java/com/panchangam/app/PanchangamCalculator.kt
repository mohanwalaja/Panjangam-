package com.panchangam.app

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

data class PanchangamResult(
    val samvatsaram: String,
    val ayanam: String,
    val rithu: String,
    val masam: String,
    val paksham: String,
    val thithi: String,
    val vasaram: String,
    val nakshatram: String,
    val yogam: String,
    val karanam: String
)

/**
 * Derives the 10 Panchangam elements for a given date/time and location.
 * Values are computed at the requested date's sunrise (the traditional
 * reference instant for a Panchangam "day"), following Lahiri ayanamsa.
 *
 * NOTE on scope/accuracy: Tithi, Nakshatra, Yoga, Karana, Vasara, and
 * Ayana are computed directly from Sun/Moon sidereal positions and are
 * astronomically accurate to within a few minutes. Masa (lunar month)
 * naming and Samvatsara (60-year Jupiter-cycle year name) use the
 * standard rule-of-thumb formulas below; regional traditions (Amanta vs
 * Purnimanta calendars, adhika/kshaya masa edge cases, and the exact
 * Ugadi cutover for Samvatsara) vary, so treat those two fields as good
 * approximations to refine further if you need calendar-exact precision
 * for a specific region.
 */
object PanchangamCalculator {

    private val nakshatraNames = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    )

    private val yogaNames = listOf(
        "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana", "Atiganda",
        "Sukarma", "Dhriti", "Shula", "Ganda", "Vriddhi", "Dhruva",
        "Vyaghata", "Harshana", "Vajra", "Siddhi", "Vyatipata", "Variyana",
        "Parigha", "Shiva", "Siddha", "Sadhya", "Shubha", "Shukla",
        "Brahma", "Indra", "Vaidhriti"
    )

    private val tithiNames = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashthi",
        "Saptami", "Ashtami", "Navami", "Dashami", "Ekadashi", "Dwadashi",
        "Trayodashi", "Chaturdashi", "Purnima/Amavasya"
    )

    private val karanaNames = listOf(
        "Bava", "Balava", "Kaulava", "Taitila", "Garija", "Vanija", "Vishti",
        "Shakuni", "Chatushpada", "Naga", "Kimstughna"
    )

    private val masaNames = listOf(
        "Chaitra", "Vaishakha", "Jyeshtha", "Ashadha", "Shravana", "Bhadrapada",
        "Ashwina", "Kartika", "Margashira", "Pausha", "Magha", "Phalguna"
    )

    private val rithuNames = listOf(
        "Vasanta (Spring)", "Grishma (Summer)", "Varsha (Monsoon)",
        "Sharad (Autumn)", "Hemanta (Pre-winter)", "Shishira (Winter)"
    )

    private val vasaraNames = listOf(
        "Ravivara (Sunday)", "Somavara (Monday)", "Mangalavara (Tuesday)",
        "Budhavara (Wednesday)", "Guruvara (Thursday)", "Shukravara (Friday)",
        "Shanivara (Saturday)"
    )

    // 60-year Samvatsara (Jupiter) cycle names, standard South Indian order.
    private val samvatsaraNames = listOf(
        "Prabhava", "Vibhava", "Shukla", "Pramoda", "Prajapati", "Angirasa",
        "Shrimukha", "Bhava", "Yuva", "Dhata", "Ishvara", "Bahudhanya",
        "Pramathi", "Vikrama", "Vrisha", "Chitrabhanu", "Svabhanu", "Tarana",
        "Parthiva", "Vyaya", "Sarvajit", "Sarvadhari", "Virodhi", "Vikriti",
        "Khara", "Nandana", "Vijaya", "Jaya", "Manmatha", "Durmukhi",
        "Hemalamba", "Vilambi", "Vikari", "Sharvari", "Plava", "Shubhakrit",
        "Shobhakrit", "Krodhi", "Vishvavasu", "Parabhava", "Plavanga", "Kilaka",
        "Saumya", "Sadharana", "Virodhikrit", "Paridhavi", "Pramadicha", "Ananda",
        "Rakshasa", "Nala", "Pingala", "Kalayukti", "Siddharthi", "Raudri",
        "Durmati", "Dundubhi", "Rudhirodgari", "Raktakshi", "Krodhana", "Akshaya"
    )

    fun calculate(
        year: Int, month: Int, day: Int,
        latitude: Double, longitude: Double, utcOffsetHours: Double
    ): PanchangamResult {

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        // Must be 0h UTC (midnight), NOT any other hour. The sunrise
        // equation in AstroUtils expects the Julian Day anchored at 0h UT
        // for this calendar date. Using any other hour here introduces a
        // systematic offset (e.g. setting 6:00 here shifts every result
        // by exactly 6 hours).
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val jdMidday = AstroUtils.toJulianDay(cal)
        val sunriseHour = AstroUtils.sunriseHourLocal(jdMidday, latitude, longitude, utcOffsetHours)

        // Build the exact sunrise instant (in UTC) for this date/location.
        val sunriseCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        sunriseCal.set(year, month - 1, day, 0, 0, 0)
        sunriseCal.set(Calendar.MILLISECOND, 0)
        val sunriseUtcHour = sunriseHour - utcOffsetHours
        sunriseCal.add(Calendar.MILLISECOND, (sunriseUtcHour * 3600 * 1000).toInt())
        val jdSunrise = AstroUtils.toJulianDay(sunriseCal)

        val sunLong = AstroUtils.sunSiderealLongitude(jdSunrise)
        val moonLong = AstroUtils.moonSiderealLongitude(jdSunrise)

        // --- Tithi ---
        var diff = moonLong - sunLong
        if (diff < 0) diff += 360.0
        val tithiIndex = floor(diff / 12.0).toInt().coerceIn(0, 29)
        val paksha = if (tithiIndex < 15) "Shukla Paksha" else "Krishna Paksha"
        val tithiInPaksha = tithiIndex % 15
        val tithiName = tithiNames[tithiInPaksha.coerceIn(0, 14)]

        // --- Nakshatra ---
        val nakshatraSpan = 360.0 / 27.0
        val nakshatraIndex = floor(moonLong / nakshatraSpan).toInt().coerceIn(0, 26)
        val nakshatraName = nakshatraNames[nakshatraIndex]

        // --- Yoga ---
        var yogaSum = sunLong + moonLong
        if (yogaSum >= 360.0) yogaSum -= 360.0
        val yogaSpan = 360.0 / 27.0
        val yogaIndex = floor(yogaSum / yogaSpan).toInt().coerceIn(0, 26)
        val yogaName = yogaNames[yogaIndex]

        // --- Karana (half-tithi, 11 repeating karanas across the month) ---
        val karanaNumber = floor(diff / 6.0).toInt().coerceIn(0, 59)
        val karanaName = when {
            karanaNumber == 0 -> karanaNames[10]        // Kimstughna
            karanaNumber in 1..56 -> karanaNames[(karanaNumber - 1) % 7]
            karanaNumber == 57 -> karanaNames[7]         // Shakuni
            karanaNumber == 58 -> karanaNames[8]         // Chatushpada
            else -> karanaNames[9]                        // Naga
        }

        // --- Vasara (weekday), counted sunrise-to-sunrise ---
        val weekdayCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        weekdayCal.timeInMillis = sunriseCal.timeInMillis
        val dayOfWeek = weekdayCal.get(Calendar.DAY_OF_WEEK) // 1=Sunday
        val vasaraName = vasaraNames[(dayOfWeek - 1).coerceIn(0, 6)]

        // --- Ayana (Uttarayana / Dakshinayana) based on Sun's sidereal
        // longitude relative to Makara (Capricorn, 270°) and Karka
        // (Cancer, 90°) sankranti points.
        val ayanaName = if (sunLong in 270.0..360.0 || sunLong in 0.0..90.0) {
            "Uttarayana"
        } else {
            "Dakshinayana"
        }

        // --- Masa: named from the sidereal rashi (zodiac sign) occupied
        // by the Sun, which closely tracks the lunar month name in the
        // Amanta system for most of the year.
        val rashiIndex = floor(sunLong / 30.0).toInt().coerceIn(0, 11)
        // Chaitra masa begins near Sun entering Meena/Mesha; offset the
        // rashi index so index 0 (Mesha) aligns with Chaitra/Vaishakha
        // transition used by most Panchangams.
        val masaIndex = (rashiIndex + 11) % 12
        val masaName = masaNames[masaIndex]

        // --- Ritu (season): pairs of masas, 2 per ritu ---
        val rituIndex = (masaIndex / 2).coerceIn(0, 5)
        val rituName = rithuNames[rituIndex]

        // --- Samvatsara: 60-year Jupiter cycle. Uses the common
        // approximation: Saka year = Gregorian year - 78 (adjusted near
        // the Gregorian new year, since the Hindu year starts around
        // March/April); index = (Saka year + 12) mod 60 in the commonly
        // used South Indian reckoning.
        val sakaYear = if (month >= 4) year - 78 else year - 79
        val samvatsaraIndex = ((sakaYear + 12) % 60 + 60) % 60
        val samvatsaraName = samvatsaraNames[samvatsaraIndex]

        return PanchangamResult(
            samvatsaram = samvatsaraName,
            ayanam = ayanaName,
            rithu = rituName,
            masam = masaName,
            paksham = paksha,
            thithi = tithiName,
            vasaram = vasaraName,
            nakshatram = nakshatraName,
            yogam = yogaName,
            karanam = karanaName
        )
    }
}
