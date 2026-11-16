package fr.croustillapp.visual.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.croustillapp.R

@Composable
fun <T> CustomDropDownMenu(
    modifier: Modifier = Modifier,
    selectedValue: T,
    options: List<T>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (T) -> Unit,
    labelProvider: @Composable (T) -> Unit,
    dropdownItemLabelProvider: @Composable (T) -> Unit = labelProvider,
    isDefaultOption: (T) -> Boolean = { false }
) {
    Box(modifier = modifier) {
        CustomFilter(
            selected = !isDefaultOption(selectedValue),
            onClick = { onExpandedChange(!expanded) },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ddm),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { labelProvider(selectedValue) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            options.forEach { option ->
                val isSelected = (selectedValue == option)
                val isDefault = isDefaultOption(option)

                DropdownMenuItem(
                    text = {
                        val textWeight = if (isDefault) FontWeight.Bold else FontWeight.Normal

                        ProvideTextStyle(
                            value = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = textWeight
                            )
                        ) {
                            dropdownItemLabelProvider(option)
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_visual_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}