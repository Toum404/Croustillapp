package fr.croustillapp.features.bottomsheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.features.data.DailyMenuDto
import fr.croustillapp.features.data.DayType
import fr.croustillapp.features.data.HolidayHelper
import fr.croustillapp.features.data.JourOuvert
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.ui.theme.Jersey10Family
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FR: Section affichant les menus du jour avec un sélecteur déroulant (DropdownMenu) pour changer de date.
 * EN: Section presenting daily menus along with a DropdownMenu selector to toggle dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSection(
    dailyMenus: List<DailyMenuDto>,
    onMenuSelected: (DailyMenuDto?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedIndex by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    val hasMenus = dailyMenus.isNotEmpty()
    val selectedMenu = if (hasMenus) dailyMenus.getOrNull(selectedIndex) else null

    // FR: Notifie le composant parent dès que le menu sélectionné change.
    // EN: Notifies the parent component as soon as the selected menu state updates.
    LaunchedEffect(selectedMenu) {
        onMenuSelected(selectedMenu)
    }

    val todayDateFormatted = remember(context) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        formatApiDate(context, sdf.format(Date()))
    }

    val sheetColor = BottomSheetDefaults.ContainerColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(getOpaqueSurfaceVariant(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.label_menu),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Jersey10Family
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (hasMenus) expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = hasMenus,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = sheetColor,
                    disabledContainerColor = sheetColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = if (hasMenus) 0.2f else 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMenu?.let { formatApiDate(context, it.date) } ?: todayDateFormatted,
                        color = if (hasMenus) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_ddm),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (hasMenus) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            if (hasMenus) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    dailyMenus.forEachIndexed { index, menu ->
                        val isSelected = index == selectedIndex
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = formatApiDate(context, menu.date),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedIndex = index
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (hasMenus && selectedMenu != null) {
            // FR: Itération sur les catégories (Entrées, Plats, Desserts) et affichage sous forme de liste à puces.
            // EN: Iterating through meal categories (Starters, Mains, Desserts) and listing them using bullet points.
            selectedMenu.repas.firstOrNull()?.categories?.forEach { cat ->
                Text(
                    text = cat.libelle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                cat.plats.forEach { plat ->
                    Text(
                        text = "• ${plat.libelle}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.aucun_menu),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .alpha(0.6f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * FR: Section gérant l'affichage des horaires d'ouverture avec injection d'une alerte en cas de jour férié.
 * EN: Section managing the schedule layout injection along with banner alerts for upcoming public holidays.
 */
@Composable
fun ScheduleSection(horaires: List<String>?, joursOuverts: List<JourOuvert>?, isStrasbourg: Boolean) {
    // FR: Recours à l'aide HolidayHelper pour intercepter la proximité d'un jour férié.
    // EN: Leverages HolidayHelper utilities to detect the proximity of exceptional holiday closures.
    val holidayAlert = remember(isStrasbourg) { HolidayHelper.checkUpcomingHoliday(isStrasbourg) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            holidayAlert?.let { alert ->
                val holidayNameTranslated = stringResource(alert.holidayStringRes)

                val alertText = when (alert.targetDayType) {
                    DayType.TODAY -> stringResource(R.string.holiday_alert_today, holidayNameTranslated)
                    DayType.TOMORROW -> stringResource(R.string.holiday_alert_tomorrow, holidayNameTranslated)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_visual_calendrier),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = alertText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (holidayAlert == null) {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (holidayAlert != null) 24.dp else 0.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(getOpaqueSurfaceVariant(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.titre_horaires),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Jersey10Family
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                // FR: Option 1 : Affichage des lignes de texte d'horaires préformatées reçues de l'API.
                // EN: Option 1: Rendering raw pre-formatted schedule text lines fetched from the backend API.
                !horaires.isNullOrEmpty() -> {
                    horaires.forEach { ligne ->
                        Text(
                            text = ligne.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // FR: Option 2 : Construction d'un tableau d'horaires structuré si les lignes brutes manquent.
                // EN: Option 2: Building a structured schedule table view if raw textual data lines are missing.
                !joursOuverts.isNullOrEmpty() -> {
                    PixelScheduleTable(joursOuverts = joursOuverts)
                }

                else -> {
                    Text(
                        text = stringResource(R.string.horaires_non_disponibles),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .alpha(0.5f)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * FR: Section d'affichage des caractéristiques techniques de l'établissement (Identifiant, Accessibilité PMR, Izly).
 * EN: Infrastructure and features display row highlighting attributes (Identifier, PMR Accessibility, Izly).
 */
@Composable
fun RestaurantFeaturesSection(restaurant: Restaurant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(getOpaqueSurfaceVariant(alpha = 0.5f))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.statut_code),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "#${restaurant.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Jersey10Family,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        VerticalDivider()

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureIcon(
                iconRes = R.drawable.ic_visual_pmr,
                label = stringResource(id = R.string.statut_pmr),
                isActive = restaurant.pmr
            )
        }

        VerticalDivider()

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureIcon(
                iconRes = R.drawable.ic_visual_izly,
                label = stringResource(id = R.string.statut_izly),
                isActive = restaurant.acceptsIzly
            )
        }
    }
}

/**
 * FR: Vue d'attente contenant un indicateur de progression circulaire pour le chargement asynchrone des cartes de menus.
 * EN: Placeholder loading view containing a centered circular progress indicator for asynchronous menus data fetches.
 */
@Composable
fun MenuLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(getOpaqueSurfaceVariant(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.label_menu),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Jersey10Family
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

/**
 * FR: Vue d'erreur interactive incitant l'utilisateur à cliquer pour re-déclencher la requête réseau échouée.
 * EN: Interactive fallback error view encouraging users to tap in order to clear and retry failed network tasks.
 */
@Composable
fun MenuErrorView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(getOpaqueSurfaceVariant(alpha = 0.5f))
            .clickable { onRetry() }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.label_menu),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Jersey10Family
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.erreur_menu),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.5f)
            )
            Text(
                text = stringResource(id = R.string.recharger_menu),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                modifier = Modifier.alpha(0.5f)
            )
        }
    }
}