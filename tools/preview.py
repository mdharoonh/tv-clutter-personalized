"""Approximate renders of the launcher layout (same maths as the Java) for checking placement without a device."""
import json, os, sys
from PIL import Image, ImageDraw, ImageFont, ImageOps, ImageEnhance, ImageFilter
A = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets')
OUT = sys.argv[1]
cfg = json.load(open(A + '/config.json', encoding='utf-8'))
W, H = 1920, 1080
font = lambda s: ImageFont.truetype(A + '/fonts/outfit.ttf', int(s))

def radial(sel):
    bg = ImageOps.fit(Image.open(A + '/radial/bg.jpg').convert('RGB'), (W, H))
    im = bg.convert('RGBA'); d = ImageDraw.Draw(im, 'RGBA')
    cx, cy, ring = W / 2, H * .5, H * .4
    d.ellipse([cx - ring, cy - ring, cx + ring, cy + ring], outline=(255, 255, 255, 50), width=2)
    p = Image.open(A + '/radial/%s.png' % sel).convert('RGBA'); ph = H * .6; pw = ph * p.width / p.height
    im.alpha_composite(p.resize((int(pw), int(ph))), (int(cx - pw / 2), int(cy - ph / 2)))
    for k, (x, y) in dict(up=(cx, cy - ring), down=(cx, cy + ring), left=(cx - ring * 1.55, cy), right=(cx + ring * 1.55, cy)).items():
        s = cfg['radial'][k]; label = s.get('label') or cfg['apps'][s['app']]['label']
        pw_, ph_ = H * .3, H * .1; on = k == sel
        d.rounded_rectangle([x - pw_ / 2, y - ph_ / 2, x + pw_ / 2, y + ph_ / 2], ph_ / 2, fill=(8, 10, 22, 150), outline=(255, 255, 255, 90) if not on else '#ffd', width=4 if on else 2)
        d.text((x, y), label, font=font(ph_ * .42), fill='white', anchor='mm')
    return im

def fav(sel):
    im = Image.new('RGBA', (W, H), (6, 8, 14, 255))
    f = cfg['favourites'][sel]
    sc = ImageOps.fit(Image.open(A + '/' + f['scene']).convert('L'), (W, H)).point(lambda v: int(v * .45)).convert('RGBA')
    im.alpha_composite(sc)
    cw = int(W * .86); ch = int(cw * cfg['favAspect']); cl = (W - cw) // 2; ct = int(H * .9) - ch
    col = tuple(int(f['color'][i:i + 2], 16) for i in (1, 3, 5))
    g = Image.new('RGBA', (W, H), (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
    gd.ellipse([cl - cw // 8, ct - ch // 4, cl + cw * 9 // 8, ct + ch * 3 // 2 - ch // 4 + ch // 4], fill=col + (110,))
    im.alpha_composite(g.filter(ImageFilter.GaussianBlur(120)))
    order = sorted(range(len(cfg['favourites'])), key=lambda i: ((20 if cfg['favourites'][i]['row'] == 'front' else 0) + (5 if i == sel else 0)))
    for i in order:
        c = cfg['favourites'][i]; p = Image.open(A + '/' + c['cutout']).convert('RGBA')
        w, h = int(c['w'] * cw), int(c['h'] * ch); p = p.resize((w, h)); on = i == sel
        if not on:
            a = p.getchannel('A').point(lambda v: int(v * .7)); p = ImageOps.grayscale(p.convert('RGB')).convert('RGBA'); p.putalpha(a)
        else:
            w2, h2 = int(w * 1.12), int(h * 1.12); p = p.resize((w2, h2)); w, h = w2, h2
        x = cl + c['x'] * cw + (c['w'] * cw - w) / 2; y = ct + (c['y'] + c['h']) * ch - h
        im.alpha_composite(p, (int(x), int(y)))
    ly = ct + int(ch * .75); grad = Image.new('RGBA', (cw, H - ly))
    gd = ImageDraw.Draw(grad)
    for yy in range(grad.height): gd.line([(0, yy), (cw, yy)], fill=(6, 8, 14, int(255 * yy / grad.height)))
    im.alpha_composite(grad, (cl, ly))
    d = ImageDraw.Draw(im); d.text((W / 2, ct - H * .15), f['title'], font=font(H * .06), fill='white', anchor='mm')
    a = cfg['apps'][f['app']]; d.text((W / 2, ct - H * .07), a['label'], font=font(H * .03), fill='white', anchor='mm')
    return im

for k in ('idle','up','left','right','down'): radial(k).convert('RGB').save(OUT + '/r_%s.jpg' % k)
fav(2).convert('RGB').save(OUT + '/f_neeya.jpg'); fav(1).convert('RGB').save(OUT + '/f_heart.jpg')
s = Image.new('RGB', (1920, 1080))
for i, n in enumerate(['r_idle', 'r_up', 'r_left', 'r_right']):
    t = Image.open(OUT + '/%s.jpg' % n); t.thumbnail((960, 540)); s.paste(t, ((i % 2) * 960, (i // 2) * 540))
s.save(OUT + '/sheet2.jpg')
