package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("LogLocked")
public interface LogLockedConfig extends Config
{

	@ConfigItem(
			keyName = "unlocked_logs",
			name = "Unlocked Col Log Sections",
			description = "",
			hidden = true
	)
	default String unlockedLogs()
	{
		return "";
	}

	@ConfigItem(
			keyName = "additional_log_slots",
			name = "Additional Log Slots",
			description = "Add log unlock slots"
	)
	default int additionalSlots()
	{
		return 0;
	}


	@ConfigItem(
			keyName = "overlay",
			name = "Display Overlay",
			description = "Display Overlay"
	)
	default boolean displayOverlay()
	{
		return true;
	}


	@ConfigItem(
			keyName = "icon",
			name = "Icon",
			description = ""
	)
	default ColLogIcon icon()
	{
		return ColLogIcon.NORMAL_LOG_ICON;
	}

	@ConfigItem(
			keyName = "stored_info",
			name = "stored_info",
			description = "",
			hidden = true
	)
	default String storedInfo()
	{
		return "0/0";
	}


}
