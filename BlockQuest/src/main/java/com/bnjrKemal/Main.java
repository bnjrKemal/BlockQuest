package com.bnjrKemal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin implements Listener {

    static HashSet<SpecificChest> chests;
    private static ScheduledExecutorService scheduler = null;
    private static ScheduledFuture<?> scheduledFuture = null;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new PAPI(this).register();
        saveDefaultConfig();
        registerEventsAndCommands();
        loadChests();
        scheduleTask();
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null)
            return; // Tıklanan blok hedef lokasyon değilse buradan çık

        // SpecificChest nesnesini bul
        SpecificChest specificChest = getChestAtLocation(clickedBlock.getLocation());

        if (specificChest != null) {
            // Tıklamayı işle
            event.setCancelled(true);
            player.sendMessage("Hedef lokasyondaki bir bloğa tıklandı!");
            specificChest.setFounder(player);
            specificChest.stop();
        }
    }

    private static void scheduleTask() {

        scheduler = Executors.newScheduledThreadPool(1);

        LocalTime currentTime = LocalTime.now();

        long initialDelay = 60 - currentTime.getSecond();
        System.out.println("InitialDelay : " + initialDelay);
        System.out.println("scheduler.toString() : " + scheduler.toString());
        scheduledFuture = scheduler.scheduleAtFixedRate(Main::checkEvent, initialDelay, 60, TimeUnit.SECONDS);
        System.out.println("scheduler.toString() 2 : " + scheduler.toString());
    }

    private static void checkEvent(){
        System.out.println("checkEvent(); scheduler.toString() 3 : "  + scheduler.toString());
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        for (SpecificChest specificChest : chests) {
            for (String eventTime : specificChest.getEvents()) {
                String[] parts = eventTime.split(":");
                int eventHour = Integer.parseInt(parts[0]);
                int eventMinute = Integer.parseInt(parts[1]);
                if (currentHour == eventHour && currentMinute == eventMinute) {
                    specificChest.start();
                    break;
                }
            }
        }
        scheduledFuture.cancel(true);
        scheduleTask();
    }

    private void shutdown(){

        scheduler.shutdown();

        for (SpecificChest specificChest : chests)
            specificChest.stop();

    }

    public void reloading() {

        shutdown();

        // Konfigürasyonu yeniden yükle
        reloadConfig();

        // Sandıkları tekrar yükle
        loadChests();

        // Yeni bir task başlat
        scheduleTask();
    }

    private SpecificChest getChestAtLocation(Location location) {
        // Belirli bir lokasyondaki SpecificChest nesnesini döndür
        for (SpecificChest specificChest : chests) {
            if (specificChest.equalsLocation(location, specificChest.getLocation())) {
                return specificChest;
            }
        }
        return null;
    }

    private void registerEventsAndCommands() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("blockquest").setExecutor(new DECommand(this));
    }

    private void loadChests() {

        File chestFiles = new File(getDataFolder(), "/chests/");
        if (!chestFiles.exists()) {
            chestFiles.mkdirs();
            saveResource("chests/mychest.yml", true);
        }

        File[] chestFileList = chestFiles.listFiles();

        if (chestFileList != null) {
            chests = new HashSet<>();

            for (File file : chestFileList) {
                // Dosya uzantısının ".yml" olduğundan emin olalım
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    new SpecificChest(this, file);
                }
            }
        }
    }
}
