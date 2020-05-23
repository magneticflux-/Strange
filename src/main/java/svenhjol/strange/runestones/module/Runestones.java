package svenhjol.strange.runestones.module;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import svenhjol.charm.Charm;
import svenhjol.meson.Meson;
import svenhjol.meson.MesonModule;
import svenhjol.meson.helper.PlayerHelper;
import svenhjol.meson.iface.Config;
import svenhjol.meson.iface.Module;
import svenhjol.strange.Strange;
import svenhjol.strange.base.StrangeCategories;
import svenhjol.strange.base.StrangeSounds;
import svenhjol.strange.base.helper.RunestoneHelper;
import svenhjol.strange.outerlands.module.Outerlands;
import svenhjol.strange.ruins.module.UndergroundRuins;
import svenhjol.strange.runestones.block.AmethystRuneBlock;
import svenhjol.strange.runestones.block.BaseRunestoneBlock;
import svenhjol.strange.runestones.block.RunestoneBlock;
import svenhjol.strange.runestones.capability.*;

import java.util.*;

@Module(mod = Strange.MOD_ID, category = StrangeCategories.RUNESTONES, hasSubscriptions = true,
    description = "Runestones allow fast travel to points of interest in your world by using an Ender Pearl.")
public class Runestones extends MesonModule {
    public static final ResourceLocation RUNESTONES_CAP_ID = new ResourceLocation(Strange.MOD_ID, "runestone_capability");
    public static final List<RunestoneBlock> normalRunestones = new ArrayList<>();
    public static List<Destination> innerDests = new ArrayList<>();
    public static List<Destination> outerDests = new ArrayList<>();
    public static List<Destination> allDests = new ArrayList<>();
    public static List<Destination> ordered = new ArrayList<>();
    public static final Map<UUID, BlockPos> playerTeleportRunestone = new HashMap<>();
    public static final Map<UUID, Map<Integer, BlockPos>> playerTeleportObelisk = new HashMap<>();
    private static final int INTERVAL = 10;

    @Config(name = "Runestone for Quark Big Dungeons", description = "If true, one of the runestones will link to a Big Dungeon from Quark.\n" +
        "This module must be enabled in Quark for the feature to work.\n" +
        "If false, or Quark is not available, this runestone destination will be a stone circle instead.")
    public static boolean useQuarkBigDungeon = true;

    @Config(name = "Allow portal runestones")
    public static boolean allowPortalRunestones = true;

    @Config(name = "Maximum runestone travel distance", description = "Maximum number of blocks that you will be transported from a stone circle runestone.")
    public static int maxDist = 4000;

    @Config(name = "Travel protection duration", description = "Number of seconds of regeneration and slow-fall when travelling through a stone circle runestone.")
    public static int protectionDuration = 6;

    @CapabilityInject(IRunestonesCapability.class)
    public static final Capability<IRunestonesCapability> RUNESTONES = null;

