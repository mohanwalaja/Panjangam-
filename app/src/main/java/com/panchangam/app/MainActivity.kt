package com.panchangam.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.panchangam.app.databinding.ActivityMainBinding
import com.panchangam.app.databinding.RowElementBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Default location: Chennai, India. Overwritten if user grants
    // location permission and taps "Use My Location".
    private var latitude = 13.0827
    private var longitude = 80.2707
    private var locationLabel = "Chennai, India (default)"

    private var selectedCalendar: Calendar = Calendar.getInstance()

    private val locationPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.prevDayButton.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, -1)
            refresh()
        }
        binding.nextDayButton.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1)
            refresh()
        }
        binding.todayButton.setOnClickListener {
            selectedCalendar = Calendar.getInstance()
            refresh()
        }
        binding.locationButton.setOnClickListener {
            requestLocationAndRefresh()
        }

        refresh()
    }

    private fun requestLocationAndRefresh() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequestCode
            )
            return
        }
        fetchDeviceLocation()
    }

    private fun fetchDeviceLocation() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    locationLabel = "Lat ${"%.3f".format(latitude)}, Lon ${"%.3f".format(longitude)}"
                    refresh()
                }
            }
        } catch (e: SecurityException) {
            // Permission was revoked between the check and this call; ignore.
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchDeviceLocation()
        }
    }

    private fun refresh() {
        val utcOffsetHours = TimeZone.getDefault().getOffset(selectedCalendar.timeInMillis) / 3600000.0

        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH) + 1
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val result = PanchangamCalculator.calculate(
            year, month, day, latitude, longitude, utcOffsetHours
        )

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.dateText.text = dateFormat.format(selectedCalendar.time)
        binding.locationLabel.text = "Location: $locationLabel"

        setRow(binding.rowSamvatsaram, "Samvatsaram", result.samvatsaram)
        setRow(binding.rowAyanam, "Ayanam", result.ayanam)
        setRow(binding.rowRithu, "Rithu", result.rithu)
        setRow(binding.rowMasam, "Masam", result.masam)
        setRow(binding.rowPaksham, "Paksham", result.paksham)
        setRow(binding.rowThithi, "Thithi", "${result.thithi}\n(${result.thithiStart} - ${result.thithiEnd})")
        setRow(binding.rowVasaram, "Vasaram", result.vasaram)
        setRow(binding.rowNakshatram, "Nakshatram", "${result.nakshatram}\n(${result.nakshatramStart} - ${result.nakshatramEnd})")
        setRow(binding.rowYogam, "Yogam", result.yogam)
        setRow(binding.rowKaranam, "Karanam", result.karanam)
    }

    private fun setRow(row: RowElementBinding, label: String, value: String) {
        row.labelText.text = label
        row.valueText.text = value
    }
}
