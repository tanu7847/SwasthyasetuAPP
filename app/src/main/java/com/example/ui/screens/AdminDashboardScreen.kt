package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DashboardAnalytics
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    analytics: DashboardAnalytics,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TealPrimaryDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        Column {
                            Text(
                                text = "SwasthyaSetu Analytics Command",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "System Telehealth & Triage Operations Overview",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_admin_stats_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            }
        }

        // 4 Primary KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Patient Visits",
                    value = "${analytics.totalScreenings}",
                    subtitle = "+${analytics.screeningsToday} today",
                    icon = Icons.Default.People,
                    color = TealPrimary,
                    containerColor = TealPrimaryLight,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Completed Treatments",
                    value = "${analytics.completedReferrals}",
                    subtitle = "${analytics.openReferrals} in progress",
                    icon = Icons.Default.TaskAlt,
                    color = SuccessGreenText,
                    containerColor = SuccessGreenContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Teleconsultations",
                    value = "${analytics.totalConsultations}",
                    subtitle = "${analytics.consultationsToday} conducted today",
                    icon = Icons.Default.VideoCall,
                    color = ClinicalBlueOnContainer,
                    containerColor = ClinicalBlueContainer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Critical Triage (SOS)",
                    value = "${analytics.criticalEmergencyCount}",
                    subtitle = "Hospital diverted 112/108",
                    icon = Icons.Default.Emergency,
                    color = EmergencyRedText,
                    containerColor = EmergencyRedContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Each Day Patient Count Chart / Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Each Day Count of Patients",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "7-Day Patient Visits to Online Application",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextSecondary
                            )
                        }

                        Surface(
                            color = TealPrimaryLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Realtime Sync",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interactive Daily Bars
                    val maxVal = analytics.dailyVisitsMap.values.maxOrNull() ?: 20
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        analytics.dailyVisitsMap.forEach { (day, count) ->
                            val heightFraction = (count.toFloat() / maxVal.toFloat()).coerceIn(0.15f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .width(22.dp)
                                        .background(
                                            if (day.contains("Today") || day.endsWith(")")) TealPrimary else ClinicalBlue,
                                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )

                                Text(
                                    text = day.split(" ").firstOrNull() ?: day,
                                    fontSize = 10.sp,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Triage Management & Referral Breakdown Card
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
                    Text(
                        text = "Clinical Triage & Specialist Referral Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Routine Teleconsultation Cases:", style = MaterialTheme.typography.bodySmall)
                        Text("${analytics.totalScreenings - analytics.criticalEmergencyCount}", fontWeight = FontWeight.Bold, color = SuccessGreenText)
                    }
                    LinearProgressIndicator(
                        progress = { 0.82f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = SuccessGreen,
                        trackColor = SlateBorder
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Critical Emergency Cases (Hospital Diverted):", style = MaterialTheme.typography.bodySmall)
                        Text("${analytics.criticalEmergencyCount}", fontWeight = FontWeight.Bold, color = EmergencyRedText)
                    }
                    LinearProgressIndicator(
                        progress = { 0.18f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = EmergencyRed,
                        trackColor = SlateBorder
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Specialist Referrals Completed Treatment:", style = MaterialTheme.typography.bodySmall)
                        Text("${analytics.completedReferrals} (${analytics.openReferrals} ongoing)", fontWeight = FontWeight.Bold, color = TealPrimary)
                    }
                    LinearProgressIndicator(
                        progress = { 0.74f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = TealPrimary,
                        trackColor = SlateBorder
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(text = subtitle, fontSize = 10.sp, color = color.copy(alpha = 0.85f))
        }
    }
}
