package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;

import java.util.HashMap;
import java.util.Map;

public class WidgetManager {
    private static WidgetManager instance;
    
    private final Map<String, Widget> widgets = new HashMap<>();
    private boolean editorMode = false;
    private Widget selectedWidget = null;
    
    private WidgetManager() {}
    
    public static WidgetManager getInstance() {
        if (instance == null) {
            instance = new WidgetManager();
        }
        return instance;
    }
    
    public void initialize() {
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        
        // Create default widgets
        registerWidget(new MusicWidget());
        registerWidget(new LiveWidget());
        registerWidget(new QueueWidget());
        registerWidget(new ChatWidget());
        registerWidget(new NotificationWidget());
        
        // Apply saved configuration
        for (Widget widget : widgets.values()) {
            widget.applyConfig(config);
            
            // Restore visibility
            Boolean visibility = config.widgetVisibility.get(widget.getId());
            if (visibility != null) {
                widget.setVisible(visibility);
            }
            
            // Restore position
            PulseConfig.WidgetPosition pos = config.widgetPositions.get(widget.getId());
            if (pos != null) {
                widget.setX(pos.x);
                widget.setY(pos.y);
            }
        }
        
        Pulse.LOGGER.info("WidgetManager initialized with {} widgets", widgets.size());
    }
    
    public void registerWidget(Widget widget) {
        if (widget != null) {
            widgets.put(widget.getId(), widget);
            Pulse.LOGGER.debug("Registered widget: {}", widget.getName());
        }
    }
    
    public void unregisterWidget(String id) {
        widgets.remove(id);
    }
    
    public Widget getWidget(String id) {
        return widgets.get(id);
    }
    
    public Map<String, Widget> getAllWidgets() {
        return new HashMap<>(widgets);
    }
    
    public void renderWidgets(net.minecraft.client.gui.DrawContext context, 
                              net.minecraft.client.render.RenderTickCounter tickCounter) {
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        float scale = config.widgetScale;
        
        for (Widget widget : widgets.values()) {
            if (widget.isVisible() && widget.isEnabled()) {
                try {
                    if (editorMode) {
                        widget.renderEditor(context, tickCounter, scale);
                    } else {
                        widget.render(context, tickCounter, scale);
                    }
                } catch (Exception e) {
                    Pulse.LOGGER.error("Error rendering widget {}: {}", widget.getId(), e.getMessage());
                }
            }
        }
    }
    
    public void toggleWidget(String id) {
        Widget widget = widgets.get(id);
        if (widget != null) {
            widget.setVisible(!widget.isVisible());
            
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.getConfig().widgetVisibility.put(id, widget.isVisible());
            configManager.saveAsync();
        }
    }
    
    public void setWidgetVisible(String id, boolean visible) {
        Widget widget = widgets.get(id);
        if (widget != null) {
            widget.setVisible(visible);
            
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.getConfig().widgetVisibility.put(id, visible);
            configManager.saveAsync();
        }
    }
    
    public void setEditorMode(boolean editorMode) {
        this.editorMode = editorMode;
        if (!editorMode) {
            selectedWidget = null;
            // Save all positions
            saveAllPositions();
        }
    }
    
    public boolean isEditorMode() {
        return editorMode;
    }
    
    public void selectWidget(Widget widget) {
        this.selectedWidget = widget;
    }
    
    public Widget getSelectedWidget() {
        return selectedWidget;
    }
    
    public void handleMouseClick(double mouseX, double mouseY, int button) {
        if (editorMode) {
            // Check widgets in reverse order (top to bottom)
            for (Widget widget : widgets.values()) {
                if (widget.isVisible() && widget.isMouseOver(mouseX, mouseY)) {
                    if (button == 0) { // Left click - select for drag
                        selectWidget(widget);
                        widget.startDrag(mouseX, mouseY);
                    } else if (button == 1) { // Right click - toggle visibility
                        widget.setVisible(!widget.isVisible());
                    }
                    break;
                }
            }
        }
    }
    
    public void handleMouseDrag(double mouseX, double mouseY) {
        if (editorMode && selectedWidget != null) {
            selectedWidget.onDrag(mouseX, mouseY);
        }
    }
    
    public void handleMouseRelease(double mouseX, double mouseY) {
        if (selectedWidget != null) {
            selectedWidget.stopDrag();
            selectedWidget.savePosition();
            selectedWidget = null;
        }
    }
    
    public void saveAllPositions() {
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        
        for (Widget widget : widgets.values()) {
            config.widgetPositions.put(widget.getId(), 
                new PulseConfig.WidgetPosition(widget.getX(), widget.getY()));
        }
        
        configManager.saveAsync();
    }
    
    public void resetLayout() {
        // Reset to default positions
        int y = 10;
        for (Widget widget : widgets.values()) {
            widget.setX(10);
            widget.setY(y);
            y += widget.getHeight() + 10;
        }
        saveAllPositions();
    }
    
    public void applyConfig(PulseConfig config) {
        for (Widget widget : widgets.values()) {
            widget.applyConfig(config);
        }
    }
    
    public void shutdown() {
        saveAllPositions();
        widgets.clear();
    }
}