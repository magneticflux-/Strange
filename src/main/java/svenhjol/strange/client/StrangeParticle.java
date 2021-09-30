package svenhjol.strange.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.Random;

@SuppressWarnings("ClassCanBeRecord")
@Environment(EnvType.CLIENT)
public class StrangeParticle extends TextureSheetParticle {
    private static final Random RANDOM = new Random();
    private final SpriteSet spriteProvider;

    // copypasta from GlowParticle#<init>
    public StrangeParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteProvider) {
        super(world, x, y, z, vx, vy, vz);
        this.friction = 0.6F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.spriteProvider = spriteProvider;
        this.quadSize *= 0.78F;
        this.hasPhysics = false;
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // copypasta from PortalParticle#getBrightness
    @Override
    public int getLightColor(float tint) {
        int i = super.getLightColor(tint);
        float f = (float) this.age / (float) this.lifetime;
        f *= f;
        f *= f;
        int j = i & 255;
        int k = i >> 16 & 255;
        k += (int) (f * 15.0F * 16.0F);
        if (k > 240) {
            k = 240;
        }

        return j | k << 16;
    }

    // copypasta from GlowParticle#tick
    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteProvider);
    }

    public static class AxisFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public AxisFactory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double r, double g, double b) {
            StrangeParticle particle = new StrangeParticle(world, x, y, z, 0.5D, 0.5D, 0.5D, this.spriteProvider);
            particle.setLifetime(80 + RANDOM.nextInt(20));
            particle.setColor((float) r, (float) g, (float) b);
            particle.setAlpha(0.81F);
            particle.friction = 0.1F; // some multiplier for velocity, idk
            particle.speedUpWhenYMotionIsBlocked = true;
            return particle;
        }
    }

    public static class OreGlowFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public OreGlowFactory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double r, double g, double b) {
            StrangeParticle particle = new StrangeParticle(world, x, y, z, 0.06D, 0.6D - RANDOM.nextDouble(), 0.05D, this.spriteProvider);
            particle.setLifetime(25 + RANDOM.nextInt(2));
            particle.setColor((float)r, (float)g, (float)b);
            particle.setAlpha((RANDOM.nextFloat() * 0.2F) + 0.8F);
            particle.friction = 0.7F; // some multiplier for velocity, idk
            particle.speedUpWhenYMotionIsBlocked = true;
            return particle;
        }
    }

    public static class ApplyFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public ApplyFactory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double r, double g, double b) {
            StrangeParticle particle = new StrangeParticle(world, x, y, z, 0.5D - RANDOM.nextDouble(), 0.5D - RANDOM.nextDouble(), 0.5D - RANDOM.nextDouble(), this.spriteProvider);
            particle.setLifetime(4 + RANDOM.nextInt(4));
            particle.setColor((float)r, (float)g, (float)b);
            particle.setAlpha((RANDOM.nextFloat() * 0.2F) + 0.8F);
            particle.friction = 0.8F; // some multiplier for velocity, idk
            particle.speedUpWhenYMotionIsBlocked = true; // dunno
            return particle;
        }
    }
}
