package fr.croustillapp.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.croustillapp.R
import fr.croustillapp.data.DailyMenuDto
import fr.croustillapp.data.Restaurant
import fr.croustillapp.features.RestaurantViewModel
import fr.croustillapp.ui.theme.Jersey10Family
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PixelNoiseFade(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    val vectorPainter = painterResource(id = R.drawable.pixel_fade_noise)

    Box(
        modifier = modifier.drawBehind {
            val colorFilter = ColorFilter.tint(color)

            val patternWidthPx = 100.dp.toPx().roundToInt()
            val patternHeightPx = size.height.roundToInt()

            val totalRepetitions = (size.width.roundToInt() / patternWidthPx) + 1

            for (i in 0 until totalRepetitions) {

                val xOffset = i * patternWidthPx

                drawContext.canvas.save()

                drawContext.transform.translate(left = xOffset.toFloat(), top = 0f)

                with(vectorPainter) {
                    draw(
                        size = androidx.compose.ui.geometry.Size(
                            (patternWidthPx + 1).toFloat(),
                            patternHeightPx.toFloat()
                        ),
                        colorFilter = colorFilter
                    )
                }

                drawContext.canvas.restore()
            }
        }
    )
}

@Composable
fun ActionIconItem(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
fun ActionIconItemWithMenu(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    options: List<Pair<String, () -> Unit>>
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ActionIconItem(
            iconRes = iconRes,
            label = label,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onClick = { if (enabled) expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            options.forEach { (title, action) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        action()
                        expanded = false
                    }
                )
            }
        }
    }
}

