package fr.devsylone.fkpi.teams;

import fr.devsylone.fallenkingdom.game.CaptureRunnable;
import fr.devsylone.fallenkingdom.version.Version;
import fr.devsylone.fallenkingdom.version.packet.entity.BossBar;
import fr.devsylone.fkpi.FkPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.devsylone.fallenkingdom.version.Environment.getEntityByUuid;

public class CrystalCore implements Nexus {

    private final Base base;
    private final UUID crystalId;
    private WeakReference<Entity> entity;
    private final BossBar bar;
    private int damage;

    public CrystalCore(@NotNull Base base, @NotNull Entity entity) {
        this(base, entity.getUniqueId(), base.getTeam().getColor().getBukkitChatColor());
    }

    public CrystalCore(@NotNull Base base, @NotNull UUID crystalId, @NotNull ChatColor color) {
        this.base = base;
        this.crystalId = crystalId;
        this.entity = new WeakReference<>(null);
        this.bar = BossBar.INSTANCE.createBossBar("Crystal", color);
    }

    @Override
    public boolean contains(@NotNull Location test) {
        Entity entity = this.entity.get();
        if (entity == null) {
            entity = getEntityByUuid(test.getWorld(), this.crystalId);
            if (entity != null) {
                this.entity = new WeakReference<>(entity);
            }
        }
        return entity != null && entity.getLocation().distanceSquared(test) < 6*6;
    }

    @Override
    public void addEnemyInside(@NotNull Player player) {
          this.bar.addPlayer(player);
    }

    @Override
    public void removeEnemyInside(@NotNull Player player) {
        this.bar.removePlayer(player);
    }

    @Override
    public boolean isInside(@NotNull Player player) {
        return this.bar.getPlayers().contains(player);
    }

    @Override
    public @NotNull Base getBase() {
        return this.base;
    }

    @Override
    public void remove() {
        this.bar.removeAll();
        final Entity entity = this.entity.get();
        if (entity != null) {
            entity.remove();
            this.entity.clear();
        }
    }

    static final String CORE = "core";
    static final String ENTITY = "entity";
    static final String BAR_COLOR = "bar-color";

    @Override
    public void save(@NotNull ConfigurationSection config) {
        config.set("type", CORE);
        config.set(ENTITY, this.crystalId.toString());
    }

    public @NotNull UUID getEntityId() {
        return this.crystalId;
    }

    public void damage(@NotNull Team assailants, int damage) {
        this.damage += damage;
        int coreHealth = FkPI.getInstance().getChestsRoomsManager().getCoreHealth();
        this.bar.setProgress((double) Math.max(0, coreHealth - this.damage) / coreHealth);
        if (this.damage >= coreHealth) {
            final Entity entity = this.entity.get();
            if (entity != null && Version.VersionType.V1_13.isHigherOrEqual()) {
                entity.getWorld().createExplosion(entity.getLocation(), 3, false, false, entity);
            }
            this.base.markNexusAsCaptured();
            CaptureRunnable.run(this.base.getTeam(), assailants);
        }
    }

    @Override
    public String toString() {
        return "CrystalCore{" +
                "team=" + this.base.getTeam() +
                ", inside=" + this.bar.getPlayers().stream().map(Player::getName).collect(Collectors.toList()) +
                ", crystalId=" + this.crystalId +
                ", damage=" + this.damage +
                '}';
    }
}