    @Override
    public void init() {
        for (int i = 0; i < 16; i++) {
            normalRunestones.add(new RunestoneBlock(this, i));
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(IRunestonesCapability.class, new RunestonesStorage(), RunestonesCapability::new);
    }

    @Override
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        // add all destinations here; serverStarted shuffles them according to world seed
        // TODO set up constants not magic strings
        ordered = new ArrayList<>();
        ordered.add(new Destination("spawn_point", false, 1.0F));
        ordered.add(new Destination(StoneCircles.RESNAME, "stone_circle", false, 0.9F));
        ordered.add(new Destination(StoneCircles.RESNAME, "outer_stone_circle", true, 0.9F));
        ordered.add(new Destination("Village", "village", false, 0.5F));
        ordered.add(new Destination("Village", "outer_village", true, 0.7F));
        ordered.add(new Destination("Pillager_Outpost", "pillager_outpost", false, 0.75F));
        ordered.add(new Destination("Pillager_Outpost", "outer_pillager_outpost", true, 0.6F));
        ordered.add(new Destination("Desert_Pyramid", "desert_pyramid", false, 0.2F));
        ordered.add(new Destination("Jungle_Pyramid", "jungle_pyramid", false, 0.2F));
        ordered.add(new Destination("Ocean_Ruin", "ocean_ruin", false, 0.65F));
        ordered.add(new Destination("Mineshaft", "mineshaft", false, 0.5F));
        ordered.add(new Destination("Swamp_Hut", "swamp_hut", false, 0.3F));
        ordered.add(new Destination("Igloo", "igloo", false, 0.3F));

        if (useQuarkBigDungeon
            && Charm.quarkCompat != null
            && Charm.quarkCompat.hasBigDungeons()
        ) {
            ordered.add(new Destination(Charm.quarkCompat.getBigDungeonResName(), "big_dungeon", true, 0.3F));
            ordered.add(new Destination(Charm.quarkCompat.getBigDungeonResName(), "big_dungeon", false, 0.3F));
            Strange.LOG.debug("Added Quark's Big Dungeons as a runestone destination");
        } else {
            ordered.add(new Destination(StoneCircles.RESNAME, "stone_circle", true, 0.3F));
            ordered.add(new Destination(StoneCircles.RESNAME, "stone_circle", false, 0.3F));
        }

        if (Meson.isModuleEnabled("strange:underground_ruins")) {
            ordered.add(new Destination(UndergroundRuins.RESNAME, "underground_ruin", false, 0.15F));
            Strange.LOG.debug("Added Underground Ruins as a runestone destination");
        } else {
            ordered.add(new Destination(StoneCircles.RESNAME, "stone_circle", false, 0.15F));
        }
    }

