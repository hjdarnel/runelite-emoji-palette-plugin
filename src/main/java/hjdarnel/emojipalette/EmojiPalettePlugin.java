package hjdarnel.emojipalette;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(name = "Emoji Palette")
public class EmojiPalettePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		createEmojiPanel();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	private void createEmojiPanel()
	{
		EmojiPanel emojiPanel = injector.getInstance(EmojiPanel.class);
		emojiPanel.init();
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

		navButton = NavigationButton.builder().tooltip("Emoji Palette").icon(icon).priority(10).panel(emojiPanel).build();

		clientToolbar.addNavigation(navButton);
	}
}
