package com.example;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "LogLocked",
	description = "Plugin that allows users to unlock Col Log sections based on total log slots (10slots = 1section)- Made for Dabe",
	tags = "LogLocked,ColLog,Dabe"
)
public class LogLocked extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private LogLockedConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LogLockedOverlay overlay;

	@Inject
	private ClientThread clientThread;

	private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("New item added to your collection log:.*");
	private final HashMap<ColLogIcon, Integer> iconIds = new HashMap<>();
	private final List<String> validEntries = new ArrayList<>();
	private ColLogIcon selectedIcon = null;
	int totalLogSlots;
	int bankedSlots = 0;
	int slotsTillNext;
	public List<String> unlockedSlots;

	@Override
	protected void startUp() throws Exception
	{
		unlockedSlots = new ArrayList<>(List.of(config.unlockedLogs().split(",")));

		updateSelectedIcon();

		bankedSlots = Integer.parseInt(config.storedInfo().split("/")[0]);
		slotsTillNext = Integer.parseInt(config.storedInfo().split("/")[1]);

		if (client.getModIcons() == null)
		{
			iconIds.clear();
			return;
		}

		loadSprites();

		overlayManager.add(overlay);
		log.info("LogLocked started");
	}


	@Override
	protected void shutDown() throws Exception
	{
		iconIds.clear();

		clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));

		configManager.setConfiguration("LogLocked","stored_info",bankedSlots + "/" + slotsTillNext);

		overlayManager.remove(overlay);
		log.info("LogLocked stopped");
	}




	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("LogLocked") && event.getKey().equals("icon"))
		{
			updateSelectedIcon();
			clientThread.invoke(this::updateChatbox);
		}
	}


	@Subscribe
	public void onChatMessage(ChatMessage event)
	{

		if (event.getType() == ChatMessageType.GAMEMESSAGE
			&& COLLECTION_LOG_ITEM_REGEX.matcher(event.getMessage()).matches())
		{
			totalLogSlots++;
			bankedSlots = ((totalLogSlots / 10) + config.additionalSlots()) - (unlockedSlots.size() - 1);
			slotsTillNext = calcSlotsTillNext(totalLogSlots);
			configManager.setConfiguration("LogLocked","stored_info",bankedSlots + "/" + slotsTillNext);

		}


		if (event.getName() == null || client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}


		boolean isLocalPlayer = Text.standardize(event.getName()).equalsIgnoreCase(Text.standardize(client.getLocalPlayer().getName()));

		if (isLocalPlayer)
		{
			event.getMessageNode().setName(
					getImgTag(iconIds.getOrDefault(selectedIcon, IconID.NO_ENTRY.getIndex())) +
							Text.removeTags(event.getName()));
		}
	}


	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!event.getEventName().equals("setChatboxInput"))
		{
			return;
		}

		updateChatbox();
	}


	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		updateChatbox();

		Widget colLogEntryHeader = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER);
		Widget colLogEntryList = client.getWidget(621,34);


		if (colLogEntryHeader != null)
		{
			int i = 0;
			while (colLogEntryList.isHidden() && i < 5)
			{
				i++;
				switch (i)
				{
					case 1:
						colLogEntryList = client.getWidget(621,12);
						break;

					case 2:
						colLogEntryList = client.getWidget(621,16);
						break;

					case 3:
						colLogEntryList = client.getWidget(621,32);
						break;

					case 4:
						colLogEntryList = client.getWidget(621,34);
						break;

					case 5:
						colLogEntryList = client.getWidget(621,35);
						break;
				}

			}
		}



		if (colLogEntryHeader != null && !colLogEntryList.isHidden())
		{
			for (Widget widget : colLogEntryList.getDynamicChildren())
			{
				validEntries.add(widget.getText());
				if (!unlockedSlots.contains(widget.getText()))
				{
					widget.setTextColor(Color.GRAY.getRGB());
				}
				else if (widget.getTextColor() != 901389 && unlockedSlots.contains(widget.getText()))
				{
					widget.setTextColor(new Color(255, 152, 31).getRGB());
				}
			}
		}

		if (colLogEntryHeader != null)
		{

			Widget colLogEntry = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY);
			String openedLogName = colLogEntryHeader.getChild(0).getText();

			colLogEntry.setHidden(!unlockedSlots.contains(openedLogName));

			colLogEntry.revalidate();

		}


	}




	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{

		Widget colLogTitleWig = client.getWidget(621,1);

		if (colLogTitleWig != null)
		{
			colLogTitleWig = colLogTitleWig.getChild(1);

			totalLogSlots = Integer.parseInt(colLogTitleWig.getText()
					.split("- ")[1]
					.split("/")[0]);

			bankedSlots = ((totalLogSlots / 10) + config.additionalSlots()) - (unlockedSlots.size() - 1);

			slotsTillNext = calcSlotsTillNext(totalLogSlots);

			configManager.setConfiguration("LogLocked","stored_info",bankedSlots + "/" + slotsTillNext);


			colLogTitleWig.setText(colLogTitleWig.getText() + " - Banked:" + bankedSlots + " - Next:" + slotsTillNext);
			colLogTitleWig.revalidate();
		}


	}


	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (event.getFirstEntry().getOption().equals("Check"))
		{
			String target = event.getFirstEntry().getTarget();
			int startIdx = target.indexOf('>') + 1;
			int endIdx = target.lastIndexOf('<');

			String colLogSectionName = target.substring(startIdx, endIdx);

			if (!unlockedSlots.contains(colLogSectionName) && validEntries.contains(colLogSectionName))
			{
				client.createMenuEntry(1)
						.setOption("Unlock")
						.setTarget(event.getFirstEntry().getTarget() + " Cost: 10")
						.setType(MenuAction.RUNELITE)
						.onClick(this::menuEntryClicked)
				;
			}



		}


	}


	private void menuEntryClicked(MenuEntry menuEntry)
	{
		if (bankedSlots >= 1)
		{
			String target = menuEntry.getTarget();
			int startIdx = target.indexOf('>') + 1;
			int endIdx = target.lastIndexOf('<');

			String colLogSectionName = target.substring(startIdx, endIdx);

			unlockedSlots.add(colLogSectionName);

			String updatedUnlocks = String.join(",", unlockedSlots);

			configManager.setConfiguration("LogLocked","unlocked_logs",updatedUnlocks);

			client.addChatMessage(ChatMessageType.GAMEMESSAGE
					,""
					,"Congratulations you have unlocked the "+ colLogSectionName +" Collection Log section."
					,""
					,false);

			updateColLogHeader();

			configManager.setConfiguration("LogLocked","stored_info",bankedSlots + "/" + slotsTillNext);

		}
		else
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE
					,""
					,"You do not have enough banked slots to unlock this Collection Log section."
					,""
					,false);
		}

	}

	public int calcSlotsTillNext(int totalLogSlots)
	{
		totalLogSlots = totalLogSlots % 10 == 0 ? 10 : 10 - (totalLogSlots % 10);
		return totalLogSlots;
	}

	public void updateColLogHeader()
	{
		Widget colLogTitleWig = client.getWidget(621,1);

		if (colLogTitleWig != null)
		{
			colLogTitleWig = colLogTitleWig.getChild(1);

			int oldBankedSlots = bankedSlots;

			bankedSlots = bankedSlots - 1;

			String header = colLogTitleWig.getText().replace("Banked:" + oldBankedSlots,"Banked:" + bankedSlots);

			colLogTitleWig.setText(header);
			colLogTitleWig.revalidate();
		}

	}



	private void loadSprites()
	{
		clientThread.invoke(() ->
		{
			IndexedSprite[] modIcons = client.getModIcons();
			List<IndexedSprite> newList = new ArrayList<>();

			int modIconsStart = modIcons.length - 1;


			for (ColLogIcon icon : ColLogIcon.values())
			{
				BufferedImage bufferedImage = getSprite(3390);
				if (icon.name().equals("FADED_LOG_ICON"))
				{
					bufferedImage = ImageUtil.luminanceOffset(bufferedImage, +70);
				}
				bufferedImage = ImageUtil.resizeImage(bufferedImage,13,13);

				final IndexedSprite sprite = ImageUtil.getImageIndexedSprite(bufferedImage, client);

				if (sprite == null)
				{
					continue;
				}

				newList.add(sprite);
				modIconsStart++;
				iconIds.put(icon, modIconsStart);
			}

			IndexedSprite[] newAry = Arrays.copyOf(modIcons, modIcons.length + newList.size());
			System.arraycopy(newList.toArray(new IndexedSprite[0]), 0, newAry, modIcons.length, newList.size());
			client.setModIcons(newAry);
		});
	}

	private BufferedImage getSprite(int id)
	{
		return getPixels(id).toBufferedImage();
	}

	private SpritePixels getPixels(int archive)
	{

		SpritePixels[] sp = client.getSprites(client.getIndexSprites(), archive, 0);
		if (sp == null)
		{
			return null;
		}

		return sp[0];
	}


	private String getImgTag(int i)
	{
		return "<img=" + i + ">";
	}

	private void updateChatbox()
	{
		Widget chatboxTypedText = client.getWidget(WidgetInfo.CHATBOX_INPUT);

		if (getIconIdx() == -1)
		{
			return;
		}

		if (chatboxTypedText == null || chatboxTypedText.isHidden())
		{
			return;
		}

		String[] chatbox = chatboxTypedText.getText().split(":", 2);
		String rsn = Objects.requireNonNull(client.getLocalPlayer()).getName();

		chatboxTypedText.setText(getImgTag(getIconIdx()) + Text.removeTags(rsn) + ":" + chatbox[1]);
	}


	private int getIconIdx()
	{
		if (selectedIcon == null)
		{
			updateSelectedIcon();
		}

		return iconIds.getOrDefault(selectedIcon, IconID.NO_ENTRY.getIndex());
	}

	private void updateSelectedIcon()
	{
		if (selectedIcon != config.icon())
		{
			selectedIcon = config.icon();
		}
	}



	@Provides
	LogLockedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LogLockedConfig.class);
	}
}
