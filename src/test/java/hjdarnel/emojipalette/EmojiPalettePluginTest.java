package hjdarnel.emojipalette;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EmojiPalettePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EmojiPalettePlugin.class);
		RuneLite.main(args);
	}
}