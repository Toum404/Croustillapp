package fr.croustillapp.visual.restaurant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family

@Composable
fun BsSection(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(AppDimens.cornerSmall))
        .background(color = MaterialTheme.colorScheme.surfaceContainer)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
            } else {
                Modifier
            }
        )
        .padding(AppDimens.paddingBig)

    Column(modifier = baseModifier.then(modifier)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Jersey10Family
        )

        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

        content()
    }
}