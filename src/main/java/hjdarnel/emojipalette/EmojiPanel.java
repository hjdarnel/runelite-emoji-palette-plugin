package hjdarnel.emojipalette;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;

@Slf4j
class EmojiPanel extends PluginPanel
{

	private static final File EMOJI_DIR = new File(RuneLite.CACHE_DIR, "emojis");
	private static final File INDEX_FILE = new File(EMOJI_DIR, "index.json");
	private static final File ASSETS_FILE = new File(EMOJI_DIR, "assets.zip");

	private static final int COLUMNS = 7;

	private static final String LOADING_TEXT = "Loading emojis...";
	private static final String NO_RESULTS_TEXT = "No emojis found";
	private static final String NO_CACHE_TEXT = "<html><div style='text-align: center;'>No emoji images found."
		+ "<br><br>Enable the client's built-in Emojis plugin to download them, then restart the client.</div></html>";
	private final IconTextField searchBar = new IconTextField();
	private final JPanel emojiGrid = new JPanel(new GridLayout(0, COLUMNS));
	private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
	private final JPanel scrollContent = new FixedWidthPanel();
	private final List<EmojiCell> cells = new ArrayList<>();
	@Inject
	private Gson gson;
	@Inject
	private ScheduledExecutorService executor;
	private String status;

	EmojiPanel()
	{
		// let the header and search bar stay pinned
		super(false);
	}

