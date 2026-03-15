import struct, zlib, os, math

def make_png(width, height, pixels, filename):
    """pixels: list of (r,g,b,a) tuples, row-major"""
    raw = b''
    for y in range(height):
        row = b'\x00'
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            row += bytes([r, g, b, a])
        raw += row
    compressed = zlib.compress(raw, 9)
    def chunk(t, d):
        c = t + d
        return struct.pack('>I', len(d)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    sig = b'\x89PNG\r\n\x1a\n'
    ihdr = chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    idat = chunk(b'IDAT', compressed)
    iend = chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'wb') as f:
        f.write(sig + ihdr + idat + iend)

def lerp(a, b, t):
    return a + (b - a) * t

def blend(fg, bg):
    """Alpha composite fg over bg"""
    fa = fg[3] / 255
    ba = bg[3] / 255
    oa = fa + ba * (1 - fa)
    if oa == 0:
        return (0, 0, 0, 0)
    r = int((fg[0]*fa + bg[0]*ba*(1-fa)) / oa)
    g = int((fg[1]*fa + bg[1]*ba*(1-fa)) / oa)
    b = int((fg[2]*fa + bg[2]*ba*(1-fa)) / oa)
    a = int(oa * 255)
    return (r, g, b, a)

def draw_circle(pixels, W, H, cx, cy, r, color, aa=True):
    x0, y0 = int(cx - r - 2), int(cy - r - 2)
    x1, y1 = int(cx + r + 2), int(cy + r + 2)
    for y in range(max(0,y0), min(H,y1)):
        for x in range(max(0,x0), min(W,x1)):
            dx, dy = x - cx, y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            if aa:
                alpha = max(0, min(1, r - dist + 0.5))
            else:
                alpha = 1.0 if dist <= r else 0.0
            if alpha > 0:
                fc = (color[0], color[1], color[2], int(color[3] * alpha))
                idx = y * W + x
                pixels[idx] = blend(fc, pixels[idx])

def draw_ellipse(pixels, W, H, cx, cy, rx, ry, color, aa=True):
    for y in range(max(0, int(cy-ry-2)), min(H, int(cy+ry+2))):
        for x in range(max(0, int(cx-rx-2)), min(W, int(cx+rx+2))):
            dx, dy = (x-cx)/rx, (y-cy)/ry
            dist = math.sqrt(dx*dx + dy*dy)
            if aa:
                # edge softness scaled by smaller radius
                edge = min(rx, ry)
                alpha = max(0, min(1, (1 - dist) * edge + 0.5))
            else:
                alpha = 1.0 if dist <= 1 else 0.0
            if alpha > 0:
                fc = (color[0], color[1], color[2], int(color[3]*alpha))
                idx = y * W + x
                pixels[idx] = blend(fc, pixels[idx])

def draw_rounded_rect(pixels, W, H, x0, y0, x1, y1, r, color):
    for y in range(max(0, int(y0)), min(H, int(y1)+1)):
        for x in range(max(0, int(x0)), min(W, int(x1)+1)):
            # distance to nearest corner
            cx = max(x0+r, min(x1-r, x))
            cy = max(y0+r, min(y1-r, y))
            dx, dy = x - cx, y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            alpha = max(0, min(1, r - dist + 0.5))
            if alpha > 0:
                fc = (color[0], color[1], color[2], int(color[3]*alpha))
                idx = y * W + x
                pixels[idx] = blend(fc, pixels[idx])

def draw_triangle(pixels, W, H, pts, color):
    """pts: [(x0,y0),(x1,y1),(x2,y2)]"""
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    for y in range(max(0,int(min(ys))-1), min(H, int(max(ys))+2)):
        for x in range(max(0,int(min(xs))-1), min(W, int(max(xs))+2)):
            # barycentric
            def sign(p1, p2, p3):
                return (p1[0]-p3[0])*(p2[1]-p3[1]) - (p2[0]-p3[0])*(p1[1]-p3[1])
            p = (x, y)
            d1 = sign(p, pts[0], pts[1])
            d2 = sign(p, pts[1], pts[2])
            d3 = sign(p, pts[2], pts[0])
            has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
            has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
            inside = not (has_neg and has_pos)
            if inside:
                fc = color
                idx = y * W + x
                pixels[idx] = blend(fc, pixels[idx])

