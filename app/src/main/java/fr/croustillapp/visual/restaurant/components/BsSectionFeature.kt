package fr.croustillapp.visual.restaurant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family

@Composable
fun BsSectionFeature(restaurant: Restaurant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.bas_info_code),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        AppVerticalDivider()

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureIcon(
                iconRes = if (restaurant.pmr) R.drawable.ic_filtre_pmr else R.drawable.ic_visual_false,
                label = stringResource(id = R.string.statut_pmr),
                isActive = restaurant.pmr
            )
        }

        AppVerticalDivider()

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureIcon(
                iconRes = if (restaurant.acceptsIzly) R.drawable.ic_filtre_izly else R.drawable.ic_visual_false,
                label = stringResource(id = R.string.statut_izly),
                isActive = restaurant.acceptsIzly
            )
        }
    }
}

@Composable
fun FeatureIcon(
    iconRes: Int,
    label: String,
    isActive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppVerticalDivider() {
    VerticalDivider(
        modifier = Modifier.height(24.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}