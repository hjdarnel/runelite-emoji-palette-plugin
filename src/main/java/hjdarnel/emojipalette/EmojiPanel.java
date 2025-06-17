package hjdarnel.emojipalette;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

@Slf4j
class EmojiPanel extends PluginPanel
{

	private static final File EMOJI_DIR = new File(RuneLite.CACHE_DIR, "emojis");

	void init()
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

		for (Emoji emoji : Emoji.values())
		{
			String name = emoji.name();
			String id = Integer.toHexString(emoji.codepoint);

			try
			{
				BufferedImage img = loadEmojiFromDisk(emoji.name(), id);

				JPanel oneCell = new JPanel();
				oneCell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				oneCell.setBorder(new EmptyBorder(2, 0, 2, 0));

				JLabel label = new JLabel(new ImageIcon(img));
				label.setToolTipText(emoji.trigger);

				oneCell.add(label);
				emojiGrid.add(oneCell);
			}
			catch (IOException e)
			{
				log.error("Unable to load emoji {}", name, e);
			}
		}

		add(emojiGrid);
	}

	private BufferedImage loadEmojiFromDisk(String name, String id) throws IOException
	{
		try (ZipFile zipFile = new ZipFile(new File(EMOJI_DIR, "assets.zip")))
		{
			ZipEntry entry = zipFile.getEntry(id + ".png");
			if (entry != null)
			{
				try (var in = zipFile.getInputStream(entry))
				{
					BufferedImage image;
					synchronized (ImageIO.class)
					{
						image = ImageIO.read(in);
					}

					log.debug("Loaded emoji {}: {}", name, id);
					return image;
				}
			}
			throw new IOException("file " + id + ".png doesn't exist");
		}
	}
}