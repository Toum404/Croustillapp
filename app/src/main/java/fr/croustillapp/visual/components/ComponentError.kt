package fr.croustillapp.visual.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    BaseErrorState(
        iconRes = R.drawable.ic_error_empty,
        title = stringResource(R.string.error_empty_tit),
        description = stringResource(R.string.error_empty_txt),
        modifier = modifier
    )
}

@Composable
private fun BaseErrorState(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val commonTint = MaterialTheme.colorScheme.onSurfaceVariant

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(AppDimens.paddingBig),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = commonTint
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = Jersey10Family,
                    fontSize = 24.sp,
                    color = commonTint
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = commonTint
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .padding(AppDimens.paddingBig),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = commonTint
            )
            Spacer(modifier = Modifier.height(AppDimens.spacerBig))
            Text(
                text = title,
                fontFamily = Jersey10Family,
                fontSize = 24.sp,
                color = commonTint,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = commonTint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}