package fr.croustillapp.features.bottomsheet

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import fr.croustillapp.R
import fr.croustillapp.features.data.DailyMenuDto
import fr.croustillapp.features.data.DayType
import fr.croustillapp.features.data.HolidayHelper
import fr.croustillapp.features.data.JourOuvert
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.ui.theme.Jersey10Family
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSection(
    restaurantId: String,
    dailyMenus: List<DailyMenuDto>,
    onMenuSelected: (DailyMenuDto?) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val actionExport = stringResource(R.string.menu_act_export)
    val actionCopy = stringResource(R.string.menu_act_copy)

    val titleShare = stringResource(R.string.menu_title_share)
    val success = stringResource(R.string.copie_toast)
    val error = stringResource(R.string.menu_act_error)

    var selectedSegment by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var customSelectedDateIndex by remember { mutableStateOf<Int?>(null) }

    val currentLocale = androidx.core.os.ConfigurationCompat.getLocales(
        androidx.compose.ui.platform.LocalConfiguration.current
    )[0] ?: Locale.getDefault()

    val availableTimestamps = remember(dailyMenus) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        dailyMenus.mapNotNull { menu ->
            try {
                val parsedDate = sdf.parse(menu.date)
                parsedDate?.time
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    val hasNoMenus = dailyMenus.isEmpty() || availableTimestamps.isEmpty()

    val currentMenu = remember(selectedSegment, customSelectedDateIndex, dailyMenus) {
        when (selectedSegment) {
            0 -> dailyMenus.firstOrNull { isToday(it.date) }
            1 -> dailyMenus.firstOrNull { isTomorrow(it.date) }
            else -> customSelectedDateIndex?.let { dailyMenus.getOrNull(it) }
        }
    }

    LaunchedEffect(currentMenu) {
        onMenuSelected(currentMenu)
    }

    val selectedDateText = remember(selectedSegment, customSelectedDateIndex, dailyMenus, currentLocale) {
        if (selectedSegment == 2 && customSelectedDateIndex != null) {
            val menu = customSelectedDateIndex?.let { dailyMenus.getOrNull(it) }
            if (menu != null) {
                try {
                    val inputSdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val outputSdf = SimpleDateFormat("d MMM", currentLocale)
                    val dateObj = inputSdf.parse(menu.date)
                    if (dateObj != null) {
                        var formatted = outputSdf.format(dateObj)
                        if (formatted.endsWith(".")) {
                            formatted = formatted.removeSuffix(".")
                        }
                        return@remember formatted
                    }
                } catch (_: Exception) {}
            }
        }
        null
    }

    val emptyStateDate = remember(selectedSegment, customSelectedDateIndex, dailyMenus) {
        val calendar = java.util.Calendar.getInstance()
        when (selectedSegment) {
            0 -> calendar.time
            1 -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                calendar.time
            }
            else -> {
                customSelectedDateIndex?.let { index ->
                    dailyMenus.getOrNull(index)?.date?.let { dateStr ->
                        try {
                            SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.parse(dateStr)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
        }
    }

    val formattedFullDate = remember(emptyStateDate, currentLocale) {
        emptyStateDate?.let { date ->
            val sdf = SimpleDateFormat("EEEE d MMMM yyyy", currentLocale).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString() }
        }
    }

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

        Spacer(modifier = Modifier.height(8.dp))
        val sheetColor = BottomSheetDefaults.ContainerColor

        val chipBorder = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = true,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
            borderWidth = 0.dp
        )

        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = sheetColor,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = getOpaqueSurfaceVariant(alpha = 0.5f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = selectedSegment == 0,
                onClick = {
                    selectedSegment = 0
                    customSelectedDateIndex = null
                },
                border = chipBorder,
                colors = chipColors,
                shape = RoundedCornerShape(6.dp),
                label = {
                    Text(
                        text = stringResource(R.string.date_today),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selectedSegment == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }
            )

            FilterChip(
                modifier = Modifier.weight(1f),
                selected = selectedSegment == 1,
                onClick = {
                    selectedSegment = 1
                    customSelectedDateIndex = null
                },
                border = chipBorder,
                colors = chipColors,
                shape = RoundedCornerShape(6.dp),
                label = {
                    Text(
                        text = stringResource(R.string.date_tomorrow),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selectedSegment == 1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }
            )

            FilterChip(
                modifier = Modifier.weight(0.8f),
                selected = selectedSegment == 2,
                onClick = {
                    selectedSegment = 2
                    showDatePicker = true
                },
                enabled = !hasNoMenus,
                border = chipBorder,
                colors = chipColors,
                shape = RoundedCornerShape(6.dp),
                label = {
                    if (selectedDateText != null) {
                        Text(
                            text = selectedDateText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                            fontWeight = if (selectedSegment == 2) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ddm_calendar),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentMenu != null) {
            currentMenu.repas.firstOrNull()?.categories?.forEach { cat ->
                Text(
                    text = cat.libelle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )

                cat.plats.forEach { plat ->
                    var showDropdownItem by remember { mutableStateOf(false) }
                    var pressOffsetItem by remember { mutableStateOf(DpOffset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        pressOffsetItem = with(density) {
                                            DpOffset(offset.x.toDp(), offset.y.toDp())
                                        }
                                        showDropdownItem = true
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = "▪ ${plat.libelle}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                        )

                        DropdownMenu(
                            expanded = showDropdownItem,
                            onDismissRequest = { showDropdownItem = false },
                            offset = pressOffsetItem
                        ) {
                            DropdownMenuItem(
                                text = { Text(actionExport) },
                                onClick = {
                                    showDropdownItem = false

                                    currentMenu.let { menu ->
                                        val dateStr = menu.date
                                        val url = "https://api.croustillant.menu/v1/restaurants/$restaurantId/menu/$dateStr/image"

                                        coroutineScope.launch {
                                            try {
                                                val imageFile = withContext(Dispatchers.IO) {
                                                    val file = File(
                                                        context.cacheDir,
                                                        "menu_${restaurantId}_$dateStr.png"
                                                    )
                                                    URL(url).openStream().use { input ->
                                                        file.outputStream().use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    file
                                                }

                                                val photoUri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    imageFile
                                                )

                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_STREAM, photoUri)
                                                    type = "image/png"
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                val shareIntent = Intent.createChooser(sendIntent, titleShare)
                                                context.startActivity(shareIntent)

                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ddm_img),
                                        modifier = Modifier.size(18.dp),
                                        contentDescription = "Icône télécharger"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(actionCopy) },
                                onClick = {
                                    showDropdownItem = false

                                    currentMenu.let { menu ->
                                        val stringBuilder = StringBuilder()

                                        menu.repas.forEach { repas ->
                                            repas.categories.forEach { cat ->
                                                stringBuilder.append("${cat.libelle.uppercase()}\n")
                                                cat.plats.forEach { plat ->
                                                    stringBuilder.append("• ${plat.libelle}\n")
                                                }
                                                stringBuilder.append("\n")
                                            }
                                        }

                                        val textToCopy = stringBuilder.toString().trim()

                                        coroutineScope.launch {
                                            try {
                                                clipboard.setClipEntry(
                                                    ClipData.newPlainText("Menu", textToCopy).toClipEntry()
                                                )
                                                Toast.makeText(context, success, Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {
                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ddm_copy),
                                        modifier = Modifier.size(18.dp),
                                        contentDescription = "Icône copier"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val topText = if (selectedSegment == 2 && customSelectedDateIndex == null) {
                    stringResource(R.string.menu_empty)
                } else {
                    formattedFullDate
                }

                if (topText != null) {
                    Text(
                        text = topText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.75f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val bottomText = if (selectedSegment == 2 && customSelectedDateIndex == null) {
                    stringResource(R.string.menu_select)
                } else {
                    stringResource(R.string.aucun_menu)
                }

                Text(
                    text = bottomText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(customSelectedDateIndex, dailyMenus) {
            if (customSelectedDateIndex != null) {
                val menu = dailyMenus.getOrNull(customSelectedDateIndex!!)
                if (menu != null) {
                    try {
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        sdf.parse(menu.date)?.time
                    } catch (_: Exception) {
                        null
                    }
                } else null
            } else null
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return availableTimestamps.contains(utcTimeMillis)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val selectedDateStr = sdf.format(Date(millis))
                            val foundIndex = dailyMenus.indexOfFirst { it.date == selectedDateStr }
                            if (foundIndex != -1) {
                                customSelectedDateIndex = foundIndex
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.select))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * FR: Section gerant l'affichage des horaires d'ouverture avec injection d'une alerte en cas de jour ferie.
 * EN: Section managing the schedule layout injection along with banner alerts for upcoming public ays.
 */
@Composable
fun ScheduleSection(horaires: List<String>?, joursOuverts: List<JourOuvert>?, isStrasbourg: Boolean) {
    // FR: Recours a l'aide HolidayHelper pour intercepter la proximite d'un jour ferie.
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
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_visual_warning),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = alertText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                // FR: Option 1 : Affichage des lignes de texte d'horaires preformatees reçues de l'API.
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

                // FR: Option 2 : Construction d'un tableau d'horaires structure si les lignes brutes manquent.
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
 * FR: Section d'affichage des caracteristiques techniques de l'etablissement (Identifiant, Accessibilite PMR, Izly).
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
 * FR: Vue d'erreur interactive incitant l'utilisateur a cliquer pour re-declencher la requête reseau echouee.
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