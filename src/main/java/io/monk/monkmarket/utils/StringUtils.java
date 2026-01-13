package io.monk.monkmarket.utils;

public class StringUtils {
    
    public static String colorize(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
    
    public static String formatPrice(double price) {
        if (price < 1000) {
            return String.format("%.2f", price);
        } else if (price < 1000000) {
            return String.format("%.1fk", price / 1000);
        } else {
            return String.format("%.1fM", price / 1000000);
        }
    }
    
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }
    
    public static String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return "";
        
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    public static boolean containsRussian(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }
    
    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
}