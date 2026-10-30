"""Texturas da versao 4.0.0: Braco de Ferro, Link Neural, Chipware, os 4 chips e o Soquete de Chipware (entidade)."""
import os
from gen_textures2 import *  # noqa: F401,F403  (BASE, cores e funcoes de desenho)

PCB = (22, 44, 58, 255)
PCB2 = (14, 28, 40, 255)
GOLD = (255, 205, 60, 255)
GOLD2 = (190, 140, 30, 255)
WHITE = (235, 245, 255, 255)
ORANGE = (255, 140, 30, 255)
BLUE = (60, 130, 255, 255)


def chip_base(accent, accent2, pattern):
    im, px = new()
    rect(px, 3, 1, 12, 13, PCB)
    rect(px, 3, 1, 12, 1, MET3)
    rect(px, 3, 1, 3, 13, MET)
    rect(px, 12, 1, 12, 13, PCB2)
    rect(px, 3, 13, 12, 13, PCB2)
    # contatos dourados
    for x in (4, 6, 8, 10):
        rect(px, x, 14, x + 1, 15, GOLD if x % 4 == 0 else GOLD2)
    # nucleo (die)
    rect(px, 5, 4, 10, 9, accent2)
    rect(px, 6, 5, 9, 8, accent)
    for (x, y) in pattern:
        px[x, y] = WHITE
    # trilhas
    for y in (10, 11):
        px[5, y] = accent
        px[10, y] = accent
    return outline(im)


def chipware():
    return chip_base(MET3, MET2, [(7, 6), (8, 7)])


def chip_martial():
    # punho / duas linhas cruzadas
    return chip_base(RD, (120, 12, 20, 255), [(6, 5), (7, 6), (8, 7), (9, 8), (9, 5), (8, 6), (7, 7), (6, 8)])


def chip_pain():
    # cruz de cura
    return chip_base(GR, GR2, [(7, 5), (8, 5), (7, 8), (8, 8), (6, 6), (6, 7), (9, 6), (9, 7), (7, 6), (8, 7)])


def chip_mining():
    # picareta
    return chip_base(ORANGE, (150, 70, 10, 255), [(6, 5), (7, 5), (8, 5), (9, 6), (7, 6), (7, 7), (7, 8)])


def chip_walker():
    # bota / seta para cima
    return chip_base(CY, (20, 120, 160, 255), [(7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8)])


def braco_ferro():
    im, px = new()
    IR = (216, 220, 224, 255)
    IR2 = (170, 176, 184, 255)
    IR3 = (245, 247, 250, 255)
    # antebraco (bloco de ferro) e mao
    rect(px, 5, 1, 10, 10, IR)
    rect(px, 5, 1, 10, 1, IR3)
    rect(px, 5, 1, 5, 10, IR3)
    rect(px, 10, 1, 10, 10, IR2)
    rect(px, 5, 10, 10, 10, IR2)
    rect(px, 4, 11, 11, 14, MET2)
    rect(px, 4, 11, 11, 11, MET)
    # dedos
    for x in (4, 6, 8, 10):
        rect(px, x, 14, x + 1, 15, MET3)
    # rebites e luz verde
    for p in ((6, 3), (9, 3), (6, 8), (9, 8)):
        px[p] = MET2
    rect(px, 7, 4, 8, 7, GR)
    px[7, 5] = (200, 255, 190, 255)
    return outline(im)


def link_neural():
    im, px = new()
    # coluna com conector e no ciano
    rect(px, 6, 1, 9, 14, MET2)
    rect(px, 6, 1, 6, 14, MET)
    rect(px, 9, 1, 9, 14, K)
    for y in (3, 6, 9, 12):
        rect(px, 4, y, 11, y, MET3)
        rect(px, 5, y + 1, 10, y + 1, MET2)
    rect(px, 7, 7, 8, 8, CY)
    px[7, 7] = WHITE
    px[7, 1] = GR
    px[8, 1] = GR
    px[7, 14] = GR2
    px[8, 14] = GR2
    return outline(im)


def socket():
    """Soquete de Chipware: duas barras verticais e um disco com cruz de quatro lobulos."""
    im, px = new()
    # barras
    for x0 in (0, 14):
        rect(px, x0, 1, x0 + 1, 14, MET2)
        rect(px, x0, 1, x0, 14, MET3)
        rect(px, x0 + 1, 1, x0 + 1, 14, K)
        for y in (3, 7, 11):
            px[x0, y] = CY
    # disco
    cx, cy = 7.5, 7.5
    for x in range(16):
        for y in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 5.6:
                px[x, y] = MET2 if d > 4.6 else K
    # cruz de quatro lobulos
    rect(px, 7, 3, 8, 12, MET3)
    rect(px, 3, 7, 12, 8, MET3)
    for p in ((7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)):
        px[p] = MET2
    rect(px, 7, 7, 8, 8, CY)
    return im


ITEMS3 = {"chipware": chipware, "chip_artes_marciais": chip_martial, "chip_supressao_dor": chip_pain,
          "chip_mineracao": chip_mining, "chip_andarilho": chip_walker, "braco_ferro": braco_ferro,
          "link_neural": link_neural}

if __name__ == "__main__":
    os.makedirs(os.path.join(BASE, "item"), exist_ok=True)
    os.makedirs(os.path.join(BASE, "entity"), exist_ok=True)
    for n, f in ITEMS3.items():
        f().save(os.path.join(BASE, "item", n + ".png"))
    socket().save(os.path.join(BASE, "entity", "chipware_socket.png"))