    @Override
    public void onServerStarted(FMLServerStartedEvent event) {
        long seed = event.getServer().getWorld(DimensionType.OVERWORLD).getSeed();
        allDests = new ArrayList<>();
        outerDests = new ArrayList<>();
        innerDests = new ArrayList<>();

        Random rand = new Random();
        rand.setSeed(seed);

        for (Destination dest : ordered) {
            if (dest.outerlands) {
                outerDests.add(dest);
            } else {
                innerDests.add(dest);
            }
        }

        Collections.shuffle(innerDests, rand);
        Collections.shuffle(outerDests, rand);
        allDests.addAll(innerDests);
        allDests.addAll(outerDests);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPearlImpact(ProjectileImpactEvent event) {
        if (!event.getEntity().world.isRemote
            && event.getEntity() instanceof EnderPearlEntity
            && event.getRayTraceResult().getType() == RayTraceResult.Type.BLOCK
            && ((ProjectileItemEntity) event.getEntity()).getThrower() instanceof PlayerEntity
        ) {
            PlayerEntity player = (PlayerEntity) ((ProjectileItemEntity) event.getEntity()).getThrower();
            if (player == null) return; // to prevent null inspection issues below

            World world = event.getEntity().world;
            BlockPos pos = ((BlockRayTraceResult) event.getRayTraceResult()).getPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BaseRunestoneBlock) { // remove ender pearl if it's a runestone collision
                event.setCanceled(true);
                event.getEntity().remove();
            }

            if (block instanceof RunestoneBlock) {
                playerTeleportRunestone.put(player.getUniqueID(), pos);
                effectActivate(world, pos);
            }

            if (allowPortalRunestones && block instanceof AmethystRuneBlock) {
                BlockPos lpos = new BlockPos(pos.down()); // get lowest stone
                while (world.getBlockState(lpos).getBlock() instanceof AmethystRuneBlock && lpos.getY() > 1) {
                    lpos = lpos.add(0, -1, 0);
                }
                if (lpos.getY() <= 1) return;

                BlockPos hpos = new BlockPos(pos.up()); // get highest stone
                while (world.getBlockState(hpos).getBlock() instanceof AmethystRuneBlock && hpos.getY() < 255) {
                    hpos = hpos.add(0, 1, 0);
                }
                if (hpos.getY() >= 255) return;

                int x = pos.getX();
                int z = pos.getZ();
                List<Integer> order = new ArrayList<>();

                for (int i = hpos.getY() - 1; i > lpos.getY(); i--) {
                    BlockPos p = new BlockPos(x, i, z);
                    final BlockState state1 = world.getBlockState(p);
                    final Block block1 = state1.getBlock();
                    if (!(block1 instanceof AmethystRuneBlock)) return;

                    ResourceLocation reg = block1.getRegistryName();
                    if (reg == null) return;

                    int val = getRuneValue((AmethystRuneBlock) block1);
                    final DyeColor dyeColor = null;
                    if (dyeColor == null) {
                        runeError(world, pos, player);
                        return;
                    }

                    order.add(val);
                    order.add(dyeColor.ordinal());
                }

                if (order.isEmpty()) return;

                StringBuilder sb = new StringBuilder();
                for (int o : order) {
                    sb.append(Integer.toHexString(o));
                }
                String s = sb.toString();
                if (s.length() < 5 || s.length() > 18) {
                    runeError(world, pos, player);
                    return;
                }

                long l;
                int dim;

                try {
                    final String d0 = s.substring(3, 4);
                    final String d1 = s.substring(s.length() - 1);
                    final String p0 = s.substring(0, 3);
                    final String p1 = s.substring(4, s.length() - 1);

                    final String dimHex = d0 + d1;
                    final String posHex = p0 + p1;

                    dim = Integer.parseUnsignedInt(dimHex, 16);
                    if (dim < 0 || dim > 255) {
                        runeError(world, pos, player);
                        return;
                    }

                    dim -= 128;
                    Meson.LOG.debug("Dimension: " + dim);

                    Meson.LOG.debug("Trying to parse hex to long: " + posHex);
                    l = Long.parseUnsignedLong(posHex, 16);
                } catch (Exception e) {
                    Meson.LOG.debug("Failed: " + e.getMessage());
                    runeError(world, pos, player);
                    return;
                }

                BlockPos dest = BlockPos.fromLong(l);
                Strange.LOG.debug(dest.toString());

                Map<Integer, BlockPos> location = new HashMap<>();
                location.put(dim, dest);
                playerTeleportObelisk.put(player.getUniqueID(), location);
                effectActivate(world, pos);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END
            && event.player.world.getGameTime() % INTERVAL == 0
            && !event.player.world.isRemote
            && !allDests.isEmpty()
        ) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            ServerWorld world = (ServerWorld) player.world;

            checkLookingAtRune(world, player);
            checkTeleport(world, player);
        }
    }

    // show message when looking at a runestone.  See Entity.java:1433
    private void checkLookingAtRune(ServerWorld world, ServerPlayerEntity player) {
        Vec3d vec3d = player.getEyePosition(1.0F);
        Vec3d vec3d1 = player.getLook(1.0F);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * 6, vec3d1.y * 6, vec3d1.z * 6);

