package by.tigre.music.player.desktop

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage

/**
 * App / taskbar / tray icon — same palette as [PlayerWindow] cover placeholder:
 * [by.tigre.music.player.desktop.presentation.theme.DesktopCoverBg] +
 * [by.tigre.music.player.desktop.presentation.theme.DesktopGreen] (full opacity for small sizes).
 */
internal fun createAppIcon(size: Int = 512): BufferedImage {
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

    val s = size.toDouble()

    g.color = Color(0x2A, 0x2A, 0x2A) // DesktopCoverBg
    val r = (s * 0.18).toInt()
    g.fillRoundRect(0, 0, size, size, r, r)

    g.color = Color(0x00, 0xCC, 0x44) // DesktopGreen

    val stemW = s * 0.065
    val beamH = s * 0.085
    val sx1 = s * 0.305
    val sx2 = s * 0.665
    val stTop1 = s * 0.175
    val stTop2 = s * 0.095
    val stBot1 = s * 0.755
    val stBot2 = s * 0.645

    fun beam(yLeft: Double, yRight: Double) {
        val path = GeneralPath()
        path.moveTo(sx1, yLeft)
        path.lineTo(sx2 + stemW, yRight)
        path.lineTo(sx2 + stemW, yRight + beamH)
        path.lineTo(sx1, yLeft + beamH)
        path.closePath()
        g.fill(path)
    }

    beam(stTop1, stTop2)
    beam(stTop1 + beamH * 1.7, stTop2 + beamH * 1.7)

    g.fillRect(sx1.toInt(), stTop1.toInt(), stemW.toInt(), (stBot1 - stTop1).toInt())
    g.fillRect(sx2.toInt(), stTop2.toInt(), stemW.toInt(), (stBot2 - stTop2).toInt())

    val nw = s * 0.28
    val nh = s * 0.17
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

    noteHead(sx1 + stemW / 2, stBot1 + nh * 0.15)
    noteHead(sx2 + stemW / 2, stBot2 + nh * 0.15)

    g.dispose()
    return img
}
