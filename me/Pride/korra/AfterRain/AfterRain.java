package me.Pride.korra.AfterRain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import net.md_5.bungee.api.ChatColor;

public class AfterRain extends WaterAbility implements AddonAbility {
	
	private static final String PATH = "ExtraAbilities.Prride.AfterRain.";
	private static final FileConfiguration CONFIG = ConfigManager.getConfig();
	
	@Attribute(Attribute.COOLDOWN)
	private static final long COOLDOWN = CONFIG.getLong(PATH + "Cooldown");
	@Attribute(Attribute.SELECT_RANGE)
	private static final double SELECT_RANGE = CONFIG.getDouble(PATH + "SelectRange");
	@Attribute(Attribute.SPEED)
	private static final double PULL_SPEED = CONFIG.getDouble(PATH + "PullSpeed");
	
	private enum State {
		PULLING, PLAYER, PEAK
	}
	
	private double y;
	private int idx = 0, pos = 0;
	private boolean done;
	
	private Location location, destination, peak;
	private Block target;
	private Listener listener;
	
	private State state;
	
	private final List<Material> RAINBOW = Arrays.asList(Material.RED_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, 
														 Material.LIME_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS, 
														 Material.BLUE_STAINED_GLASS, Material.PURPLE_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS);
	
	private Set<TempBlock> tempBlocks = new HashSet<>();
	
	public AfterRain(Player player) {
		super(player);
		
		if (!bPlayer.canBend(this)) return;
		
		target = getWaterSourceBlock(player, SELECT_RANGE, false);
		
		if (target == null) return;
		
		location = target.getLocation();
		destination = GeneralMethods.getRightSide(player.getLocation().add(0, 0.7, 0), 0.55);
		peak = player.getLocation().add(0, 3, 0);
		
		state = State.PULLING;
		
		start();
	}

	@Override
	public long getCooldown() {
		return COOLDOWN;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "AfterRain";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public void progress() {
		if (!player.isOnline() || player.isDead()) {
			remove();
			return;
		}
		
		if (state == State.PULLING) {
			pullWater();
		} else if (state == State.PLAYER) {
			raiseWater();
		} else if (state == State.PEAK) {
			makeArc();
		}
		
		Material rain;
		
		if (state != State.PEAK) {
			rain = Material.WATER;	
		} else {
			idx++; pos++;
			if (idx >= (RAINBOW.size() - 1)) idx = 0;
			rain = RAINBOW.get(idx);
		}
		
		if (done) {
			new BukkitRunnable() {
				private int newIdx = pos;
				private Location base = location.clone();
				
				public void run() {
					if (pos >= (RAINBOW.size() - 1)) {
						newIdx = pos % RAINBOW.size();
					}
					pos--;
					ParticleEffect.BLOCK_CRACK.display(base, 5, 0.35F, 0.35F, 0.35F, 0F, (pos >= RAINBOW.size() - 1) ? RAINBOW.get(newIdx).createBlockData() : RAINBOW.get(pos).createBlockData());
					player.getWorld().playSound(base, Sound.BLOCK_GLASS_BREAK, 0.7F, 1F);
					
					if (pos <= 0) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 70, 2));
						cancel();
						remove();
						return;
					}
				}
			}.runTaskTimer(ProjectKorra.plugin, 0, 1);
		}
		if (isAir(location.getBlock().getType())) {
			tempBlocks.add(new TempBlock(location.getBlock(), rain.createBlockData(), 1500));
		}
		
		if (!player.isSneaking()) {
			remove();
			return;
		}
		
		if (state != State.PULLING) {
			if (player.getLocation().distance(location) > 4) {
				remove();
				return;
			}
		}
	}
	
	private void pullWater() {
		destination = GeneralMethods.getRightSide(player.getLocation().add(0, 0.7, 0), 1.95);
		location.add(GeneralMethods.getDirection(location, destination).normalize().multiply(PULL_SPEED));
		
		if (atDestination(location, destination)) {
			state = State.PLAYER;
		}
	}
	
	private void raiseWater() {
		peak = player.getLocation().add(0, 3, 0).clone();
		Location oldPeak = destination.clone(),
				 newPeak = this.peak.clone();
		
		oldPeak.add(0, 2, 0);
		Location loc;
		
		if (atDestination(location, oldPeak.clone())) {
			loc = newPeak;
			
		} else if (atDestination(location, newPeak)) {
			y = 0;
			loc = newPeak;
			state = State.PEAK;
			
		} else {
			loc = oldPeak;
		}
		location.add(GeneralMethods.getDirection(location, loc).normalize().multiply(1));
	}
	
	private void makeArc() {
		Location left = GeneralMethods.getLeftSide(peak.clone(), 2.25).clone();
		Vector direction = GeneralMethods.getDirection(location, left);
		location = location.add(direction.normalize().multiply(1));
		
		if (atDestination(location, left)) {
			y += 0.5;
			location.subtract(0, y, 0);
			
			if (!isTransparent(location.getBlock()) && !TempBlock.isTempBlock(location.getBlock())) {
				done = true;
			} else if (y > 16) {
				done = true;
			}
		}
	}
	
	private boolean atDestination(Location location, Location destination) {
		return location.distanceSquared(destination) <= (1 * 1);
	}
	
	@Override
	public void remove() {
		if (!done) {
			tempBlocks.forEach(tb -> tb.revertBlock());
		}
		
		super.remove();
		
		bPlayer.addCooldown(this);
	}

	@Override
	public String getAuthor() {
		return "Prride & xLumos";
	}

	@Override
	public String getVersion() {
		return "Build 1.0";
	}

	@Override
	public void load() {
		ProjectKorra.log.info(ChatColor.BLUE + "AfterRain loaded in!");
		listener = new AfterRainListener();
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
		
		FileConfiguration config = ConfigManager.getConfig();
		
		config.addDefault("ExtraAbilities.Prride.AfterRain.Cooldown", 10000);
		config.addDefault("ExtraAbilities.Prride.AfterRain.SelectRange", 15);
		config.addDefault("ExtraAbilities.Prride.AfterRain.PullSpeed", 1);
		ConfigManager.defaultConfig.save();
	}

	@Override
	public void stop() {
		HandlerList.unregisterAll(listener);
	}

}