	void init()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final PluginErrorPanel errorPanel = new PluginErrorPanel();
		errorPanel.setBorder(new EmptyBorder(10, 25, 10, 25));
		errorPanel.setContent("Emoji Palette", "Hover over an emoji to view the text trigger");

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addClearListener(this::filter);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				filter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				filter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				filter();
			}
		});

		emojiGrid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		emojiGrid.setBorder(new EmptyBorder(5, 0, 0, 0));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

		JPanel topPanel = new JPanel(new BorderLayout(0, BORDER_OFFSET));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		topPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
		topPanel.add(errorPanel, BorderLayout.NORTH);
		topPanel.add(searchBar, BorderLayout.CENTER);

		scrollContent.setLayout(new BorderLayout());
		scrollContent.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollContent.setBorder(new EmptyBorder(0, 10, 15, 10));

		JScrollPane scrollPane = new JScrollPane(scrollContent);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

		add(topPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		setStatus(LOADING_TEXT);
		executor.execute(this::load);
	}

	private void load()
	{
		final List<LoadedEmoji> loaded = loadEmojis();

		SwingUtilities.invokeLater(() ->
		{
			for (LoadedEmoji emoji : loaded)
			{
				cells.add(new EmojiCell(emoji));
			}

			if (cells.isEmpty())
			{
				setStatus(NO_CACHE_TEXT);
				return;
			}

			log.debug("Loaded {} emojis", cells.size());
			filter();
		});
	}

	/**
	 * Builds the display list: the {@link Emoji} enum first, in its declared order, so the emojis with a short
	 * text trigger stay at the top, then everything else the client knows about in alphabetical order.
	 */
	private List<LoadedEmoji> loadEmojis()
	{
		final Map<String, String> codepointsByName = readIndex();

		final Map<String, String> nameByCodepoint = new HashMap<>();
		for (Map.Entry<String, String> entry : codepointsByName.entrySet())
		{
			nameByCodepoint.putIfAbsent(entry.getValue().toLowerCase(Locale.ROOT), entry.getKey());
		}

		final Map<String, EmojiEntry> entries = new LinkedHashMap<>();

		for (Emoji emoji : Emoji.values())
		{
			String id = Integer.toHexString(emoji.codepoint);
			// Keep the enum name searchable alongside the client's much longer one
			entries.putIfAbsent(id, new EmojiEntry(id, emoji.trigger, emoji.name(), nameByCodepoint.get(id)));
		}

		for (Map.Entry<String, String> entry : new TreeMap<>(codepointsByName).entrySet())
		{
			String name = entry.getKey();
			String id = entry.getValue().toLowerCase(Locale.ROOT);
			entries.putIfAbsent(id, new EmojiEntry(id, ':' + name + ':', name, null));
		}

		return loadImages(entries.values());
	}

	private Map<String, String> readIndex()
	{
		if (!INDEX_FILE.exists())
		{
			log.debug("Emoji index {} does not exist", INDEX_FILE);
			return Collections.emptyMap();
		}

		try (Reader in = new InputStreamReader(new FileInputStream(INDEX_FILE), StandardCharsets.UTF_8))
		{
			Index index = gson.fromJson(in, Index.class);
			if (index != null && index.names != null)
			{
				return index.names;
			}
		}
		catch (IOException | JsonParseException e)
		{
			log.error("Unable to read emoji index", e);
		}

		return Collections.emptyMap();
	}

	private List<LoadedEmoji> loadImages(Iterable<EmojiEntry> entries)
	{
		final List<LoadedEmoji> loaded = new ArrayList<>();

		try (ZipFile zipFile = new ZipFile(ASSETS_FILE))
		{
			for (EmojiEntry entry : entries)
			{
				ZipEntry zipEntry = zipFile.getEntry(entry.id + ".png");
				if (zipEntry == null)
				{
					log.debug("No image for emoji {}: {}.png doesn't exist", entry.trigger, entry.id);
					continue;
				}

				try (var in = zipFile.getInputStream(zipEntry))
				{
					BufferedImage image;
					synchronized (ImageIO.class)
					{
						image = ImageIO.read(in);
					}

					loaded.add(new LoadedEmoji(entry, image));
				}
				catch (IOException e)
				{
					log.error("Unable to load emoji {}", entry.id, e);
				}
			}
		}
		catch (IOException e)
		{
			log.debug("Unable to open emoji assets {}", ASSETS_FILE, e);
		}

		return loaded;
	}

	private void filter()
	{
		final String query = searchBar.getText().trim().toLowerCase(Locale.ROOT);

		emojiGrid.removeAll();

		int matches = 0;
		for (EmojiCell cell : cells)
		{
			if (cell.matches(query))
			{
				emojiGrid.add(cell.panel);
				matches++;
			}
		}

		if (!cells.isEmpty())
		{
			setStatus(matches == 0 ? NO_RESULTS_TEXT : null);
		}

		emojiGrid.revalidate();
		emojiGrid.repaint();
	}

	/**
	 * Swaps the grid out for a message, or back again when {@code text} is null. The two are mutually exclusive
	 * because an empty grid still draws its border as a stray band above the message.
	 */
	private void setStatus(String text)
	{
		if (Objects.equals(status, text))
		{
			return;
		}

		status = text;
		scrollContent.removeAll();

		if (text == null)
		{
			scrollContent.add(emojiGrid, BorderLayout.NORTH);
		}
		else
		{
			statusLabel.setText(text);
			scrollContent.add(statusLabel, BorderLayout.NORTH);
		}

		scrollContent.revalidate();
		scrollContent.repaint();
	}

	/**
	 * Keeps the scrolled content at the panel width so the scrollbar sits beside it rather than over it.
	 * The client has its own copy of this, but it is package private.
	 */
	private static class FixedWidthPanel extends JPanel
	{
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(PANEL_WIDTH, super.getPreferredSize().height);
		}
	}

	private static class Index
	{
		Map<String, String> names;
	}

	private static class EmojiEntry
	{
		private final String id;
		private final String trigger;
		private final String searchText;

		private EmojiEntry(String id, String trigger, String name, String alternateName)
		{
			this.id = id;
			this.trigger = trigger;

			// Underscores are swapped for spaces so "heart eyes" matches HEART_EYES
			StringBuilder text = new StringBuilder(name).append(' ').append(trigger);
			if (alternateName != null)
			{
				text.append(' ').append(alternateName);
			}
			this.searchText = text.toString().replace('_', ' ').toLowerCase(Locale.ROOT);
		}
	}

	private static class LoadedEmoji
	{
		private final EmojiEntry entry;
		private final BufferedImage image;

		private LoadedEmoji(EmojiEntry entry, BufferedImage image)
		{
			this.entry = entry;
			this.image = image;
		}
	}

	private static class EmojiCell
	{
		private final String searchText;
		private final JPanel panel;

		private EmojiCell(LoadedEmoji emoji)
		{
			this.searchText = emoji.entry.searchText;

			panel = new JPanel();
			panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			panel.setBorder(new EmptyBorder(2, 0, 2, 0));

			JLabel label = new JLabel(new ImageIcon(emoji.image));
			label.setToolTipText(emoji.entry.trigger);
			panel.add(label);
		}

		private boolean matches(String query)
		{
			return query.isEmpty() || searchText.contains(query);
		}
	}
}