fun copyToClipboard(
    scope: CoroutineScope,
    clipboard: Clipboard,
    context: Context,
    label: String,
    text: String
) {
    scope.launch {
        val clipData = ClipData.newPlainText(label, text)
        clipboard.setClipEntry(ClipEntry(clipData))

        val message = "$label ${context.getString(R.string.copie_toast)}"

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantBottomSheet(
    restaurant: Restaurant,
    isFavorite: Boolean,
    viewModel: RestaurantViewModel = viewModel(),
    onFavoriteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val sheetColor = BottomSheetDefaults.ContainerColor

    val scrollState = rememberScrollState()

    val toastAdresse = stringResource(R.string.toast_btn_adresse)
    val toastTel = stringResource(R.string.toast_btn_telephone)
    val toastEmail = stringResource(R.string.toast_btn_courriel)
    val toastPartage = stringResource(R.string.toast_btn_partager)
    val labelCopie = stringResource(R.string.copie_action)

    val btnCarte = stringResource(R.string.action_btn_carte)
    val btnTel = stringResource(R.string.action_btn_telephoner)
    val btnEmail = stringResource(R.string.action_btn_courriel)
    val btnPartage = stringResource(R.string.action_btn_partager)

    val shareUrl = remember(restaurant.id) { restaurant.generateShareUrl() }
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()

    LaunchedEffect(restaurant.id) {
        viewModel.loadMenu(restaurant.id)
    }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
            scrollState.scrollTo(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor,
        dragHandle = null,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    AppImage(
                        url = restaurant.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(170.dp)
                    )

                    PixelNoiseFade(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.BottomCenter),
                        color = sheetColor
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 16.dp)) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Jersey10Family,
                        fontSize = 36.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (restaurant.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                id = R.string.statut_actuellement,
                                if (restaurant.isOpen) stringResource(R.string.statut_ouvert).lowercase()
                                else stringResource(R.string.statut_ferme).lowercase()
                            ),
                            color = statusColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val adresseOptions = remember(restaurant.adresse, toastAdresse, labelCopie, btnCarte) {
                            listOf(
                                labelCopie to {
                                    restaurant.adresse?.let { copyToClipboard(scope, clipboard, context, toastAdresse, it) }
                                    Unit
                                },
                                btnCarte to {
                                    try {
                                        val uri = "geo:${restaurant.latitude},${restaurant.longitude}?q=${Uri.encode(restaurant.adresse)}".toUri()
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_carte, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_carte,
                            label = stringResource(R.string.label_btn_adresse),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.adresse.isNullOrBlank(),
                            options = adresseOptions
                        )

                        val telOptions = remember(restaurant.telephone, toastTel, labelCopie, btnTel) {
                            listOf(
                                labelCopie to {
                                    restaurant.telephone?.let { copyToClipboard(scope, clipboard, context, toastTel, it) }
                                    Unit
                                },
                                btnTel to {
                                    try {
                                        val cleanNumber = restaurant.telephone?.replace("[^0-9]".toRegex(), "")
                                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$cleanNumber".toUri()))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_telephone, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_telephone,
                            label = stringResource(R.string.label_btn_telephone),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.telephone.isNullOrBlank(),
                            options = telOptions
                        )

                        val emailOptions = remember(restaurant.email, toastEmail, labelCopie, btnEmail) {
                            listOf(
                                labelCopie to {
                                    restaurant.email?.let { copyToClipboard(scope, clipboard, context, toastEmail, it) }
                                    Unit
                                },
                                btnEmail to {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:${restaurant.email}".toUri())
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_courriel, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_courriel,
                            label = stringResource(R.string.label_btn_courriel),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.email.isNullOrBlank(),
                            options = emailOptions
                        )

                        val shareOptions = remember(shareUrl, toastPartage, labelCopie, btnPartage) {
                            listOf(
                                labelCopie to {
                                    copyToClipboard(scope, clipboard, context, toastPartage, shareUrl)
                                },
                                btnPartage to {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_partage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_partager,
                            label = stringResource(R.string.label_btn_partager),
                            modifier = Modifier.weight(1f),
                            options = shareOptions
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (val state = menuState) {
                        is RestaurantViewModel.MenuUiState.Loading -> MenuLoadingView()
                        is RestaurantViewModel.MenuUiState.Error -> MenuErrorView { viewModel.loadMenu(restaurant.id) }
                        is RestaurantViewModel.MenuUiState.Success -> {
                            MenuSection(
                                dailyMenus = state.data,
                                onMenuSelected = { /* ... */ }
                            )
                        }
                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ScheduleSection(horaires = restaurant.horaires)

                    Spacer(modifier = Modifier.height(16.dp))

                    RestaurantFeaturesSection(restaurant = restaurant)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_fermer),
                contentDescription = stringResource(id = R.string.action_fermer),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                    .padding(8.dp)
            )

            Icon(
                painter = painterResource(id = if (isFavorite) R.drawable.ic_favori_oui else R.drawable.ic_favori_non),
                contentDescription = stringResource(id = R.string.action_favoriser),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onFavoriteClick() }
                    .padding(8.dp)
            )
        }
    }
}

fun Restaurant.generateShareUrl(): String {
    val baseUrl = "https://croustillant.menu/fr/restaurants/"

    // 1. Nom
    val cleanedName = name
        .lowercase()
        // 2. Remplace les caractères accentués
        .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // 3. Remplace tout ce qui n'est pas lettre ou chiffre par un tiret
        .replace("[^a-z0-9]".toRegex(), "-")
        // 4. On évite les doubles tirets successifs (ex: "l'eau" -> "l--eau" -> "l-eau")
        .replace("-+".toRegex(), "-")
        // 5. Retire les tirets au début ou à la fin s'il y en a
        .trim('-')

    // 6. Assemble : base + nom-nettoyé + -r + id
    return "$baseUrl$cleanedName-r$id"
}

@Composable
fun ScheduleSection(horaires: List<String>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.titre_horaires),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Jersey10Family
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (!horaires.isNullOrEmpty()) {
            horaires.forEach { ligne ->
                Text(
                    text = ligne.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = stringResource(R.string.horaires_non_disponibles),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .alpha(0.5f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSection(
    dailyMenus: List<DailyMenuDto>,
    onMenuSelected: (DailyMenuDto?) -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    val hasMenus = dailyMenus.isNotEmpty()
    val selectedMenu = if (hasMenus) dailyMenus.getOrNull(selectedIndex) else null

    LaunchedEffect(selectedMenu) {
        onMenuSelected(selectedMenu)
    }

    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        formatApiDate(sdf.format(Date()))
    }

    val sheetColor = BottomSheetDefaults.ContainerColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                        text = selectedMenu?.let { formatApiDate(it.date) } ?: todayDateFormatted,
                        color = if (hasMenus) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_fleche),
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
                                    text = formatApiDate(menu.date),
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

fun formatApiDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        val outputFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE)

        val date = inputFormat.parse(dateStr) ?: return dateStr
        val formatted = outputFormat.format(date).replaceFirstChar { it.uppercase() }

        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isToday) "$formatted (Aujourd'hui)" else formatted
    } catch (_: Exception) {
        dateStr
    }
}

fun shareMenuImage(context: Context, restaurantId: String, date: String) {
    val imageUrl = "https://api.croustillant.menu/v1/restaurants/$restaurantId/menu/$date/image"

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Regarde le menu de ce resto ! \n\n$imageUrl")
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Partager le menu")
    context.startActivity(shareIntent)
}

@Composable
fun RestaurantFeaturesSection(restaurant: Restaurant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                iconRes = R.drawable.ic_pmr,
                label = stringResource(id = R.string.statut_pmr),
                isActive = restaurant.pmr
            )
        }

        VerticalDivider()

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureIcon(
                iconRes = R.drawable.ic_izly,
                label = stringResource(id = R.string.statut_izly),
                isActive = restaurant.acceptsIzly
            )
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    )
}

@Composable
fun FeatureIcon(
    iconRes: Int,
    label: String,
    isActive: Boolean
) {
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = contentColor
        )
    }
}

@Composable
fun MenuLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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

@Composable
fun MenuErrorView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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