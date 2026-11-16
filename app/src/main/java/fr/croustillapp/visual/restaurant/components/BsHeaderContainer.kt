package fr.croustillapp.visual.restaurant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import kotlin.math.roundToInt

@Composable
fun BottomSheetHeaderContainer(
    backgroundColor: Color,
    sheetColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.bsSizeHeader)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(backgroundColor)
    ) {
        content()

        PixelNoiseFade(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.BottomCenter),
            color = sheetColor
        )
    }
}

@Composable
fun PixelNoiseFade(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    val vectorPainter = painterResource(id = R.drawable.svg_pixel_noise)

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
                        size = Size((patternWidthPx + 1).toFloat(), patternHeightPx.toFloat()),
                        colorFilter = colorFilter
                    )
                }
                drawContext.canvas.restore()
            }
        }
    )
}