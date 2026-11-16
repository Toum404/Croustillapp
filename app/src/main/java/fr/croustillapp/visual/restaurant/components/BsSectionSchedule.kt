package fr.croustillapp.visual.restaurant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.data.helper.DayType
import fr.croustillapp.data.helper.HolidayHelper
import fr.croustillapp.data.model.JourOuvert
import fr.croustillapp.ui.theme.AppDimens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BsSectionSchedule(horaires: List<String>?, joursOuverts: List<JourOuvert>?, zone: String) {
    // FR: Recours a l'aide HolidayHelper pour intercepter la proximite d'un jour ferie.
    // EN: Leverages HolidayHelper utilities to detect the proximity of exceptional holiday closures.
    val isDroitLocal = remember(zone) {
        HolidayHelper.isDroitLocalZone(zone)
    }

    val holidayAlert = remember(isDroitLocal) {
        HolidayHelper.checkUpcomingHoliday(isDroitLocal)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        holidayAlert?.let { alert ->
            val holidayNameTranslated = stringResource(alert.holidayStringRes)

            val alertText = when (alert.targetDayType) {
                DayType.TODAY -> stringResource(R.string.bs_alert_ajd, holidayNameTranslated)
                DayType.TOMORROW -> stringResource(R.string.bs_alert_dem, holidayNameTranslated)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 10.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = alertText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        BsSection(
            title = stringResource(R.string.bs_label_horaires)
        ) {
            when {
                !horaires.isNullOrEmpty() -> {
                    horaires.forEach { ligne ->
                        Text(
                            text = ligne.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                !joursOuverts.isNullOrEmpty() -> {
                    PixelScheduleTable(joursOuverts = joursOuverts)
                }

                else -> {
                    Text(
                        text = stringResource(R.string.bs_aucun_horaire),
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

@Composable
fun PixelScheduleTable(joursOuverts: List<JourOuvert>) {
    // FR: Resolution de la cle temporelle ISO pour cibler et formater le jour courant de la semaine.
    // EN: Resolves ISO calendar timeline indices to match and distinctively highlight the active day of the week.
    val currentDayOfWeek = LocalDate.now().dayOfWeek.value

    val currentLocale = Locale.getDefault()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1.4f))
            Text(text = stringResource(id = R.string.bs_colonne_matin), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(id = R.string.bs_colonne_midi), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(id = R.string.bs_colonne_soir), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(2.dp))

        joursOuverts.forEachIndexed { index, data ->
            val isToday = (index + 1) == currentDayOfWeek

            val jourTraduit = remember(index, currentLocale) {
                DayOfWeek.of(index + 1)
                    .getDisplayName(TextStyle.FULL, currentLocale)
                    .replaceFirstChar { it.titlecase(currentLocale) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1.4f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = if (isToday) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RectangleShape
                                )
                        )

                        Text(
                            text = jourTraduit,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ScheduleStatusIcon(isOpen = data.ouverture.matin, isToday = isToday)
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ScheduleStatusIcon(isOpen = data.ouverture.midi, isToday = isToday)
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ScheduleStatusIcon(isOpen = data.ouverture.soir, isToday = isToday)
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

        Text(
            text = stringResource(id = R.string.bs_info_horaires),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScheduleStatusIcon(isOpen: Boolean, isToday: Boolean) {
    val iconRes = if (isOpen) R.drawable.ic_visual_check else R.drawable.ic_visual_empty
    val tintColor = when {
        isOpen && isToday -> MaterialTheme.colorScheme.primary
        isOpen -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier.size(16.dp)
    )
}