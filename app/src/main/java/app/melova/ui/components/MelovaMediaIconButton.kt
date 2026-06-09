package app.melova.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MelovaMediaIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    contentColor: Color = Color.White,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 12.dp
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(buttonSize),
        shape = RoundedCornerShape(cornerRadius),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.35f),
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
        }
    }
}
