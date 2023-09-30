package com.example;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

public class LogLockedOverlay extends OverlayPanel {

    private final LogLocked plugin;
    private final LogLockedConfig config;


    @Inject
    LogLockedOverlay(LogLocked plugin, LogLockedConfig config)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        setPriority(OverlayPriority.HIGH);
        setResizable(true);

    }


    @Override
    public Dimension render(Graphics2D graphics)
    {

        if (!config.displayOverlay())
        {
            return null;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Available unlocks:")
                .right(String.valueOf(plugin.availUnlocks))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Slots till next:")
                .right(String.valueOf(plugin.slotsTillNext))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Unlocked sections:")
                .right(String.valueOf(plugin.unlockedSlots.size()))
                .build());


        panelComponent.setPreferredSize(new Dimension(150,100));
        panelComponent.setBorder(new Rectangle(5, 5, 5, 5));

        return super.render(graphics);

    }


}
