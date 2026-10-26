package org.secretarrow.noiddroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A Brutalist panel — heavy border, single accent corner, no shadows.
 * The bottom-right corner is missing a piece of border to create a
 * visual "notch" that mirrors the splash logo's crosshair.
 */
@Composable
fun BrutalPanel(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg =
        if (accent) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
    val fg =
        if (accent) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val border =
        if (accent) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }

    Box(
        modifier =
            modifier
                .background(bg, RoundedCornerShape(0.dp))
                .border(
                    BorderStroke(2.dp, border),
                    RoundedCornerShape(0.dp),
                ).padding(0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            content()
        }
        // Decorative crosshair mark in the corner
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            Box(Modifier.size(12.dp).background(fg))
        }
    }
    // Squelch unused-colour warning
    @Suppress("unused")
    val _ = Color.Transparent
}

/** Section header — large, bold, all-caps. */
@Composable
fun BrutalSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        modifier = modifier,
    )
}

/** Stat block — large number above a small label. */
@Composable
fun BrutalStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