        BlockRayTraceResult result = world.rayTraceBlocks(new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player));
        BlockPos runePos = result.getPos();
        BlockState lookedAt = world.getBlockState(runePos);

        if (lookedAt.getBlock() instanceof RunestoneBlock) {
            RunestoneBlock block = (RunestoneBlock) lookedAt.getBlock();
            if (normalRunestones.contains(block)) {
                TranslationTextComponent message;
                IRunestonesCapability cap = Runestones.getCapability(player);
                int rune = getRuneValue(block);

                if (cap.getDiscoveredTypes().contains(rune)) {
                    Destination dest = allDests.get(rune);
                    TranslationTextComponent description = new TranslationTextComponent("runestone.strange." + dest.description);
                    BlockPos destPos = dest.isSpawnPoint() ? world.getSpawnPoint() : cap.getDestination(runePos);

                    if (destPos != null) {
                        effectTravelled(world, runePos);
                        message = new TranslationTextComponent("runestone.strange.rune_travelled", description, destPos.getX(), destPos.getZ());
                    } else {
                        effectDiscovered(world, runePos);
                        message = new TranslationTextComponent("runestone.strange.rune_connects", description);
                    }
                } else {
                    message = new TranslationTextComponent("runestone.strange.rune_unknown");
                }

                player.sendStatusMessage(message, true);
            }
        }
    }

    private void checkTeleport(ServerWorld world, ServerPlayerEntity player) {
        if (!playerTeleportRunestone.isEmpty() && playerTeleportRunestone.containsKey(player.getUniqueID())) {
            UUID id = player.getUniqueID();
            BlockPos pos = playerTeleportRunestone.get(id);
            doTeleport(world, player, pos);
            playerTeleportRunestone.remove(id);

        } else if (allowPortalRunestones && !playerTeleportObelisk.isEmpty() && playerTeleportObelisk.containsKey(player.getUniqueID())) {
            final UUID id = player.getUniqueID();
            final Map<Integer, BlockPos> location = playerTeleportObelisk.get(id);
            final Optional<Integer> key = location.keySet().stream().findFirst();

            if (key.isPresent()) {
                final int dim = key.get();
                final BlockPos pos = location.get(dim);
                if (player.dimension.getId() != dim)
                    PlayerHelper.changeDimension(player, dim);

                PlayerHelper.teleport(player, pos, dim, p -> {
                    int x = Math.max(-30000000, Math.min(30000000, pos.getX()));
                    int y = Math.max(-64, Math.min(1024, pos.getY()));
                    int z = Math.max(-30000000, Math.min(30000000, pos.getZ()));
                    player.setPositionAndUpdate(x, y, z);
                });
            }
            playerTeleportObelisk.remove(id);
        }
    }

    @SubscribeEvent
    public void onAttachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof PlayerEntity)) return;
        event.addCapability(Runestones.RUNESTONES_CAP_ID, new RunestonesProvider());
    }

    @SubscribeEvent
    public void onPlayerSave(PlayerEvent.SaveToFile event) {
        event.getPlayer().getPersistentData().put(
            Runestones.RUNESTONES_CAP_ID.toString(),
            Runestones.getCapability(event.getPlayer()).writeNBT()
        );
    }

    @SubscribeEvent
    public void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        Runestones.getCapability(event.getPlayer()).readNBT(
            event.getPlayer().getPersistentData()
                .get(Runestones.RUNESTONES_CAP_ID.toString())
        );
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Runestones.getCapability(event.getPlayer()).readNBT(
            event.getPlayer().getPersistentData()
                .get(Runestones.RUNESTONES_CAP_ID.toString())
        );
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getPlayer().getPersistentData().put(
            Runestones.RUNESTONES_CAP_ID.toString(),
            Runestones.getCapability(event.getPlayer()).writeNBT()
        );
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        IRunestonesCapability oldCap = Runestones.getCapability(event.getOriginal());
        IRunestonesCapability newCap = Runestones.getCapability(event.getPlayer());
        newCap.readNBT(oldCap.writeNBT());
    }

    @SuppressWarnings("ALL") // what
    public static IRunestonesCapability getCapability(PlayerEntity player) {
        return player.getCapability(RUNESTONES, null).orElse(new DummyCapability());
    }

    public static BlockState getRunestoneBlock(int runeValue) {
        return normalRunestones.get(runeValue).getDefaultState();
    }

    public static int getRuneValue(BaseRunestoneBlock block) {
        return block.getRuneValue();
    }

    public static BlockState getRandomBlock(BlockPos pos) {
        List<Destination> pool = Outerlands.isOuterPos(pos) ? allDests : innerDests;
        return getRunestoneBlock(new Random(pos.toLong()).nextInt(pool.size()));
    }

    public static BlockPos getInnerPos(World world, Random rand) {
        return Outerlands.getInnerPos(world, rand);
    }

    public static BlockPos getOuterPos(World world, Random rand) {
        return Outerlands.getOuterPos(world, rand);
    }

    public static BlockPos normalizeInnerPos(BlockPos pos) {
        if (!Meson.isModuleEnabled("strange:outerlands"))
            return pos;

        int x = pos.getX();
        int z = pos.getZ();
        int nx = x;
        int nz = z;

        if (Math.abs(x) > Math.abs(z)) {
            if (x <= 0 && x <= -Outerlands.threshold) {
                nx = -Outerlands.threshold;
            } else if (x > 0 && x > Outerlands.threshold) {
                nx = Outerlands.threshold;
            }
        } else if (Math.abs(x) < Math.abs(z)) {
            if (z <= 0 && z <= -Outerlands.threshold) {
                nz = -Outerlands.threshold;
            } else if (z > 0 && z > Outerlands.threshold) {
                nz = Outerlands.threshold;
            }
        }

        return new BlockPos(nx, pos.getY(), nz);
    }

    public static BlockPos normalizeOuterPos(BlockPos pos) {
        if (!Meson.isModuleEnabled("strange:outerlands"))
            return pos;

        int x = pos.getX();
        int z = pos.getZ();
        int nx = x;
        int nz = z;

        if (Math.abs(x) > Math.abs(z)) {
            if (x <= 0 && x >= -Outerlands.threshold) {
                nx = -Outerlands.threshold;
            } else if (x > 0 && x < Outerlands.threshold) {
                nx = Outerlands.threshold;
            }
        } else if (Math.abs(x) < Math.abs(z)) {
            if (z <= 0 && z >= -Outerlands.threshold) {
                nz = -Outerlands.threshold;
            } else if (z > 0 && z < Outerlands.threshold) {
                nz = Outerlands.threshold;
            }
        }

        return new BlockPos(nx, pos.getY(), nz);
    }


    private BlockPos addRandomOffset(BlockPos pos, Random rand) {
        return RunestoneHelper.addRandomOffset(pos, rand, 8);
    }

    private void doTeleport(ServerWorld world, PlayerEntity player, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof RunestoneBlock)) return;
        int rune = getRuneValue((RunestoneBlock) state.getBlock());

        CriteriaTriggers.ENTER_BLOCK.trigger((ServerPlayerEntity) player, state);
        Runestones.getCapability(player).discoverType(rune);

        int duration = protectionDuration * 20;
        player.addPotionEffect(new EffectInstance(Effects.SLOW_FALLING, duration, 2));
        player.addPotionEffect(new EffectInstance(Effects.RESISTANCE, duration, 2));
        final BlockPos currentPlayerPos = player.getPosition();

        Random rand = world.rand;
        rand.setSeed(pos.toLong());

        if (player.dimension.getId() != 0)
            PlayerHelper.changeDimension(player, 0);

        Destination dest = allDests.get(rune);
        Strange.LOG.debug("Rune: " + rune + ", dest: " + dest.description);
        BlockPos destPos = dest.getDest(world, pos, rand);

        if (destPos == null) {
            Strange.LOG.warn("Dest position invalid, defaulting to spawn position");
            destPos = world.getSpawnPoint();
        }

        BlockPos destOffset = addRandomOffset(destPos, rand);
        PlayerHelper.teleportSurface(player, destOffset, 0, p1 -> {
            Runestones.getCapability(player).recordDestination(pos, destOffset);
            PlayerHelper.teleport(player, currentPlayerPos, 0,
                p2 -> PlayerHelper.teleportSurface(player, destOffset, 0, p3 -> {
                    final BlockPos playerPos = p3.getPosition();
                    p3.setPositionAndUpdate(playerPos.getX(), playerPos.getY() + 2, playerPos.getZ());
            }));
        });
    }

    private void runeError(World world, BlockPos pos, PlayerEntity player) {
        player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 5 * 20));
        world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 1.5F, Explosion.Mode.NONE);
    }

    public static void effectActivate(World world, BlockPos pos) {
        double spread = 0.75D;
        double px = pos.getX() + 0.5D + (Math.random() - 0.5D) * spread;
        double py = pos.getY() + 0.5D + (Math.random() - 0.5D) * spread;
        double pz = pos.getZ() + 0.5D + (Math.random() - 0.5D) * spread;
        ((ServerWorld) world).spawnParticle(ParticleTypes.PORTAL, px, py, pz, 10, 0.3D, 0.3D, 0.3D, 0.3D);
        world.playSound(null, pos, StrangeSounds.RUNESTONE_TRAVEL, SoundCategory.PLAYERS, 0.6F, 1.05F);
    }

    public static void effectTravelled(World world, BlockPos pos) {
        double spread = 1.5D;
        double px = pos.getX() + 0.5D + (Math.random() - 0.5D) * spread;
        double py = pos.getY() + 0.5D + (Math.random() - 0.5D) * spread;
        double pz = pos.getZ() + 0.5D + (Math.random() - 0.5D) * spread;
        ((ServerWorld) world).spawnParticle(ParticleTypes.ENCHANT, px, py, pz, 10, 0.2D, 0.6D, 0.2D, 0.45D);
    }

    public static void effectDiscovered(World world, BlockPos pos) {
        double spread = 1.75D;
        if (world.rand.nextFloat() < 0.6F) {
            double px = pos.getX() + 0.5D + (Math.random() - 0.5D) * spread;
            double py = pos.getY() + 0.5D + (Math.random() - 0.5D) * spread;
            double pz = pos.getZ() + 0.5D + (Math.random() - 0.5D) * spread;
            ((ServerWorld) world).spawnParticle(ParticleTypes.ENCHANT, px, py, pz, 3, 0.15D, 0.3D, 0.15D, 0.2D);
        }
    }

    @SuppressWarnings("unused")
    public static class Destination {
        public final static String SPAWN = "spawn";
        private final static int dist = 100;

        public String structure;
        public String description;
        public boolean outerlands;
        public float weight;

        public Destination(String structure, String description, boolean outerlands, float weight) {
            this.structure = structure;
            this.description = description;
            this.outerlands = outerlands;
            this.weight = weight;
        }

        public Destination(String description, boolean outerlands, float weight) {
            this(SPAWN, description, outerlands, weight);
        }

        public boolean isSpawnPoint() {
            return this.structure.equals(SPAWN);
        }

        public BlockPos getDest(ServerWorld world, BlockPos runePos, Random rand) {
            Strange.LOG.debug("Structure: " + structure);

            BlockPos spawn = world.getSpawnPoint();
            final int x = runePos.getX();
            final int z = runePos.getZ();
            final WorldBorder border = world.getWorldBorder();

            if (isSpawnPoint())
                return spawn;

            final int xdist = -maxDist + rand.nextInt(maxDist *2);
            final int zdist = -maxDist + rand.nextInt(maxDist *2);
            BlockPos p = runePos.add(xdist, 0, zdist);

            if (p.getX() > border.maxX())
                p = new BlockPos(border.maxX(), p.getY(), p.getZ());

            if (p.getX() < border.minX())
                p = new BlockPos(border.minX(), p.getY(), p.getZ());

            if (p.getZ() > border.maxZ())
                p = new BlockPos(p.getX(), p.getY(), border.maxZ());

            if (p.getZ() < border.minZ())
                p = new BlockPos(p.getX(), p.getY(), border.minZ());

            BlockPos target;

            if (outerlands) {
                target = getOuterPos(world, rand); // get a random outerlands pos
            } else if (Outerlands.isOuterPos(runePos)) {
                target = normalizeOuterPos(p); // if you're in the outerlands, find a close-by outerlands pos
            } else {
                target = normalizeInnerPos(p); // if you're not in outerlands, find a close-by inner pos
            }

            BlockPos dest = world.findNearestStructure(structure, target, dist, true);
            return dest == null ? world.getSpawnPoint() : dest;
        }
    }
}
