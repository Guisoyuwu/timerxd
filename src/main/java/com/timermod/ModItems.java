package com.timermod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public final class ModItems {

    /** Palo "A casa Pa": te saca del server. */
    public static class KickStick extends Item {
        public KickStick(Settings s) { super(s); }
        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            if (!world.isClient && user instanceof ServerPlayerEntity sp) {
                sp.networkHandler.disconnect(Text.literal("¡A casa Pa!"));
            }
            return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
        }
    }

    /** Palo que te teletransporta a X=0 Z=0. */
    public static class TpZeroStick extends Item {
        public TpZeroStick(Settings s) { super(s); }
        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            if (world instanceof ServerWorld sw && user instanceof ServerPlayerEntity sp) {
                sw.getChunk(0, 0); // fuerza la carga del chunk
                int y = sw.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                if (sw.getDimension().hasCeiling()) { // Nether: buscar hueco libre
                    for (int yy = sw.getLogicalHeight() - 2; yy > sw.getBottomY() + 1; yy--) {
                        BlockPos p = new BlockPos(0, yy, 0);
                        if (sw.getBlockState(p).isAir() && sw.getBlockState(p.up()).isAir()
                                && !sw.getBlockState(p.down()).getCollisionShape(sw, p.down()).isEmpty()) {
                            y = yy;
                            break;
                        }
                    }
                }
                sp.teleport(sw, 0.5, y, 0.5, sp.getYaw(), sp.getPitch());
                sp.getItemCooldownManager().set(this, 20);
            }
            return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
        }
    }

    public static final Item A_CASA_PA = new KickStick(new Item.Settings().maxCount(1));
    public static final Item TP_CERO = new TpZeroStick(new Item.Settings().maxCount(1));
    public static final Item ICHIGO_SWORD = new IchigoSword(new Item.Settings().maxCount(1).fireproof());

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(TimerMod.MOD_ID, "a_casa_pa"), A_CASA_PA);
        Registry.register(Registries.ITEM, new Identifier(TimerMod.MOD_ID, "tp_cero"), TP_CERO);
        Registry.register(Registries.ITEM, new Identifier(TimerMod.MOD_ID, "ichigo_sword"), ICHIGO_SWORD);

        Registry.register(Registries.ITEM_GROUP, new Identifier(TimerMod.MOD_ID, "main"),
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.timermod.main"))
                        .icon(() -> new ItemStack(ICHIGO_SWORD))
                        .entries((ctx, e) -> {
                            e.add(A_CASA_PA);
                            e.add(TP_CERO);
                            e.add(ICHIGO_SWORD);
                        })
                        .build());
    }

    private ModItems() {}
}
