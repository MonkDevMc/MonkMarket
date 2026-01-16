package io.monk.monkmarket.utils;

import java.text.DecimalFormat;

public class NumberUtils {
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat SIMPLE_FORMAT = new DecimalFormat("#,##0");
    
    public static String formatPrice(double price) {
        return PRICE_FORMAT.format(price);
    }
    
    public static String formatNumber(int number) {
        return SIMPLE_FORMAT.format(number);
    }
    
    public static String formatNumber(double number) {
        return SIMPLE_FORMAT.format(number);
    }
    
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public static int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
    
    public static double calculateTotalPrice(double pricePerUnit, int amount) {
        return round(pricePerUnit * amount, 2);
    }
    
    public static String formatWithCurrency(double amount, String currency) {
        return PRICE_FORMAT.format(amount) + " " + currency;
    }
}