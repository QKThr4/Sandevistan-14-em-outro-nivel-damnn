"""Texturas da versao 4.0.0 (2a leva): variantes coloridas do Sandevistan e das pecas, Link Neural pixelado,
Braco de Ferro retangular, Estrutura Ossea e Sistema Nervoso."""
import os
from PIL import Image
from gen_textures2 import BASE, K, T, MET, MET2, MET3, CY, GR, new, rect, outline  # noqa: F401

APOGEE_MAIN = (51, 255, 0, 255)
VARIANTS = {
    "falcon": (31, 168, 74),
    "dynalar": (47, 140, 255),
    "zetatech": (194, 198, 138),
    "qiant": (255, 138, 30),
}


def recolor(src, color):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    w, h = im.size
    for x in range(w):
        for y in range(h):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if (r, g, b) == APOGEE_MAIN[:3]:
                px[x, y] = color + (a,)
            elif g > r and g > b and g > 20 and (r, g, b) != (0, 0, 0):
                k = g / 255.0
                px[x, y] = (int(color[0] * k * 0.9), int(color[1] * k * 0.9), int(color[2] * k * 0.9), a)
    return im


def variants():
    item = os.path.join(BASE, "item")
    ent = os.path.join(BASE, "entity")
    for v, c in VARIANTS.items():
        for part in ("acelerador", "espinha", "neuralink"):
            recolor(os.path.join(item, part + ".png"), c).save(os.path.join(item, part + "_" + v + ".png"))
        recolor(os.path.join(item, "sandevistan.png"), c).save(os.path.join(item, "sandevistan_" + v + ".png"))
        recolor(os.path.join(ent, "sandevistan_back.png"), c).save(os.path.join(ent, "sandevistan_back_" + v + ".png"))


# ---- Link Neural: duas barras verticais em cima e um disco de quatro lobulos embaixo ----
D1 = (48, 50, 54, 255)
D2 = (30, 32, 36, 255)
D3 = (74, 77, 84, 255)
S1 = (205, 210, 218, 255)
S2 = (140, 146, 156, 255)


