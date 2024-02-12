package com.bnjrKemal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SpecificChest {

    private final Main main;
    private final YamlConfiguration yaml;
    private final String fileName;
    private final int X, Z;
    private Hologram hologram;
    private Location loc;
    private final String startMessage, prefix, foundChest, notFoundChest;
    private final int height, terminateTime;
    private Player founder;
    private final ScheduledExecutorService scheduler;

    public SpecificChest(Main instance, File file) {
        main = instance;
        yaml = YamlConfiguration.loadConfiguration(file);
        loc = getRandomLocation();
        founder = null;
        fileName = file.getName().replace(".yml", "");
        X = getYaml().getInt("random-location.X");
        Z = getYaml().getInt("random-location.Z");
        terminateTime = getYaml().getInt("terminate-time", 1);
        startMessage = main.getConfig().getString("start_message").replace("{minute}", terminateTime + "");
        prefix = main.getConfig().getString("prefix");
        foundChest = main.getConfig().getString("found-chest");
        notFoundChest = main.getConfig().getString("not-found-chest");
        height = getYaml().getInt("hologram-highest", 0);
        scheduler = Executors.newScheduledThreadPool(1);

        Main.chests.add(this);
    }

    public void start() {
        placeChest();

        scheduler.schedule(() -> {
            if (getLocation() != null) {
                stop();
            }
        }, terminateTime, TimeUnit.MINUTES);

        System.out.println("Scheduler getName(): " + scheduler.getClass().getName());

    }

    public void stop() {
        String broadcast = (getFounder() != null) ? getFoundChestMessage() : getNotFoundChestMessage();

        if (getFounder() != null && Bukkit.getPlayer(getFounder().getName()) != null) {
            getRewards().forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", getFounder().getName())));
            setFounder(null);
        }

        getLocation().getBlock().setType(Material.AIR);
        if(hologram != null) hologram.despawnHologram();
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(broadcast));
    }

    public void setFounder(Player founder) {
        this.founder = founder;
    }

    public void createHologram() {
        hologram = new Hologram(loc, getYaml().getStringList("hologram"), height);
    }

    public void placeChest() {
        Location validLocation = findValidLocation();
        validLocation.getBlock().setType(getType());
        sendStartMessage();
        createHologram();
    }

    private Location findValidLocation() {
        loc = getRandomLocation();
        Block targetBlock = loc.getBlock();

        // Geçerli bir konum bulana kadar döngü
        while (!targetBlock.getType().isSolid()) {
            loc = getRandomLocation();
            targetBlock = loc.getBlock();

            try {
                Thread.sleep(50); // 50 milisaniye bekleme
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return loc.add(0, 1, 0); // Geçerli konumu döndür
    }

    public Location getRandomLocation() {
        int x = ThreadLocalRandom.current().nextInt(X, X * 2 + 1) - X;
        int z = ThreadLocalRandom.current().nextInt(Z, Z * 2 + 1) - Z;
        return new Location(getWorld(), x, getWorld().getHighestBlockYAt(x, z), z);
    }

    public Location getLocation() {
        return loc;
    }

    public YamlConfiguration getYaml() {
        return yaml;
    }

    public List<String> getRewards() {
        return getYaml().getStringList("rewards");
    }

    public List<String> getEvents() {
        return getYaml().getStringList("events");
    }

    public String getName() {
        return getYaml().getString("name");
    }

    public Material getType() {
        return Material.matchMaterial(getYaml().getString("type"));
    }

    public World getWorld() {
        return Bukkit.getWorld(getYaml().getString("world"));
    }

    public Player getFounder() {
        return founder;
    }

    public String getFileName() {
        return fileName;
    }

    private String diffTimeToString(Date firstDate, Date lastDate) {
        long diff = lastDate.getTime() - firstDate.getTime();
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        StringBuilder str = new StringBuilder();
        appendTimeUnit(str, diffDays, "day");
        appendTimeUnit(str, diffHours, "hour");
        appendTimeUnit(str, diffMinutes, "minute");

        if (str.length() == 0) {
            // Buraya ulaşmak, yani str boşsa, o zaman "şimdi" ekleyebilirsiniz.
            str.append(main.getConfig().getString("format.now"));
        }

        return str.toString();
    }

    private String buildTime() {
        SimpleDateFormat nowFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String now = nowFormat.format(date);

        SimpleDateFormat dayMountYearFormat = new SimpleDateFormat("dd:MM:yyyy");
        String dayMountYear = dayMountYearFormat.format(date);

        int nowHour = Integer.parseInt(now.split(":")[0]);
        int nowMinute = Integer.parseInt(now.split(":")[1]);

        for (String nextTime : getEvents()) {
            int nextHour = Integer.parseInt(nextTime.split(":")[0]);
            int nextMinute = Integer.parseInt(nextTime.split(":")[1]);

            if (nowHour < nextHour || (nowHour == nextHour && nowMinute < nextMinute)) {
                return dayMountYear + " " + nextTime;
            }
        }

        SimpleDateFormat nextFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm");
        Date nextDate = getNextEventDate(dayMountYear + " " + getEvents().get(0), nextFormat);

        return nextFormat.format(nextDate);
    }

    private Date getNextEventDate(String dateString, SimpleDateFormat format) {
        try {
            Date date = format.parse(dateString);
            if (new Date().getTime() > date.getTime()) {
                date = new Date(date.getTime() + 86400 * 1000);
            }
            return date;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String remainingTime() {
        SimpleDateFormat nextFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm");
        Date nextDate = null;
        try {
            nextDate = nextFormat.parse(buildTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return diffTimeToString(new Date(), nextDate);
    }

    private String getFoundChestMessage() {
        return foundChest
                .replace("{prefix}", prefix)
                .replace("{name}", getName())
                .replace("{player}", founder.getName());
    }

    private String getNotFoundChestMessage() {
        return notFoundChest
                .replace("{prefix}", prefix)
                .replace("{name}", getName());
    }

    private boolean sendStartMessage() {
        String broadcast = startMessage
                .replace("{prefix}", prefix)
                .replace("{name}", getName())
                .replace("{world}", getWorld().getName())
                .replace("{x}", String.valueOf((int) getLocation().getX()))
                .replace("{y}", String.valueOf((int) getLocation().getY()))
                .replace("{z}", String.valueOf((int) getLocation().getZ()));

        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(broadcast));
        return true;
    }

    public boolean equalsLocation(Location loc1, Location loc2) {
        return (int) loc1.getX() == (int) loc2.getX()
                && (int) loc1.getY() == (int) loc2.getY()
                && (int) loc1.getZ() == (int) loc2.getZ()
                && loc1.getWorld() == loc2.getWorld();
    }

    private boolean appendTimeUnit(StringBuilder str, long value, String unit) {
        if (value > 0) {
            String format = main.getConfig().getString("format." + unit, unit); // Eğer formata özel bir tanımlama yoksa, varsayılan olarak birim adını kullan
            str.append(value).append(" ").append(format);
            str.append(" ");
            return true;
        }
        return false;
    }

}
