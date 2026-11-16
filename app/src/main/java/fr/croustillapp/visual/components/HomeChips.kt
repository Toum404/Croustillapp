package fr.croustillapp.visual.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.data.translator.getTranslationForType
import fr.croustillapp.visual.restaurant.RestaurantViewModel

@Composable
fun HomeFilterRow(
    showOnlyOpen: Boolean,
    showOnlyPmr: Boolean,
    showOnlyIzly: Boolean,
    selectedRegion: String,
    selectedType: String,
    regionsList: List<String>,
    typesList: List<String>,
    isRegionMenuExpanded: Boolean,
    isTypeMenuExpanded: Boolean,
    onRegionMenuExpandedChange: (Boolean) -> Unit,
    onTypeMenuExpandedChange: (Boolean) -> Unit,
    viewModel: RestaurantViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                CustomFilter(
                    selected = showOnlyOpen,
                    onClick = { viewModel.toggleOnlyOpen(!showOnlyOpen) },
                    label = { Text(text = stringResource(id = R.string.bs_statut_on), maxLines = 1) }
                )
            }
            item {
                CustomFilter(
                    selected = showOnlyPmr,
                    onClick = { viewModel.toggleOnlyPmr(!showOnlyPmr) },
                    label = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_filtre_pmr),
                            contentDescription = stringResource(id = R.string.statut_pmr),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
            item {
                CustomFilter(
                    selected = showOnlyIzly,
                    onClick = { viewModel.toggleOnlyIzly(!showOnlyIzly) },
                    label = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_filtre_izly),
                            contentDescription = stringResource(id = R.string.statut_izly),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
            item {
                CustomDropDownMenu(
                    selectedValue = selectedRegion,
                    options = regionsList,
                    expanded = isRegionMenuExpanded,
                    onExpandedChange = onRegionMenuExpandedChange,
                    onOptionSelected = { viewModel.updateRegion(it) },
                    isDefaultOption = { it == "Toutes" },
                    labelProvider = { region ->
                        Text(
                            text = if (region == "Toutes") stringResource(R.string.filtre_region) else region,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    dropdownItemLabelProvider = { region ->
                        Text(text = if (region == "Toutes") stringResource(R.string.type) else region)
                    }
                )
            }
            item {
                CustomDropDownMenu(
                    selectedValue = selectedType,
                    options = typesList,
                    expanded = isTypeMenuExpanded,
                    onExpandedChange = onTypeMenuExpandedChange,
                    onOptionSelected = { viewModel.updateType(it) },
                    isDefaultOption = { it == "Tous" },
                    labelProvider = { type ->
                        Text(
                            text = if (type == "Tous") stringResource(R.string.filtre_type) else getTranslationForType(type),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    dropdownItemLabelProvider = { type ->
                        Text(text = getTranslationForType(type))
                    }
                )
            }
        }
    }
}