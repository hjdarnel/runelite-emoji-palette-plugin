package hjdarnel.emojipalette;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
class EmojiPanel extends PluginPanel
{
	private EmojiPalettePlugin plugin;


	void init(EmojiPalettePlugin plugin ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException
	{
		this.plugin = plugin;

		setBorder(new EmptyBorder(10, 10, 10, 10));

		final PluginErrorPanel errorPanel = new PluginErrorPanel();
		errorPanel.setBorder(new EmptyBorder(10, 25, 10, 25));
		errorPanel.setContent("Emoji Palette", "Click to insert an emoji or hover over to view the text trigger");
		add(errorPanel, BorderLayout.NORTH);

		JPanel emojiPanel = new JPanel();
		emojiPanel.setLayout(new GridLayout(10, 3));
		emojiPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		emojiPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

		// get Emoji.values(), Emoji.trigger, and Emoji.loadImage() accessible using reflection
		Class<Enum<?>> emojisClass = (Class<Enum<?>>) getClass().getClassLoader().loadClass("net.runelite.client.plugins.emojis.Emoji");
		Method valuesMethod = emojisClass.getDeclaredMethod("values");
		valuesMethod.setAccessible(true);
		Field triggerField = emojisClass.getDeclaredField("trigger");
		triggerField.setAccessible(true);
		Method loadImageMethod = emojisClass.getDeclaredMethod("loadImage");
		loadImageMethod.setAccessible(true);

		for (final Enum<?> emoji : (Enum<?>[]) valuesMethod.invoke(null))
		{
			JPanel panel = makeEmojiPanel(emoji, triggerField, loadImageMethod);
			emojiPanel.add(panel);
		}

		add(emojiPanel);
	}

	// Builds a JPanel displaying an icon with tooltip
	private JPanel makeEmojiPanel(Enum<?> emoji, Field triggerField, Method loadImageMethod) throws IllegalAccessException, InvocationTargetException
	{
		JLabel label = new JLabel();

		ImageIcon icon = new ImageIcon((Image) loadImageMethod.invoke(emoji));
		ImageIcon hoveredIcon = new ImageIcon(ImageUtil.alphaOffset((Image) loadImageMethod.invoke(emoji), -100));
		String emojiText = EmojiPalettePlugin.unescapeTags((String) triggerField.get(emoji));

		label.setToolTipText(emojiText);
		label.setIcon(icon);
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				plugin.insertEmoji(emojiText);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				label.setIcon(hoveredIcon);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				label.setIcon(icon);
			}
		});


		JPanel emojiPanel = new JPanel();
		emojiPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		emojiPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
		emojiPanel.add(label);

		return emojiPanel;
	}
}