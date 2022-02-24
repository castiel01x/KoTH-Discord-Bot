package me.castiel.kothbot;

import com.benzimmer123.koth.KOTH;
import com.benzimmer123.koth.api.objects.KOTHArena;
import com.benzimmer123.koth.api.objects.Schedule;
import me.castiel.kothbot.listeners.KoTHStartEventListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KoTHBot extends JavaPlugin {

    private static KoTHBot koTHBot;
    public final List<String> cooldown = new ArrayList<>();
    public JDA jda;

    @Override
    public void onEnable() {
        koTHBot = this;
        saveDefaultConfig();
        Configuration cfg = getConfig();
        startBot(cfg.getString("bot-token"), cfg.getString("activity-type"), cfg.getString("activity-msg"), cfg.getString("activity-streaming-url"), cfg.getInt("activity-update-delay"));
        if (getConfig().getBoolean("koth-starting-embed.enabled", false))
            startTracker();
        if (getConfig().getBoolean("koth-started-embed.enabled", false))
            getServer().getPluginManager().registerEvents(new KoTHStartEventListener(), this);
    }

    @Override
    public void onDisable() {
        jda.cancelRequests();
        jda.shutdown();
    }

    private void startBot(String token, String type, String msg, String url, int delay){
        JDABuilder jdaBuilder = JDABuilder.createDefault(token);
        try {
            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                String message = msg.replace("{PLAYER-COUNT}", String.valueOf(Bukkit.getOnlinePlayers().size()));
                if (type.equalsIgnoreCase("watching"))
                    jda.getPresence().setActivity(Activity.watching(message));
                else if (type.equalsIgnoreCase("streaming"))
                    jda.getPresence().setActivity(Activity.streaming(message, url));
                else if (type.equalsIgnoreCase("listening"))
                    jda.getPresence().setActivity(Activity.listening(message));
                else
                    jda.getPresence().setActivity(Activity.playing(message));
            }
        }.runTaskTimer(this, 5, delay * 20L);
    }

    private void startTracker(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String koth : getConfig().getConfigurationSection("koths").getKeys(false)) {
                    KOTHArena kothArena = KOTH.getInstance().getKOTHManager().getKOTHFromString(getConfig().getString("koths." + koth + ".name"));
                    if (kothArena.isActive() || cooldown.contains(kothArena.getName(true))) continue;
                    for (Schedule schedule : kothArena.getKOTHScheduler().getScheduled()) {
                        Instant now = Instant.now();
                        ZonedDateTime local = now.atZone(schedule.getDate().getZone());
                        if (getDateDiff(local, schedule.getDate())) {
                            cooldown.add(kothArena.getName(true));
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle(getConfig().getString("koth-starting-embed.title").replace("%koth%", kothArena.getName(true)));
                            Location loc = kothArena.getKOTHLocation().getLocation1();
                            for (String key : getConfig().getConfigurationSection("koth-starting-embed.fields").getKeys(false)) {
                                eb.addField(getConfig().getString("koth-starting-embed.fields." + key + ".name", ""), getConfig().getString("koth-starting-embed.fields." + key + ".value", "")
                                        .replace("%coords%", "X: %X%, Y: %Y%, Z: %Z%")
                                        .replace("%X%", String.valueOf(loc.getBlockX()))
                                        .replace("%Y%", String.valueOf(loc.getBlockY()))
                                        .replace("%Z%", String.valueOf(loc.getBlockZ())),
                                        getConfig().getBoolean("koth-starting-embed.fields." + key + ".inline", false));
                            }
                            eb.setThumbnail(getConfig().getString("koths." + koth + ".thumbnail-image"));
                            eb.setColor(new Color(getConfig().getInt("koth-starting-embed.color")));
                            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                            Date date = new Date(System.currentTimeMillis());
                            eb.setFooter(getConfig().getString("koth-starting-embed.footer").replace("%date%", formatter.format(date)), getConfig().getString("koth-starting-embed.footer-image"));
                            jda.getTextChannelById(getConfig().getString("koth-starting-embed.channel-id")).sendMessageEmbeds(eb.build()).queue();
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20 * 60, 20 * 60);
    }

    private boolean getDateDiff(ZonedDateTime date1, ZonedDateTime date2) {
        return (date1.toEpochSecond() / 60) >= (date2.toEpochSecond() / 60) - getConfig().getLong("koth-starting-embed.send-before-start");
    }

    public static KoTHBot getInstance() {
        return koTHBot;
    }
}