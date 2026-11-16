package fr.croustillapp.visual.restaurant.components

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import fr.croustillapp.R
import fr.croustillapp.data.helper.isToday
import fr.croustillapp.data.helper.isTomorrow
import fr.croustillapp.data.model.UiMenu
import fr.croustillapp.extra.addMenuToCalendar
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.components.CustomFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun BsSectionMenu(
    restaurantId: String,
    restaurantName: String,
    restaurantAddress: String,
    dailyMenus: List<UiMenu>,
    onMenuSelected: (UiMenu?) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val actionExport = stringResource(R.string.menu_act_export)
    val actionCalendar = stringResource(R.string.menu_act_event)
    val actionCopy = stringResource(R.string.menu_act_copy)

    val titleShare = stringResource(R.string.menu_title_share)
    val success = stringResource(R.string.copie_toast)
    val error = stringResource(R.string.menu_act_error)

    var selectedSegment by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var customSelectedDateIndex by remember { mutableStateOf<Int?>(null) }

    val currentLocale = ConfigurationCompat.getLocales(
        LocalConfiguration.current
    )[0] ?: Locale.getDefault()

    val availableTimestamps = remember(dailyMenus) {
        val sdf = SimpleDateFormat(AppDimens.DATE_FORMAT_PATTERN, Locale.FRANCE).apply {
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
                    val inputSdf = SimpleDateFormat(AppDimens.DATE_FORMAT_PATTERN, Locale.FRANCE).apply {
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
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
        null
    }

    val emptyStateDate = remember(selectedSegment, customSelectedDateIndex, dailyMenus) {
        val calendar = Calendar.getInstance()
        when (selectedSegment) {
            0 -> calendar.time
            1 -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.time
            }
            else -> {
                customSelectedDateIndex?.let { index ->
                    dailyMenus.getOrNull(index)?.date?.let { dateStr ->
                        try {
                            SimpleDateFormat(AppDimens.DATE_FORMAT_PATTERN, Locale.FRANCE).apply {
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
            .animateContentSize(
                animationSpec = tween(durationMillis = AppDimens.ANIMATION, easing = LinearEasing)
            )
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(AppDimens.paddingBig)
    ) {
        Text(
            text = stringResource(id = R.string.bs_label_menu),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Jersey10Family
        )

        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomFilter(
                modifier = Modifier.weight(1f),
                selected = selectedSegment == 0,
                onClick = {
                    selectedSegment = 0
                    customSelectedDateIndex = null
                },
                label = {
                    Text(
                        text = stringResource(R.string.bs_chp_ajd),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )

            CustomFilter(
                modifier = Modifier.weight(1f),
                selected = selectedSegment == 1,
                onClick = {
                    selectedSegment = 1
                    customSelectedDateIndex = null
                },
                shape = RoundedCornerShape(6.dp),
                label = {
                    Text(
                        text = stringResource(R.string.bs_chip_dem),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )

            CustomFilter(
                modifier = Modifier.weight(1f),
                selected = selectedSegment == 2,
                onClick = {
                    showDatePicker = true
                },
                enabled = !hasNoMenus,
                shape = RoundedCornerShape(6.dp),
                leadingIcon = {
                    if (selectedDateText != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_visual_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                label = {
                    if (selectedDateText != null) {
                        Text(
                            text = selectedDateText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
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
            val hasPointsSystem = remember(currentMenu) {
                currentMenu.repas.any { repas ->
                    repas.categories.any { cat ->
                        cat.plats.any { plat -> plat.points != null }
                    }
                }
            }

            currentMenu.repas.firstOrNull()?.categories?.forEach { cat ->
                Text(
                    text = cat.libelle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "▪ ${plat.libelle}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(if (hasPointsSystem) 0.80f else 1f)
                            )

                            if (hasPointsSystem) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .padding(horizontal = 8.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                Box(
                                    modifier = Modifier.weight(0.20f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (plat.points != null) {
                                        Text(
                                            text = "${plat.points} ${stringResource(R.string.bs_menu_pts)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Visible
                                        )
                                    }
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showDropdownItem,
                            onDismissRequest = { showDropdownItem = false },
                            offset = pressOffsetItem,
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
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

                                            } catch (_: Exception) {
                                                try {
                                                    val fallbackIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, url)
                                                        type = "text/plain"
                                                    }
                                                    val chooser = Intent.createChooser(fallbackIntent, titleShare)
                                                    context.startActivity(chooser)

                                                } catch (fallbackEx: Exception) {
                                                    fallbackEx.printStackTrace()
                                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ddm_img),
                                        modifier = Modifier.size(20.dp),
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(actionCalendar) },
                                onClick = {
                                    showDropdownItem = false
                                    addMenuToCalendar(
                                        context = context,
                                        restaurantName = restaurantName,
                                        restaurantAddress = restaurantAddress,
                                        currentMenu = currentMenu,
                                        errorToast = error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ddm_event),
                                        modifier = Modifier.size(20.dp),
                                        contentDescription = null
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
                                        painter = painterResource(id = R.drawable.ic_copy_off),
                                        modifier = Modifier.size(20.dp),
                                        contentDescription = stringResource(R.string.copie_action)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } else {
            val isCustomEmptyState = selectedSegment == 2 && customSelectedDateIndex == null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.bsSectionMenu)
                    .then(
                        if (isCustomEmptyState) {
                            Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showDatePicker = true }
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                val topText = if (selectedSegment == 2 && customSelectedDateIndex == null) {
                    stringResource(R.string.bs_menu_empty)
                } else {
                    formattedFullDate
                }

                if (topText != null) {
                    Text(
                        text = topText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val bottomText = if (selectedSegment == 2 && customSelectedDateIndex == null) {
                    stringResource(R.string.bs_menu_select)
                } else {
                    stringResource(R.string.bs_aucun_menu)
                }

                Text(
                    text = bottomText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppDimens.spacerMedium))
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(customSelectedDateIndex, dailyMenus) {
            if (customSelectedDateIndex != null) {
                val menu = dailyMenus.getOrNull(customSelectedDateIndex!!)
                menu?.let {
                    try {
                        SimpleDateFormat(AppDimens.DATE_FORMAT_PATTERN, Locale.FRANCE).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.parse(it.date)?.time
                    } catch (_: Exception) { null }
                }
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
                if (customSelectedDateIndex == null && selectedSegment == 2) {
                    selectedSegment = 0
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat(AppDimens.DATE_FORMAT_PATTERN, Locale.FRANCE).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val selectedDateStr = sdf.format(Date(millis))

                            val todayMenu = dailyMenus.firstOrNull { isToday(it.date) }
                            val tomorrowMenu = dailyMenus.firstOrNull { isTomorrow(it.date) }

                            when (selectedDateStr) {
                                todayMenu?.date -> {
                                    selectedSegment = 0
                                    customSelectedDateIndex = null
                                }
                                tomorrowMenu?.date -> {
                                    selectedSegment = 1
                                    customSelectedDateIndex = null
                                }
                                else -> {
                                    val foundIndex = dailyMenus.indexOfFirst { it.date == selectedDateStr }
                                    if (foundIndex != -1) {
                                        selectedSegment = 2
                                        customSelectedDateIndex = foundIndex
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.bs_btn_3))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        if (customSelectedDateIndex == null && selectedSegment == 2) {
                            selectedSegment = 0
                        }
                    }
                ) {
                    Text(stringResource(R.string.bs_btn_1))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}