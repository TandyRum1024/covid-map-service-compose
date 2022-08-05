package com.vin.covidmapservice.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vin.covidmapservice.domain.Center
import com.vin.covidmapservice.dummyCenterData
import com.vin.covidmapservice.isInPreview
import com.vin.covidmapservice.ui.theme.CovidMapServiceTheme

// Vaccination center information display
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CenterDisplay(
    // (dummy center data for preview purposes)
    center: Center = dummyCenterData
) {
    Column {
        // Titlecard: Center name and facility name
        Surface(
            color = MaterialTheme.colors.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column (modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(center.centerName, style = MaterialTheme.typography.h6)
                Text(center.facilityName, style = MaterialTheme.typography.caption)
            }
        }
        Spacer(modifier = Modifier.size(width = 0.dp, height = 8.dp))
        ListItem(text = { Text(center.address) }, icon = { Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colors.primary) })
        ListItem(text = { Text(center.phoneNumber) }, icon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colors.primary) })
        ListItem(
            text = { Text("Last Updated") },
            secondaryText = { Text(center.updatedAt) },
            icon = { Icon(Icons.Filled.DateRange, null, tint = MaterialTheme.colors.primary) })
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun CenterDisplayPreview() {
    CovidMapServiceTheme {
        CompositionLocalProvider(isInPreview provides true) { // Notify the preview state
            CenterDisplay()
        }
    }
}