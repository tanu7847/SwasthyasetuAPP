package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NearbyHospital
import com.example.data.model.SymptomIntake
import com.example.ui.theme.*

@Composable
fun EmergencyHospitalsScreen(
    latestTriage: SymptomIntake?,
    nearbyHospitals: List<NearbyHospital>,
    userLatitude: Double,
    userLongitude: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Emergency Alert Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "CRITICAL EMERGENCY PROTOCOL",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Emergency services dispatched / Reach nearest hospital immediately",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_112_button")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = EmergencyRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call 112 SOS", color = EmergencyRed, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_108_button")
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = EmergencyRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call 108 Ambulance", color = EmergencyRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Primary Healthcare & Ayurvedic Homecare Survival Tips
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = SuccessGreenText
                        )
                        Text(
                            text = "First-Aid & Ayurvedic Survival Tips (Until Hospital Arrival)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = TealPrimaryDark
                        )
                    }

                    val tips = latestTriage?.ayurvedicHomeCareTips ?: listOf(
                        "Sit upright in 90° high Fowler's posture to maintain maximum lung oxygenation.",
                        "Loosen all tight clothing, collar buttons, and waistbands immediately.",
                        "If bleeding: Apply continuous firm compression with clean sterile cloth; elevate limb above heart level.",
                        "Keep patient calm, warm, and conscious. Do not administer solid food or heavy Kashaya during acute choking/fainting.",
                        "For high fever: Continuous room-temperature forehead cloth sponging."
                    )

                    tips.forEach { tip ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("🌿", fontSize = 14.sp)
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // GPS Nearby Hospitals Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GPS Nearby Hospitals & Trauma Centers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Current GPS Coordinates: ${String.format("%.4f", userLatitude)}, ${String.format("%.4f", userLongitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                }

                Surface(
                    color = TealPrimaryLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${nearbyHospitals.size} Centers Found",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List of Nearby Hospitals
        items(nearbyHospitals) { hospital ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hospital_card_${hospital.distanceKm}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hospital.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = hospital.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        }

                        Surface(
                            color = ClinicalBlueContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${hospital.distanceKm} km",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ClinicalBlueOnContainer
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bed,
                                contentDescription = null,
                                tint = SuccessGreenText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${hospital.icuBedsAvailable} ICU Beds",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreenText
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "24/7 Emergency Wing",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary
                            )
                        }
                    }

                    Divider(color = SlateBorder)

                    // Action Buttons: Call & Maps Directions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call Hospital", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${hospital.latitude},${hospital.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Directions", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
