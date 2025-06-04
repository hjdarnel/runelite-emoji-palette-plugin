package hjdarnel.emojipalette;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
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

	void init() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));

		final PluginErrorPanel errorPanel = new PluginErrorPanel();
		errorPanel.setBorder(new EmptyBorder(10, 25, 10, 25));
		errorPanel.setContent("Emoji Palette", "Hover over an emoji to view the text trigger");
		add(errorPanel, BorderLayout.NORTH);

		JPanel emojiGrid = new JPanel();
		emojiGrid.setLayout(new GridLayout(0, 7));
		emojiGrid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		emojiGrid.setBorder(new EmptyBorder(5, 0, 5, 0));

		Class<?> emojiClass = getClass().getClassLoader().loadClass("net.runelite.client.plugins.emojis.Emoji");

		Field triggerField = emojiClass.getDeclaredField("trigger");
		triggerField.setAccessible(true);

		Method valuesMethod = emojiClass.getDeclaredMethod("values");
		valuesMethod.setAccessible(true);

		Object[] enumConstants = (Object[]) valuesMethod.invoke(null);

		for (Object enumConst : enumConstants)
		{
			String trigger = (String) triggerField.get(enumConst);
			String path = ((Enum<?>) enumConst).name().toLowerCase() + ".png";
			BufferedImage img = ImageUtil.loadImageResource(emojiClass, path);

			JPanel oneCell = new JPanel();
			oneCell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			oneCell.setBorder(new EmptyBorder(2, 0, 2, 0));

			JLabel label = new JLabel(new ImageIcon(img));
			label.setToolTipText(trigger);

			oneCell.add(label);
			emojiGrid.add(oneCell);
		}

		add(emojiGrid);
	}
}