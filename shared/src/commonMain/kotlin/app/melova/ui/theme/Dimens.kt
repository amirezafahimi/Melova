package app.melova.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The few sizes pages are built from. Gaps stay on a 4 dp step (4, 8, 12, 16, 24, 32); anything
 * else here is a size a page would otherwise write by hand and get slightly different each time.
 */
object MelovaDimens {
    /** Small pieces: a field, a row inside a card, a tile's cover. */
    val RadiusSmall = 12.dp

    /** Cards and banners. A box inside a card takes [RadiusSmall]. */
    val RadiusCard = 20.dp

    /** A normal button; also the smallest a tap target may be. */
    val ButtonHeight = 48.dp

    /** The one main action of a page, such as «شروع ضبط». */
    val PrimaryButtonHeight = 52.dp

    /** A button inside a row or a card that sits beside other content. */
    val CompactButtonHeight = 40.dp

    /** The picture, avatar or number badge at the start of a list row. */
    val ListLead = 40.dp
    val IconSmall = 16.dp
    val Icon = 20.dp
    val IconLarge = 24.dp
}

val MelovaSmallShape = RoundedCornerShape(MelovaDimens.RadiusSmall)
val MelovaCardShape = RoundedCornerShape(MelovaDimens.RadiusCard)
val MelovaPillShape = RoundedCornerShape(percent = 50)