def bar(px, x0, y0, h):
    """Barra de metal (3 de largura) com bordas claras, como as do conector real."""
    rect(px, x0, y0, x0 + 2, y0 + h - 1, D2)
    for y in range(y0, y0 + h):
        px[x0, y] = S1
        px[x0 + 2, y] = S2
    px[x0 + 1, y0] = S1
    px[x0 + 1, y0 + h - 1] = S2
    px[x0 + 1, y0 + h // 2] = D3


def disc(px, ox, oy, size):
    """Disco de quatro lobulos escuros com miolo prateado e furo no centro. size = 12 (par)."""
    c = (size - 1) / 2.0
    for x in range(size):
        for y in range(size):
            dx, dy = x - c, y - c
            ax, ay = abs(dx), abs(dy)
            lobe = (ax <= 2.6 and ay <= c) or (ay <= 2.6 and ax <= c)
            lobe = lobe or (ax + ay) <= c + 1.2
            # recorta as pontas para ficarem arredondadas
            if (ax > c - 0.4 and ay > 1.8) or (ay > c - 0.4 and ax > 1.8):
                lobe = False
            if not lobe:
                continue
            col = D1
            if dx < -1 and dy < -1:
                col = D3
            if dx > 1 and dy > 1:
                col = D2
            px[ox + x, oy + y] = col
    # fendas entre os lobulos (diagonais) e bordas
    for t in range(2, size // 2 - 1):
        for sx, sy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
            xx = int(round(c + sx * (t + 0.5)))
            yy = int(round(c + sy * (t + 0.5)))
            if 0 <= xx < size and 0 <= yy < size and px[ox + xx, oy + yy][3] > 0:
                px[ox + xx, oy + yy] = D2
    # miolo prateado + furo
    for x in range(size):
        for y in range(size):
            if ((x - c) ** 2 + (y - c) ** 2) ** 0.5 <= 2.3:
                px[ox + x, oy + y] = S2
    for x in (size // 2 - 1, size // 2):
        for y in (size // 2 - 1, size // 2):
            px[ox + x, oy + y] = K
    # grampos prateados nas quatro direcoes
    for (x, y) in ((size // 2 - 1, 1), (size // 2, 1), (size // 2 - 1, size - 2), (size // 2, size - 2),
                   (1, size // 2 - 1), (1, size // 2), (size - 2, size // 2 - 1), (size - 2, size // 2)):
        px[ox + x, oy + y] = S1


def link_neural_item():
    im, px = new()
    bar(px, 4, 0, 4)
    bar(px, 9, 0, 4)
    # nao tem cabecas largas: duas barras com uma presilha
    disc(px, 2, 4, 12)
    return outline(im)


def socket_entity():
    """x 0..3 barra (4x7); x 4..15 disco (12x12)."""
    im, px = new()
    bar(px, 0, 0, 7)
    disc(px, 4, 0, 12)
    return im


# ---- Braco de Ferro: so um retangulo com cara de bloco de ferro ----
def braco_ferro():
    im, px = new()
    IR = (220, 220, 220, 255)
    IR2 = (198, 198, 198, 255)
    IR3 = (240, 240, 240, 255)
    IR4 = (168, 168, 168, 255)
    rect(px, 4, 1, 11, 14, IR)
    for y in range(1, 15):
        for x in range(4, 12):
            if (x * 7 + y * 13) % 5 == 0:
                px[x, y] = IR2
            elif (x * 11 + y * 3) % 7 == 0:
                px[x, y] = IR3
    rect(px, 4, 1, 11, 1, IR3)
    rect(px, 4, 1, 4, 14, IR3)
    rect(px, 11, 2, 11, 14, IR4)
    rect(px, 5, 14, 11, 14, IR4)
    return outline(im)


# ---- Estrutura Ossea Reforcada: osso longo com placas de aco ----
def estrutura_ossea():
    im, px = new()
    B1 = (240, 236, 222, 255)
    B2 = (200, 194, 172, 255)
    # osso diagonal
    for i in range(10):
        x, y = 3 + i, 12 - i
        px[x, y] = B1
        px[x + 1, y] = B2
        px[x, y - 1] = B1
    for (x, y) in ((2, 12), (3, 13), (2, 13), (12, 3), (13, 2), (12, 2), (13, 3)):
        px[x, y] = B1
    # placas de aco e luz ciano
    for (x, y) in ((6, 9), (7, 8), (8, 7), (9, 6)):
        px[x, y] = MET3
        px[x + 1, y] = MET2
    px[7, 7] = CY
    px[8, 8] = CY
    return outline(im)


# ---- Sistema Nervoso Aprimorado: no nervoso com fibras ----
def sistema_nervoso():
    im, px = new()
    Y1 = (255, 214, 64, 255)
    Y2 = (200, 150, 20, 255)
    # tronco e ramos
    rect(px, 7, 2, 8, 13, Y2)
    for y in range(2, 14):
        px[7, y] = Y1
    for (x, y) in ((6, 4), (5, 3), (4, 2), (9, 5), (10, 4), (11, 3), (6, 8), (5, 9), (4, 10), (9, 9), (10, 10), (11, 11)):
        px[x, y] = Y1 if (x + y) % 2 == 0 else Y2
    for (x, y) in ((4, 1), (11, 2), (3, 10), (12, 11)):
        px[x, y] = CY
    rect(px, 6, 6, 9, 7, CY)
    px[7, 6] = (255, 255, 255, 255)
    return outline(im)


if __name__ == "__main__":
    variants()
    item = os.path.join(BASE, "item")
    ent = os.path.join(BASE, "entity")
    link_neural_item().save(os.path.join(item, "link_neural.png"))
    socket_entity().save(os.path.join(ent, "chipware_socket.png"))
    braco_ferro().save(os.path.join(item, "braco_ferro.png"))
    estrutura_ossea().save(os.path.join(item, "estrutura_ossea.png"))
    sistema_nervoso().save(os.path.join(item, "sistema_nervoso.png"))
    models = os.path.join(BASE, "..", "models", "item")
    names = ["estrutura_ossea", "sistema_nervoso"]
    for v in VARIANTS:
        names += [p + "_" + v for p in ("acelerador", "espinha", "neuralink", "sandevistan")]
    for n in names:
        with open(os.path.join(models, n + ".json"), "w") as f:
            f.write('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "sandevistan:item/%s"\n  }\n}\n' % n)
