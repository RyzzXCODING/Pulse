package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GlassTab {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
    
    private final int x, y;
    private final Orientation orientation;
    private final List<TabItem> tabs = new ArrayList<>();
    private int selectedIndex = 0;
    private final Consumer<Integer> onTabChange;
    private final PulseTheme theme;
    
    private boolean hoveredTab = -1;
    
    public static class TabItem {
        public final String id;
        public final String label;
        public final Runnable iconDrawer;
        public final boolean disabled;
        public final String tooltip;
        
        public TabItem(String id, String label, Runnable iconDrawer, boolean disabled, String tooltip) {
            this.id = id;
            this.label = label;
            this.iconDrawer = iconDrawer;
            this.disabled = disabled;
            this.tooltip = tooltip;
        }
    }
    
    public static class Builder {
        private int x, y;
        private Orientation orientation = Orientation.HORIZONTAL;
        private Consumer<Integer> onTabChange = i -> {};
        private PulseTheme theme;
        
        public Builder(int x, int y, PulseTheme theme) { 
            this.x = x; 
            this.y = y; 
            this.theme = theme; 
        }
        public Builder orientation(Orientation o) { this.orientation = o; return this; }
        public Builder onTabChange(Consumer<Integer> cb) { this.onTabChange = cb; return this; }
        
        public GlassTab build() {
            return new GlassTab(this);
        }
    }
    
    private GlassTab(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.orientation = builder.orientation;
        this.onTabChange = builder.onTabChange;
        this.theme = builder.theme;
    }
    
    public void addTab(String id, String label, Runnable iconDrawer) {
        addTab(id, label, iconDrawer, false, null);
    }
    
    public void addTab(String id, String label, Runnable iconDrawer, boolean disabled) {
        addTab(id, label, iconDrawer, disabled, null);
    }
    
    public void addTab(String id, String label, Runnable iconDrawer, String tooltip) {
        addTab(id, label, iconDrawer, false, tooltip);
    }
    
    public void addTab(String id, String label, Runnable iconDrawer, boolean disabled, String tooltip) {
        tabs.add(new TabItem(id, label, iconDrawer, disabled, tooltip));
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        hoveredTab = -1;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int tabHeight = theme.tabHeight();
        int tabWidth = orientation == Orientation.HORIZONTAL ? 140 : theme.sidebarWidth() - theme.spacingM() * 2;
        int iconSize = 20;
        int spacing = orientation == Orientation.HORIZONTAL ? 4 : 2;
        
        for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);
            if (tab.disabled) continue;
            
            int tabX, tabY;
            if (orientation == Orientation.HORIZONTAL) {
                tabX = x + i * (tabWidth + spacing);
                tabY = y;
            } else {
                tabX = x + theme.spacingM();
                tabY = y + theme.headerHeight() + theme.spacingM() + i * (tabHeight + spacing);
            }
            
            boolean isSelected = i == selectedIndex;
            boolean isHovered = isMouseOverTab(mouseX, mouseY, tabX, tabY, tabWidth, tabHeight);
            
            if (isHovered) hoveredTab = i;
            
            int radius = theme.radiusMedium();
            
            // Background
            int bgColor, borderColor, textColor;
            
            if (isSelected) {
                bgColor = theme.accent(0.9f);
                borderColor = theme.accent(1f);
                textColor = theme.textOnAccent();
            } else if (isHovered) {
                bgColor = theme.bgHover(theme.panelOpacity() + 0.1f);
                borderColor = theme.borderAccentSoft();
                textColor = theme.textPrimary();
            } else {
                bgColor = theme.bgDark(theme.panelOpacity());
                borderColor = theme.borderSubtle();
                textColor = theme.textMuted();
            }
            
            GlassRenderer.fillRoundedRect(context, tabX, tabY, tabWidth, tabHeight, radius, bgColor);
            
            if (isSelected) {
                // Selected indicator line at bottom (horizontal) or right (vertical)
                if (orientation == Orientation.HORIZONTAL) {
                    int lineY = tabY + tabHeight - 3;
                    GlassRenderer.fillRoundedRect(context, tabX + 8, lineY, tabWidth - 16, 3, 1, theme.textOnAccent() | 0xFF000000);
                } else {
                    int lineX = tabX + tabWidth - 3;
                    GlassRenderer.fillRoundedRect(context, lineX, tabY + 8, 3, tabHeight - 16, 1, theme.textOnAccent() | 0xFF000000);
                }
            } else if (isHovered) {
                GlassRenderer.drawRoundedRectOutline(context, tabX, tabY, tabWidth, tabHeight, radius, 1, borderColor);
            }
            
            // Inner highlight
            if (theme.glowIntensity() > 0.1f && (isSelected || isHovered)) {
                int highlight = theme.innerGlow();
                GlassRenderer.fillRoundedRect(context, tabX + 1, tabY + 1, tabWidth - 2, 2, radius - 1, highlight);
                GlassRenderer.fillRoundedRect(context, tabX + 1, tabY + 1, 2, tabHeight - 2, radius - 1, highlight);
            }
            
            // Icon
            int iconX, iconY;
            if (orientation == Orientation.HORIZONTAL) {
                iconX = tabX + theme.spacingM();
                iconY = tabY + (tabHeight - iconSize) / 2;
            } else {
                iconX = tabX + theme.spacingM();
                iconY = tabY + (tabHeight - iconSize) / 2;
            }
            
            if (tab.iconDrawer != null) {
                context.getMatrices().push();
                context.getMatrices().translate(iconX, iconY, 0);
                tab.iconDrawer.run();
                context.getMatrices().pop();
            }
            
            // Label
            int labelX = iconX + (tab.iconDrawer != null ? iconSize + theme.spacingM() : 0);
            int labelY = tabY + (tabHeight - 8) / 2;
            int maxLabelWidth = tabWidth - (labelX - tabX) - theme.spacingM();
            
            String displayLabel = tab.label;
            if (theme.client.textRenderer.getWidth(displayLabel) > maxLabelWidth) {
                displayLabel = theme.client.textRenderer.trimToWidth(displayLabel, maxLabelWidth) + "...";
            }
            
            context.drawTextWithShadow(theme.client.textRenderer, displayLabel, labelX, labelY, textColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    private boolean isMouseOverTab(int mouseX, int mouseY, int tabX, int tabY, int tabWidth, int tabHeight) {
        return mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        int tabHeight = theme.tabHeight();
        int tabWidth = orientation == Orientation.HORIZONTAL ? 140 : theme.sidebarWidth() - theme.spacingM() * 2;
        int spacing = orientation == Orientation.HORIZONTAL ? 4 : 2;
        
        for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);
            if (tab.disabled) continue;
            
            int tabX, tabY;
            if (orientation == Orientation.HORIZONTAL) {
                tabX = x + i * (tabWidth + spacing);
                tabY = y;
            } else {
                tabX = x + theme.spacingM();
                tabY = y + theme.headerHeight() + theme.spacingM() + i * (tabHeight + spacing);
            }
            
            if (isMouseOverTab(mouseX, mouseY, tabX, tabY, tabWidth, tabHeight)) {
                if (i != selectedIndex) {
                    selectedIndex = i;
                    onTabChange.accept(i);
                }
                return true;
            }
        }
        return false;
    }
    
    public int getSelectedIndex() { return selectedIndex; }
    public String getSelectedId() { 
        return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex).id : null; 
    }
    public void setSelectedIndex(int index) { 
        if (index >= 0 && index < tabs.size() && !tabs.get(index).disabled) {
            selectedIndex = index;
            onTabChange.accept(index);
        }
    }
    public void setSelectedId(String id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id.equals(id)) {
                setSelectedIndex(i);
                break;
            }
        }
    }
    public List<TabItem> getTabs() { return new ArrayList<>(tabs); }
    public int getTabCount() { return tabs.size(); }
    
    public static Builder builder(int x, int y, PulseTheme theme) {
        return new Builder(x, y, theme);
    }
}