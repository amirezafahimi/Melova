package app.melova.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Room under anything at the bottom of a page. System bar insets don't give it: they are zero in
 * the browser, where an iPhone's rounded corners and home bar sit right at the page's edge.
 */
val PageBottomGap = 16.dp

/** A scrolling page's padding: the usual sides and top, with [PageBottomGap] under its end. */
val PagePadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = PageBottomGap)