def make_icon(size):
    W = H = size
    s = size / 192  # scale factor (base design at 192px)
    pixels = [(0, 0, 0, 0)] * (W * H)

    # ── 背景：圆角矩形渐变（暖橙 → 珊瑚红）──
    pad = int(4 * s)
    bg_r = int(40 * s)
    col_top    = (255, 179, 71,  255)   # 暖黄橙
    col_bottom = (255, 100, 80,  255)   # 珊瑚红
    for y in range(H):
        for x in range(W):
            # 圆角裁切
            cx2 = max(pad+bg_r, min(W-pad-bg_r, x))
            cy2 = max(pad+bg_r, min(H-pad-bg_r, y))
            dx2, dy2 = x-cx2, y-cy2
            dist2 = math.sqrt(dx2*dx2+dy2*dy2)
            alpha = max(0, min(1, bg_r - dist2 + 0.5))
            if alpha > 0:
                t = y / H
                r = int(lerp(col_top[0], col_bottom[0], t))
                g = int(lerp(col_top[1], col_bottom[1], t))
                b = int(lerp(col_top[2], col_bottom[2], t))
                pixels[y*W+x] = blend((r, g, b, int(255*alpha)), pixels[y*W+x])

    # ── 猫咪头部（奶白色大圆）──
    head_cx = W * 0.50
    head_cy = H * 0.44
    head_r  = W * 0.30
    CREAM  = (255, 245, 220, 255)
    DARK   = (80,  55,  40,  255)
    PINK   = (255, 182, 193, 255)
    PINK2  = (255, 130, 150, 255)
    draw_circle(pixels, W, H, head_cx, head_cy, head_r, CREAM)

    # ── 猫耳朵（两个三角形）──
    ear_size = W * 0.13
    # 左耳
    lx = head_cx - head_r * 0.55
    ly = head_cy - head_r * 0.78
    draw_triangle(pixels, W, H,
        [(lx, ly - ear_size),
         (lx - ear_size*0.7, ly + ear_size*0.3),
         (lx + ear_size*0.7, ly + ear_size*0.3)],
        CREAM)
    # 左耳内粉
    draw_triangle(pixels, W, H,
        [(lx, ly - ear_size*0.65),
         (lx - ear_size*0.42, ly + ear_size*0.1),
         (lx + ear_size*0.42, ly + ear_size*0.1)],
        PINK)
    # 右耳
    rx2 = head_cx + head_r * 0.55
    ry2 = head_cy - head_r * 0.78
    draw_triangle(pixels, W, H,
        [(rx2, ry2 - ear_size),
         (rx2 - ear_size*0.7, ry2 + ear_size*0.3),
         (rx2 + ear_size*0.7, ry2 + ear_size*0.3)],
        CREAM)
    draw_triangle(pixels, W, H,
        [(rx2, ry2 - ear_size*0.65),
         (rx2 - ear_size*0.42, ry2 + ear_size*0.1),
         (rx2 + ear_size*0.42, ry2 + ear_size*0.1)],
        PINK)

    # ── 眼睛（大眼萌）──
    eye_y   = head_cy - head_r * 0.10
    eye_off = head_r * 0.38
    eye_r   = W * 0.065
    # 白色眼白
    draw_circle(pixels, W, H, head_cx - eye_off, eye_y, eye_r, (255,255,255,255))
    draw_circle(pixels, W, H, head_cx + eye_off, eye_y, eye_r, (255,255,255,255))
    # 深棕瞳孔
    pupil_r = eye_r * 0.58
    draw_circle(pixels, W, H, head_cx - eye_off, eye_y, pupil_r, (60,35,20,255))
    draw_circle(pixels, W, H, head_cx + eye_off, eye_y, pupil_r, (60,35,20,255))
    # 高光
    hl_r = pupil_r * 0.35
    draw_circle(pixels, W, H, head_cx - eye_off + pupil_r*0.3, eye_y - pupil_r*0.3, hl_r, (255,255,255,220))
    draw_circle(pixels, W, H, head_cx + eye_off + pupil_r*0.3, eye_y - pupil_r*0.3, hl_r, (255,255,255,220))

    # ── 小鼻子（粉色椭圆）──
    nose_cx = head_cx
    nose_cy = head_cy + head_r * 0.18
    draw_ellipse(pixels, W, H, nose_cx, nose_cy, W*0.030, H*0.022, PINK2)

    # ── 胡须（横线）──
    WHISKER = (180, 160, 130, 200)
    wlen = W * 0.14
    wy   = nose_cy + H * 0.005
    gap  = W * 0.045
    # 左胡须
    for i in range(3):
        wy2 = wy + (i-1) * H * 0.025
        for dx in range(int(wlen)):
            xp = int(nose_cx - gap - dx)
            yp = int(wy2)
            if 0 <= xp < W and 0 <= yp < H:
                pixels[yp*W+xp] = blend(WHISKER, pixels[yp*W+xp])
    # 右胡须
    for i in range(3):
        wy2 = wy + (i-1) * H * 0.025
        for dx in range(int(wlen)):
            xp = int(nose_cx + gap + dx)
            yp = int(wy2)
            if 0 <= xp < W and 0 <= yp < H:
                pixels[yp*W+xp] = blend(WHISKER, pixels[yp*W+xp])

    # ── 嘴巴（W形小嘴）──
    MOUTH = (180, 100, 80, 220)
    mouth_y = nose_cy + H * 0.04
    mouth_w = W * 0.06
    for dx in range(-int(mouth_w), int(mouth_w)+1):
        x = nose_cx + dx
        # W 曲线：两个抛物线
        if dx < 0:
            y = mouth_y + (dx / mouth_w) ** 2 * H * 0.025
        else:
            y = mouth_y + (dx / mouth_w) ** 2 * H * 0.025
        if 0 <= int(x) < W and 0 <= int(y) < H:
            pixels[int(y)*W+int(x)] = blend(MOUTH, pixels[int(y)*W+int(x)])

    # ── 钱袋（下方）──
    bag_cx = W * 0.50
    bag_cy = H * 0.775
    bag_rx = W * 0.195
    bag_ry = H * 0.155
    GOLD   = (255, 215, 60,  255)
    GOLD2  = (230, 170, 20,  255)
    # 袋子主体（椭圆）
    draw_ellipse(pixels, W, H, bag_cx, bag_cy, bag_rx, bag_ry, GOLD)
    # 袋口（上方小椭圆）
    draw_ellipse(pixels, W, H, bag_cx, bag_cy - bag_ry*0.82, bag_rx*0.38, bag_ry*0.20, GOLD2)
    # 绳结（小圆）
    draw_circle(pixels, W, H, bag_cx, bag_cy - bag_ry*1.05, W*0.038, GOLD2)
    # 钱袋上画 ¥ 符号
    YEN = (180, 120, 0, 240)
    # ¥ 竖线
    yx = int(bag_cx)
    for dy in range(-int(bag_ry*0.45), int(bag_ry*0.55)):
        yy = int(bag_cy + dy)
        if 0 <= yx < W and 0 <= yy < H:
            pixels[yy*W+yx] = blend(YEN, pixels[yy*W+yx])
        if 0 <= yx-1 < W:
            pixels[yy*W+yx-1] = blend(YEN, pixels[yy*W+yx-1])
    # ¥ 两条横线
    for hw in range(-int(bag_rx*0.38), int(bag_rx*0.38)+1):
        for offset in [int(-bag_ry*0.05), int(bag_ry*0.12)]:
            yy = int(bag_cy + offset)
            xx = int(bag_cx + hw)
            if 0 <= xx < W and 0 <= yy < H:
                pixels[yy*W+xx] = blend(YEN, pixels[yy*W+xx])
            if 0 <= yy+1 < H:
                pixels[(yy+1)*W+xx] = blend(YEN, pixels[(yy+1)*W+xx])
    # ¥ 斜线（V形）
    v_w = int(bag_rx * 0.50)
    for i in range(v_w):
        # 左斜
        xx = int(bag_cx - i)
        yy = int(bag_cy - bag_ry*0.45 + i*0.8)
        if 0 <= xx < W and 0 <= yy < H:
            pixels[yy*W+xx] = blend(YEN, pixels[yy*W+xx])
        # 右斜
        xx = int(bag_cx + i)
        if 0 <= xx < W and 0 <= yy < H:
            pixels[yy*W+xx] = blend(YEN, pixels[yy*W+xx])

    # ── 猫爪抱钱袋（两个小圆弧爪）──
    PAW = (255, 235, 200, 255)
    PAW_DARK = (220, 180, 160, 255)
    paw_r = W * 0.085
    # 左爪
    draw_circle(pixels, W, H, bag_cx - bag_rx * 0.78, bag_cy + bag_ry*0.05, paw_r, PAW)
    draw_circle(pixels, W, H, bag_cx - bag_rx * 0.55, bag_cy + bag_ry*0.35, paw_r*0.78, PAW)
    # 左爪趾头
    for toe_off in [-0.7, 0, 0.7]:
        draw_circle(pixels, W, H,
            bag_cx - bag_rx*0.78 + toe_off * paw_r*0.45,
            bag_cy + bag_ry*0.05 - paw_r*0.72,
            paw_r*0.28, PAW_DARK)
    # 右爪
    draw_circle(pixels, W, H, bag_cx + bag_rx * 0.78, bag_cy + bag_ry*0.05, paw_r, PAW)
    draw_circle(pixels, W, H, bag_cx + bag_rx * 0.55, bag_cy + bag_ry*0.35, paw_r*0.78, PAW)
    # 右爪趾头
    for toe_off in [-0.7, 0, 0.7]:
        draw_circle(pixels, W, H,
            bag_cx + bag_rx*0.78 + toe_off * paw_r*0.45,
            bag_cy + bag_ry*0.05 - paw_r*0.72,
            paw_r*0.28, PAW_DARK)

    # ── 小星星装饰（右上角）──
    STAR = (255, 255, 180, 200)
    for (sx, sy, sr) in [
        (W*0.80, H*0.14, W*0.025),
        (W*0.88, H*0.24, W*0.016),
        (W*0.73, H*0.20, W*0.013),
    ]:
        draw_circle(pixels, W, H, sx, sy, sr, STAR)

    return pixels

sizes = {
    'mipmap-mdpi':     48,
    'mipmap-hdpi':     72,
    'mipmap-xhdpi':    96,
    'mipmap-xxhdpi':   144,
    'mipmap-xxxhdpi':  192,
}

base = r'C:\Users\Administrator\WorkBuddy\Claw\app\src\main\res'

for folder, size in sizes.items():
    pixels = make_icon(size)
    path = os.path.join(base, folder, 'ic_launcher.png')
    make_png(size, size, pixels, path)
    # round 版本（圆形裁切）
    pixels_r = list(pixels)
    cx, cy, r = size/2, size/2, size/2
    for y in range(size):
        for x in range(size):
            dx, dy = x-cx, y-cy
            dist = math.sqrt(dx*dx+dy*dy)
            alpha = max(0, min(1, r - dist + 0.5))
            old = pixels_r[y*size+x]
            pixels_r[y*size+x] = (old[0], old[1], old[2], int(old[3]*alpha))
    path_r = os.path.join(base, folder, 'ic_launcher_round.png')
    make_png(size, size, pixels_r, path_r)
    print(f'Done {folder} ({size}px)')

print('All icons generated!')
