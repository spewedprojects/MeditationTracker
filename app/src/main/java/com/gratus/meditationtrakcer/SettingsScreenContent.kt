package com.gratus.meditationtrakcer// --- COMPOSABLE FUNCTIONS (Ideally place these outside the Activity class) ---

import android.R.attr.fontFamily
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SettingsScreenContent() {
    var showYAxis by remember { mutableStateOf(true) }
    var useCustomFont by remember { mutableStateOf(false) }

    // ERROR FIX: Changed 'Table.Column' to standard Compose 'Column'
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        // ERROR FIX: 'Arrangement' is now correctly imported from foundation.layout
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GuideSection()

        GlobalSettingsSection(
            showYAxis = showYAxis,
            onYAxisChange = { showYAxis = it },
            useCustomFont = useCustomFont,
            onFontChange = { useCustomFont = it }
        )
    }
}

@Composable
fun GuideSection() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.primary),
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = colorResource(id = R.color.black_white))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Usage Guide",
                    color = colorResource(id = R.color.black_white),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorResource(id = R.color.black_white)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Hidden Shortcuts",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorResource(id = R.color.black_white),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Long-press on the Streak card to set streak goals and to view the streak stats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Long-press on `Add Manually` button to open the dialog to add back-dated entry.\\n\\nNote: All future dates are disabled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalSettingsSection(
    showYAxis: Boolean,
    onYAxisChange: (Boolean) -> Unit,
    useCustomFont: Boolean,
    onFontChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
        containerColor = colorResource(id = R.color.primary),
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = colorResource(id = R.color.black_white)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Global App Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Setting 1: Y-Axis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Graph Y-Axis", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Show vertical axis values on charts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showYAxis,
                    onCheckedChange = onYAxisChange
                )
            }

            // Setting 2: Font
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "App Font", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (useCustomFont) "Using Custom Font" else "Using System Font",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useCustomFont,
                    onCheckedChange = onFontChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
    }
}