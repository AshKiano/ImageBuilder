package com.ashkiano.imagebuilder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageBuilder extends JavaPlugin implements CommandExecutor {

    private final Map<Color, Material> colorBlockMap = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Uloží defaultní config.yml, pokud neexistuje
        initializeColorBlockMap(); // Načte mapování z config.yml
        this.getCommand("buildimage").setExecutor(this);
        this.getLogger().info("Thank you for using the ImageBuilder plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");
        Metrics metrics = new Metrics(this, 21156);
    }

    private void initializeColorBlockMap() {
        colorBlockMap.clear(); // Vymaže stávající mapování
        ConfigurationSection colorMapSection = getConfig().getConfigurationSection("colorBlockMap");
        if (colorMapSection != null) {
            for (String key : colorMapSection.getKeys(false)) {
                Color color = Color.decode("#" + key); // Převede hexadecimální řetězec na objekt Color
                String materialName = colorMapSection.getString(key);
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    colorBlockMap.put(color, material);
                } else {
                    getLogger().warning("Neplatný materiál '" + materialName + "' pro barvu '" + key + "' v config.yml");
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("buildimage") && sender instanceof Player) {
            if (args.length < 1) {
                sender.sendMessage("Usage: /buildimage <imageURL>");
                return true;
            }
            String url = args[0];
            Player player = (Player) sender;
            buildImageFromURL(player.getLocation(), url);
            return true;
        }
        return false;
    }

    private void buildImageFromURL(Location startLocation, String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            InputStream in = url.openStream();
            BufferedImage originalImage = ImageIO.read(in);

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            int newWidth = width;
            int newHeight = height;

            // Zkontrolujte, zda je obrázek větší než 128 v jakémkoliv rozměru
            if (width > 128 || height > 128) {
                // Vypočítejte nové rozměry obrázku zachováním poměru stran
                if (width > height) {
                    newWidth = 128;
                    newHeight = (newWidth * height) / width;
                } else {
                    newHeight = 128;
                    newWidth = (newHeight * width) / height;
                }
            }

            // Vytvořte nový obrázek se zmenšenými rozměry
            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT);
            BufferedImage bufferedResizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedResizedImage.createGraphics();
            g2d.drawImage(resizedImage, 0, 0, null);
            g2d.dispose();

            // Iterujte přes každý pixel v obrázku a převeďte jej na blok v Minecraftu
            for (int x = 0; x < newWidth; x++) {
                for (int z = 0; z < newHeight; z++) {
                    int rgb = bufferedResizedImage.getRGB(x, z);
                    Color color = new Color(rgb, true);
                    Material material = getClosestBlockColor(color);
                    Location blockLocation = startLocation.clone().add(x, 0, z);
                    blockLocation.getBlock().setType(material);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Material getClosestBlockColor(Color color) {
        // Zjednodušená verze, která vybere nejbližší blok podle barvy
        // Toto by se dalo vylepšit pro přesnější mapování
        Material closestMaterial = Material.WHITE_WOOL; // Výchozí blok, pokud není nalezena shoda
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<Color, Material> entry : colorBlockMap.entrySet()) {
            double distance = colorDistance(color, entry.getKey());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestMaterial = entry.getValue();
            }
        }

        return closestMaterial;
    }

    private double colorDistance(Color c1, Color c2) {
        long rmean = ( (long)c1.getRed() + (long)c2.getRed() ) / 2;
        long r = (long)c1.getRed() - (long)c2.getRed();
        long g = (long)c1.getGreen() - (long)c2.getGreen();
        long b = (long)c1.getBlue() - (long)c2.getBlue();
        return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
    }
}