package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SortingManager {
    
    private final MonkMarket plugin;
    
    public SortingManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public void sortItemsAlphabetically(List<MarketItem> items) {
        if (!plugin.getConfigManager().isAlphabeticalSorting()) {
            return;
        }
        
        items.sort(new Comparator<MarketItem>() {
            @Override
            public int compare(MarketItem item1, MarketItem item2) {
                String name1 = getItemDisplayNameForSorting(item1);
                String name2 = getItemDisplayNameForSorting(item2);
                
                return name1.compareToIgnoreCase(name2);
            }
        });
    }
    
    private String getItemDisplayNameForSorting(MarketItem item) {
        String displayName = plugin.getConfigManager().getItemDisplayName(item.getItemMaterial());
        
        if (displayName.isEmpty()) {
            displayName = formatMaterialName(item.getItemMaterial());
        }
        
        return cyrillicToLatin(displayName).toLowerCase(Locale.ROOT);
    }
    
    private String formatMaterialName(String materialName) {
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
    
    private String cyrillicToLatin(String text) {
        if (text == null) return "";
        
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case 'А': case 'а': result.append('A'); break;
                case 'Б': case 'б': result.append('B'); break;
                case 'В': case 'в': result.append('V'); break;
                case 'Г': case 'г': result.append('G'); break;
                case 'Д': case 'д': result.append('D'); break;
                case 'Е': case 'е': case 'Ё': case 'ё': result.append('E'); break;
                case 'Ж': case 'ж': result.append('Z'); break;
                case 'З': case 'з': result.append('Z'); break;
                case 'И': case 'и': result.append('I'); break;
                case 'Й': case 'й': result.append('J'); break;
                case 'К': case 'к': result.append('K'); break;
                case 'Л': case 'л': result.append('L'); break;
                case 'М': case 'м': result.append('M'); break;
                case 'Н': case 'н': result.append('N'); break;
                case 'О': case 'о': result.append('O'); break;
                case 'П': case 'п': result.append('P'); break;
                case 'Р': case 'р': result.append('R'); break;
                case 'С': case 'с': result.append('S'); break;
                case 'Т': case 'т': result.append('T'); break;
                case 'У': case 'у': result.append('U'); break;
                case 'Ф': case 'ф': result.append('F'); break;
                case 'Х': case 'х': result.append('H'); break;
                case 'Ц': case 'ц': result.append('C'); break;
                case 'Ч': case 'ч': result.append('C'); break;
                case 'Ш': case 'ш': result.append('S'); break;
                case 'Щ': case 'щ': result.append('S'); break;
                case 'Ъ': case 'ъ': result.append('`'); break;
                case 'Ы': case 'ы': result.append('Y'); break;
                case 'Ь': case 'ь': result.append('`'); break;
                case 'Э': case 'э': result.append('E'); break;
                case 'Ю': case 'ю': result.append('U'); break;
                case 'Я': case 'я': result.append('A'); break;
                default: result.append(c);
            }
        }
        
        return result.toString();
    }
    
    public Comparator<MarketItem> getPriceComparator(boolean ascending) {
        return new Comparator<MarketItem>() {
            @Override
            public int compare(MarketItem item1, MarketItem item2) {
                double diff = item1.getPricePerUnit() - item2.getPricePerUnit();
                if (ascending) {
                    return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
                } else {
                    return diff > 0 ? -1 : (diff < 0 ? 1 : 0);
                }
            }
        };
    }
    
    public Comparator<MarketItem> getDateComparator(boolean newestFirst) {
        return new Comparator<MarketItem>() {
            @Override
            public int compare(MarketItem item1, MarketItem item2) {
                if (item1.getCreatedAt() == null || item2.getCreatedAt() == null) {
                    return 0;
                }
                
                int result = item1.getCreatedAt().compareTo(item2.getCreatedAt());
                return newestFirst ? -result : result;
            }
        };
    }
}