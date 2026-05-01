#!/usr/bin/env python3
"""Build app.png / app.ico / app.icns for jpackage — same art as AppIcon.kt (desktop green note)."""
from __future__ import annotations

import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from icnsutil.IcnsFile import IcnsFile
from matplotlib.patches import Ellipse, FancyBboxPatch, PathPatch, Rectangle
from matplotlib.path import Path as MPath
from PIL import Image


def zero_rgb_where_fully_transparent(img: Image.Image) -> Image.Image:
    """Matplotlib can emit (255,255,255,0) in corners; some ICO/Shell paths ignore alpha and show white."""
    if img.mode != "RGBA":
        return img
    rgba = np.array(img, copy=True)
    transparent = rgba[:, :, 3] == 0
    rgba[transparent, 0] = 0
    rgba[transparent, 1] = 0
    rgba[transparent, 2] = 0
    return Image.fromarray(rgba)


def render_player_brand_icon(size_px: int) -> Image.Image:
    """Keep in sync with AppIcon.kt (DesktopCoverBg + DesktopGreen)."""
    s = float(size_px)
    dpi = 100
    # Default figure face is white → shows in the four wedges outside the rounded card.
    fig = plt.figure(
        figsize=(size_px / dpi, size_px / dpi),
        dpi=dpi,
        facecolor="none",
        edgecolor="none",
    )
    ax = fig.add_axes((0, 0, 1, 1), facecolor="none")
    ax.set_xlim(0, s)
    ax.set_ylim(s, 0)
    ax.set_aspect("equal")
    ax.axis("off")

    r = s * 0.18
    ax.add_patch(
        FancyBboxPatch(
            (0, 0),
            s,
            s,
            boxstyle=f"round,pad=0,rounding_size={r}",
            facecolor="#2a2a2a",
            edgecolor="none",
        )
    )

    stem_w = s * 0.065
    beam_h = s * 0.085
    sx1, sx2 = s * 0.305, s * 0.665
    st_top1, st_top2 = s * 0.175, s * 0.095
    st_bot1, st_bot2 = s * 0.755, s * 0.645
    green = "#00cc44"

    def beam(y_left: float, y_right: float) -> None:
        verts = [
            (sx1, y_left),
            (sx2 + stem_w, y_right),
            (sx2 + stem_w, y_right + beam_h),
            (sx1, y_left + beam_h),
            (0, 0),
        ]
        codes = [MPath.MOVETO, MPath.LINETO, MPath.LINETO, MPath.LINETO, MPath.CLOSEPOLY]
        ax.add_patch(PathPatch(MPath(verts, codes), facecolor=green, edgecolor="none"))

    beam(st_top1, st_top2)
    beam(st_top1 + beam_h * 1.7, st_top2 + beam_h * 1.7)

    ax.add_patch(
        Rectangle(
            (sx1, st_top1),
            stem_w,
            st_bot1 - st_top1,
            facecolor=green,
            edgecolor="none",
        )
    )
    ax.add_patch(
        Rectangle(
            (sx2, st_top2),
            stem_w,
            st_bot2 - st_top2,
            facecolor=green,
            edgecolor="none",
        )
    )

    nw, nh = s * 0.28, s * 0.17
    ax.add_patch(
        Ellipse(
            (sx1 + stem_w / 2, st_bot1 + nh * 0.15),
            nw,
            nh,
            angle=-28,
            facecolor=green,
            edgecolor="none",
        )
    )
    ax.add_patch(
        Ellipse(
            (sx2 + stem_w / 2, st_bot2 + nh * 0.15),
            nw,
            nh,
            angle=-28,
            facecolor=green,
            edgecolor="none",
        )
    )

    fig.canvas.draw()
    buf = np.asarray(fig.canvas.buffer_rgba())
    plt.close(fig)
    img = Image.fromarray(buf)
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    return zero_rgb_where_fully_transparent(img)


def main() -> None:
    here = Path(__file__).resolve().parent
    img1024 = render_player_brand_icon(1024)

    png_out = here / "app.png"
    img1024.save(png_out, "PNG")
    print(f"wrote {png_out}")

    ico_sizes = [256, 128, 64, 48, 32, 16]
    ico_images = [img1024.resize((sz, sz), Image.Resampling.LANCZOS) for sz in ico_sizes]
    ico_out = here / "app.ico"
    ico_images[0].save(
        ico_out,
        format="ICO",
        sizes=[(im.width, im.height) for im in ico_images],
        append_images=ico_images[1:],
    )
    print(f"wrote {ico_out}")

    icns_out = here / "app.icns"
    icns = IcnsFile()
    for name, side in [
        ("icon_16x16.png", 16),
        ("icon_32x32.png", 32),
        ("icon_128x128.png", 128),
        ("icon_256x256.png", 256),
        ("icon_512x512.png", 512),
        ("icon_1024x1024.png", 1024),
    ]:
        tmp = here / name
        img1024.resize((side, side), Image.Resampling.LANCZOS).save(tmp, "PNG")
        icns.add_media(file=str(tmp))
        tmp.unlink()
    icns.write(str(icns_out))
    print(f"wrote {icns_out}")


if __name__ == "__main__":
    main()
