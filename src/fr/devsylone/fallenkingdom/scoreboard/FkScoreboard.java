package fr.devsylone.fallenkingdom.scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.devsylone.fallenkingdom.utils.Messages;
import fr.devsylone.fkpi.FkPI;
import fr.devsylone.fkpi.rules.Rule;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import fr.devsylone.fallenkingdom.Fk;
import fr.devsylone.fallenkingdom.game.Game.GameState;
import fr.devsylone.fallenkingdom.manager.saveable.ScoreboardManager;
import fr.devsylone.fallenkingdom.players.FkPlayer.PlayerState;
import fr.devsylone.fallenkingdom.utils.RulesFormatter;

public class FkScoreboard
{
	private final Scoreboard bukkitBoard;
	private final ScoreboardSign sidebarBoard;

	private final Player player;

	private final HashMap<PlaceHolder, List<Integer>> placeHolders;

	private boolean formatted;

	public FkScoreboard(Player player)
	{
		formatted = true;
		this.player = player;
		this.placeHolders = new HashMap<>();
		this.bukkitBoard = FkPI.getInstance().getTeamManager().getScoreboard();

		sidebarBoard = new ScoreboardSign(player, Fk.getInstance().getScoreboardManager().getName());

		if(FkPI.getInstance().getRulesManager().getRule(Rule.HEALTH_BELOW_NAME) && bukkitBoard.getObjective("§c❤") == null)
			bukkitBoard.registerNewObjective("§c❤", "health").setDisplaySlot(DisplaySlot.BELOW_NAME);

		List<String> sidebarConfig = Fk.getInstance().getScoreboardManager().getSidebar();

		for(PlaceHolder placeHolder : PlaceHolder.values())
		{
			placeHolders.put(placeHolder, new ArrayList<>());
			for(int i = 0; i < sidebarConfig.size(); i++)
				if(placeHolder.isInLine(sidebarConfig.get(i)))
					placeHolders.get(placeHolder).add(i);
		}

		sidebarBoard.create();
		player.setScoreboard(bukkitBoard);
		refreshAll();
	}

	public void refreshAll()
	{
		for(int i = 0; i < ScoreboardSign.LINE_NUMBER; i++)
			sidebarBoard.removeLine(i);

		int index = 0;

		if(Fk.getInstance().getGame().getState().equals(GameState.BEFORE_STARTING) && !Fk.getInstance().getPlayerManager().getPlayer(player).getState().equals(PlayerState.EDITING_SCOREBOARD))
		{
			sidebarBoard.setLine(Messages.SCOREBOARD_TEAMS.getMessage(), ++index);

			if(Fk.getInstance().getFkPI().getTeamManager().getTeams().size() <= 10)
				for(fr.devsylone.fkpi.teams.Team team : Fk.getInstance().getFkPI().getTeamManager().getTeams())
					sidebarBoard.setLine(" " + team.toString() + " (§7" + team.getPlayers().size() + team.getChatColor() + ")", ++index);
			else
				for(int i = 0; i < 10; i++)
				{
					fr.devsylone.fkpi.teams.Team team = Fk.getInstance().getFkPI().getTeamManager().getTeams().get(i);
					sidebarBoard.setLine(" " + team.toString() + " (§7" + team.getPlayers().size() + team.getChatColor() + ")", ++index);
				}

			sidebarBoard.setLine("§1", ++index);
			sidebarBoard.setLine(Messages.SCOREBOARD_RULES.getMessage(), ++index);

			for(String s : RulesFormatter.formatRules(Rule.ALLOWED_BLOCKS, Rule.CHARGED_CREEPERS, Rule.DISABLED_POTIONS))
				if(index < 14)
					sidebarBoard.setLine(" " + s, ++index);
				else
					sidebarBoard.setLine(" §6... (§e/fk rules list§6)", index);
		}
		else
		{
			for(int i = 0; i < Fk.getInstance().getScoreboardManager().getSidebar().size(); i++)
				refreshLine(i);
		}
		Fk.getInstance().getScoreboardManager().refreshNicks();

		try
		{
			player.setScoreboard(bukkitBoard);
		}catch(IllegalStateException whynot)
		{
			// dropgg
		}
	}

	public void refresh(PlaceHolder... placeHolders)
	{
		if(placeHolders.length == 0)
		{
			refreshAll();
			return;
		}
		if(Fk.getInstance().getGame().getState() == GameState.BEFORE_STARTING && !Fk.getInstance().getPlayerManager().getPlayer(player).getState().equals(PlayerState.EDITING_SCOREBOARD))
			return;

		List<Integer> linesToRefresh = new ArrayList<>();

		for(PlaceHolder p : placeHolders)
		{
			for(Integer i : this.placeHolders.get(p))
				if(!linesToRefresh.contains(i))
					linesToRefresh.add(i);
		}

		for(Integer i : linesToRefresh)
			refreshLine(i);
	}

	public void setFormatted(boolean bool)
	{
		formatted = bool;

		for(int i = 0; i < Fk.getInstance().getScoreboardManager().getSidebar().size(); i++)
			refreshLine(i);
	}

	private void refreshLine(int i)
	{
		if(Fk.getInstance().getGame().getState() == GameState.BEFORE_STARTING && !Fk.getInstance().getPlayerManager().getPlayer(player).getState().equals(PlayerState.EDITING_SCOREBOARD))
			return;

		String line = Fk.getInstance().getScoreboardManager().getSidebar().get(i);
		if(formatted)
		{
			for(PlaceHolder placeHolder : placeHolders.keySet())
				if(placeHolders.get(placeHolder).contains(i))
					line = placeHolder.replace(line, player, placeHolders.get(placeHolder).indexOf(i));
		}
		else
		{
			line = line.replaceAll("§", "&");
			line = line.replaceAll("(&.)+$", ScoreboardManager.randomFakeEmpty());
		}

		if(!sidebarBoard.getLine(i).equals(line))
			sidebarBoard.setLine(line, i);
	}

	public void remove()
	{
		sidebarBoard.destroy();
	}
}
