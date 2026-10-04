package com.weighttracker.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons from the canvas design (24×24 viewBox). */
object WtIcons {
    private fun stroke(name: String, path: String, width: Float = 1.9f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()

    val Home = stroke("home", "M3 11l9-7 9 7M5 10v10h14V10")
    val PlusCircle = stroke("plus-circle", "M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18zM12 8v8M8 12h8")
    val Sliders = stroke("sliders", "M4 7h10M18 7h2M4 17h4M12 17h8M16 5v4M10 15v4")
    val Calendar = stroke("calendar", "M4 6h16v14H4zM4 10h16M8 3v4M16 3v4", 2f)
    val Trash = stroke("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Flag = stroke("flag", "M5 21V4M5 4h11l-2 4 2 4H5", 2f)
    val Database = stroke(
        "database",
        "M12 4c4.4 0 8 1.3 8 3s-3.6 3-8 3-8-1.3-8-3 3.6-3 8-3zM4 7v10c0 1.7 3.6 3 8 3s8-1.3 8-3V7M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3",
        2f,
    )
    val Download = stroke("download", "M12 4v11M7 10l5 5 5-5M5 20h14", 2f)
    val Table = stroke("table", "M4 4h16v16H4zM4 10h16M4 15h16M10 4v16", 2f)
    val Upload = stroke("upload", "M12 20V9M7 14l5-5 5 5M5 4h14", 2f)
    val ChevronLeft = stroke("chevron-left", "M15 5l-7 7 7 7", 2.2f)
    val ChevronRight = stroke("chevron-right", "M9 5l7 7-7 7", 2.2f)
    val Plus = stroke("plus", "M12 5v14M5 12h14", 2.2f)
}
