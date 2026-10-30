package com.sandevistanmod.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Caminho em grade para o jogador sem controle (apagao): contorna paredes, atravessa portas e portoes (que o cliente abre),
 * sobe e desce escadas de mao, trepadeiras e agua, pula um bloco e desce ate 3 blocos. Evita lava, fogo, cacto e teia.
 */
public final class ClientPath {
    private ClientPath() {}

    private static final class N {
        final BlockPos pos;
        final N parent;
        final float g;
        final float f;
        boolean closed;

        N(BlockPos pos, N parent, float g, float f) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }
    }

    private static final Direction[] DIRS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public static boolean isHandDoor(BlockState s) {
        return (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof FenceGateBlock)
                && !s.is(Blocks.IRON_DOOR);
    }

    private static boolean climbable(Level l, BlockPos p) {
        return l.getBlockState(p).is(BlockTags.CLIMBABLE);
    }

    private static boolean water(Level l, BlockPos p) {
        return l.getBlockState(p).getFluidState().is(FluidTags.WATER);
    }

    private static boolean harmful(BlockState s) {
        return s.getFluidState().is(FluidTags.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CACTUS)
                || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.COBWEB)
                || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.POWDER_SNOW);
    }

    private static boolean passable(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        if (harmful(s)) return false;
        if (s.is(BlockTags.CLIMBABLE) || isHandDoor(s)) return true;
        return s.getCollisionShape(l, p).isEmpty();
    }

    private static boolean supported(Level l, BlockPos p) {
        if (climbable(l, p) || water(l, p)) return true;
        BlockPos b = p.below();
        BlockState s = l.getBlockState(b);
        return !harmful(s) && !s.getCollisionShape(l, b).isEmpty();
    }

    private static boolean standable(Level l, BlockPos p) {
        return passable(l, p) && passable(l, p.above()) && supported(l, p);
    }

    private static float h(BlockPos a, BlockPos b) {
        return (Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ()) + 1.5f * Math.abs(a.getY() - b.getY())) * 1.2f;
    }

    private static boolean reached(BlockPos a, BlockPos goal) {
        return Math.abs(a.getX() - goal.getX()) <= 1 && Math.abs(a.getZ() - goal.getZ()) <= 1
                && Math.abs(a.getY() - goal.getY()) <= 1;
    }

    private static void expand(Level l, BlockPos pos, List<BlockPos> out, List<Float> cost) {
        for (Direction d : DIRS) {
            BlockPos f = pos.relative(d);
            if (standable(l, f)) {
                out.add(f);
                cost.add(1f);
                continue;
            }
            // pular um bloco
            BlockPos up = f.above();
            if (passable(l, pos.above(2)) && standable(l, up)) {
                out.add(up);
                cost.add(1.8f);
                continue;
            }
            // descer ate 3 blocos
            if (passable(l, f) && passable(l, f.above())) {
                for (int k = 1; k <= 3; k++) {
                    BlockPos dn = f.below(k);
                    if (!passable(l, dn)) break;
                    if (standable(l, dn)) {
                        out.add(dn);
                        cost.add(1f + 0.4f * k);
                        break;
                    }
                }
            }
        }
        // escadas de mao, trepadeiras e agua: subir e descer na vertical
        if (climbable(l, pos) || water(l, pos)) {
            BlockPos u = pos.above();
            if (passable(l, u) && passable(l, u.above()) && (climbable(l, u) || water(l, u) || standable(l, u))) {
                out.add(u);
                cost.add(1.2f);
            }
            BlockPos dn = pos.below();
            if (passable(l, dn) && (climbable(l, dn) || water(l, dn) || standable(l, dn))) {
                out.add(dn);
                cost.add(1f);
            }
        }
    }

    /** Caminho do inicio ate perto do objetivo (ou o ponto mais proximo que deu para alcancar). */
    public static List<BlockPos> find(Level level, BlockPos start, BlockPos goal, int maxNodes) {
        PriorityQueue<N> open = new PriorityQueue<>(Comparator.comparingDouble((N n) -> n.f));
        Map<Long, N> seen = new HashMap<>();
        N s = new N(start, null, 0f, h(start, goal));
        open.add(s);
        seen.put(start.asLong(), s);
        N best = s;
        int expanded = 0;
        List<BlockPos> nbs = new ArrayList<>();
        List<Float> costs = new ArrayList<>();
        while (!open.isEmpty() && expanded < maxNodes) {
            N cur = open.poll();
            if (cur.closed) continue;
            cur.closed = true;
            expanded++;
            if (reached(cur.pos, goal)) return build(cur);
            if (h(cur.pos, goal) < h(best.pos, goal)) best = cur;
            nbs.clear();
            costs.clear();
            expand(level, cur.pos, nbs, costs);
            for (int i = 0; i < nbs.size(); i++) {
                BlockPos np = nbs.get(i);
                float g = cur.g + costs.get(i);
                N old = seen.get(np.asLong());
                if (old != null && old.g <= g) continue;
                N n = new N(np, cur, g, g + h(np, goal));
                seen.put(np.asLong(), n);
                open.add(n);
            }
        }
        return build(best);
    }

    private static List<BlockPos> build(N end) {
        List<BlockPos> out = new ArrayList<>();
        for (N n = end; n != null; n = n.parent) out.add(n.pos);
        Collections.reverse(out);
        return out;
    }
}
