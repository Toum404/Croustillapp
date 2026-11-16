package fr.croustillapp.visual.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens

@Composable
fun CustomFilter(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(AppDimens.cornerSmall),
    leadingIcon: @Composable (() -> Unit)? = {
        if (selected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_visual_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    },
    trailingIcon: @Composable (() -> Unit)? = null,
    label: @Composable () -> Unit
) {
    val chipBorder = FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = if (enabled) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
        selectedBorderColor = Color.Transparent,
        borderWidth = if (enabled) 1.dp else 0.dp
    )

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.background,
        labelColor = MaterialTheme.colorScheme.onBackground,
        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(36.dp),
        enabled = enabled,
        shape = shape,
        border = chipBorder,
        colors = chipColors,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = label
    )
}