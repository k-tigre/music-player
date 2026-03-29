package by.tigre.music.player.desktop

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage

/** Programmatic ♫ icon — green-on-dark, matching the desktop theme. */
internal fun createAppIcon(size: Int = 512): BufferedImage {
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

    val s = size.toDouble()

    // ── Background ─────────────────────────────────────────────────────────────
    g.color = Color(0x1C, 0x1C, 0x1C)
    val r = (s * 0.18).toInt()
    g.fillRoundRect(0, 0, size, size, r, r)

    // ── Green foreground ───────────────────────────────────────────────────────
    g.color = Color(0x00, 0xCC, 0x44)

    val stemW = s * 0.065   // stem width
    val beamH = s * 0.085   // beam height

    // Stem X positions (right edge of note head → stem is on right side of ellipse)
    val sx1 = s * 0.305      // left stem left edge
    val sx2 = s * 0.665      // right stem left edge

    // Stem Y (top → bottom)
    val stTop1 = s * 0.175
    val stTop2 = s * 0.095
    val stBot1 = s * 0.755
    val stBot2 = s * 0.645

    // ── Two diagonal beams ─────────────────────────────────────────────────────
    // Each beam is a filled quadrilateral (parallelogram) sloping from left-top to right-higher.
    fun beam(yLeft: Double, yRight: Double) {
        val path = GeneralPath()
        path.moveTo(sx1, yLeft)
        path.lineTo(sx2 + stemW, yRight)
        path.lineTo(sx2 + stemW, yRight + beamH)
        path.lineTo(sx1, yLeft + beamH)
        path.closePath()
        g.fill(path)
    }

    beam(stTop1, stTop2)                           // top beam
    beam(stTop1 + beamH * 1.7, stTop2 + beamH * 1.7)  // second beam

    // ── Stems ──────────────────────────────────────────────────────────────────
    g.fillRect(sx1.toInt(), stTop1.toInt(), stemW.toInt(), (stBot1 - stTop1).toInt())
    g.fillRect(sx2.toInt(), stTop2.toInt(), stemW.toInt(), (stBot2 - stTop2).toInt())

    // ── Note heads (tilted ellipses) ───────────────────────────────────────────
    val nw = s * 0.28   // ellipse full width
    val nh = s * 0.17   // ellipse full height
    val tilt = Math.toRadians(-28.0)

    fun noteHead(cx: Double, cy: Double) {
        val saved = g.transform
        val at = AffineTransform(saved)
        at.translate(cx, cy)
        at.rotate(tilt)
        g.transform = at
        g.fill(Ellipse2D.Double(-nw / 2, -nh / 2, nw, nh))
        g.transform = saved
    }

    // Left note head: centred at the bottom of the left stem
    noteHead(sx1 + stemW / 2, stBot1 + nh * 0.15)
    // Right note head: centred at the bottom of the right stem
    noteHead(sx2 + stemW / 2, stBot2 + nh * 0.15)

    g.dispose()
    return img
}
