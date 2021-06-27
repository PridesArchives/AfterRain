package me.Pride.korra.AfterRain;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;

public class AfterRainListener implements Listener {
	
	@EventHandler
	public void onSneak(final PlayerToggleSneakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!event.isSneaking()) {
			return;
		}
		
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (bPlayer == null) {
			return;
		}
		CoreAbility coreAbil = bPlayer.getBoundAbility();

		if (coreAbil == null) {
			return;	
		}

		if (bPlayer.canBendIgnoreCooldowns(coreAbil)) {
			if (bPlayer.canBend(CoreAbility.getAbility("AfterRain")) && CoreAbility.getAbility(player, AfterRain.class) == null) {
				new AfterRain(player);
				
			}
		}
	}

}
