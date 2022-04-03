package hjdarnel.emojipalette;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
		name = "Emoji Palette"
)
public class EmojiPalettePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	private EmojiPanel emojiPanel;
	private NavigationButton navButton;
	private static final Pattern TAG_REGEXP = Pattern.compile("<[^>]*>");

	@Override
	protected void startUp() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
	{
		createEmojiPanel();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	private void createEmojiPanel() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException
	{
		emojiPanel = injector.getInstance(EmojiPanel.class);
		emojiPanel.init(this);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Emoji Palette")
				.icon(icon)
				.priority(10)
				.panel(emojiPanel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	/**
	 * Inserts a selected emoji into the chat box input
	 */
	public void insertEmoji(String emojiText)
	{
		clientThread.invoke(() ->
		{
			final String currentMessage = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
			// Pads the emoji text with spaces so always parsed correctly
			client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, currentMessage + " " + emojiText + " ");
			client.runScript(ScriptID.CHAT_PROMPT_INIT);
		});
	}

	/**
	 * Unescape a string for widgets, replacing &lt;lt&gt; and &lt;gt&gt; with their unescaped counterparts
	 */
	public static String unescapeTags(String str)
	{
		StringBuffer out = new StringBuffer();
		Matcher matcher = TAG_REGEXP.matcher(str);

		while (matcher.find())
		{
			matcher.appendReplacement(out, "");
			String match = matcher.group(0);
			switch (match)
			{
				case "<lt>":
					out.append("<");
					break;
				case "<gt>":
					out.append(">");
					break;
				case "<br>":
					out.append("\n");
					break;
				default:
					out.append(match);
			}
		}
		matcher.appendTail(out);

		return out.toString();
	}
}