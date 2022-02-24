package me.castiel.kothbot.listeners;

import com.benzimmer123.koth.api.events.KothStartEvent;
import com.benzimmer123.koth.api.objects.KOTHArena;
import me.castiel.kothbot.KoTHBot;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KoTHStartEventListener implements Listener {

    @EventHandler
    public void onKoTHStartEvent(KothStartEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        KoTHBot plugin = KoTHBot.getInstance();
        KOTHArena kothArena = event.getKOTH();
        plugin.cooldown.remove(kothArena.getName(true));
        Configuration cfg = plugin.getConfig();
        eb.setTitle(cfg.getString("koth-started-embed.title", "").replace("%koth%", kothArena.getName(true)));
        Location loc = kothArena.getKOTHLocation().getLocation1();
        for (String key : cfg.getConfigurationSection("koth-started-embed.fields").getKeys(false)) {
            eb.addField(cfg.getString("koth-started-embed.fields." + key + ".name", ""), cfg.getString("koth-started-embed.fields." + key + ".value", "")
                            .replace("%coords%", "X: %X%, Y: %Y%, Z: %Z%")
                            .replace("%X%", String.valueOf(loc.getBlockX()))
                            .replace("%Y%", String.valueOf(loc.getBlockY()))
                            .replace("%Z%", String.valueOf(loc.getBlockZ())),
                    cfg.getBoolean("koth-started-embed.fields." + key + ".inline", false));
        }
        eb.setThumbnail(cfg.getString("koth-started-embed.thumbnail-image"));
        eb.setColor(new Color(cfg.getInt("koth-started-embed.color")));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        eb.setFooter(cfg.getString("koth-started-embed.footer").replace("%date%", formatter.format(date)), cfg.getString("koth-started-embed.footer-image"));
        plugin.jda.getTextChannelById(cfg.getString("koth-started-embed.channel-id")).sendMessageEmbeds(eb.build()).queue();
    }
}
