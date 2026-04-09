package menus;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import frames.GMainFrame.ThemeHandler;
import main.GConstants;
import main.GConstants.EThemeMenu;
import main.GConstants.EThemechangeMenu;


public class GThemeMenu extends JMenu {
	private static final long serialVersionUID = GConstants.serialVersionUID;

	public GThemeMenu(String name,ThemeHandler themeHandler) {
		super(name);
		
		
		JMenu setThememenu = new JMenu(EThemechangeMenu.eSetTheme.getTitle());
		this.add(setThememenu);
		for (EThemeMenu ethemeItem : EThemeMenu.values()) {
			JMenuItem themeItem = new JMenuItem(ethemeItem.getTitle());
			themeItem.setActionCommand(ethemeItem.getActionCommand());
			themeItem.addActionListener(themeHandler);
			setThememenu.add(themeItem);
		}

		// 테마 초기화
		JMenuItem clearThememenu = new JMenuItem(EThemechangeMenu.eClearTheme.getTitle());
		clearThememenu.setActionCommand(EThemechangeMenu.eClearTheme.getActionCommand());
		clearThememenu.addActionListener(themeHandler);
		this.add(clearThememenu);
		
	}

	public void initialize() {}
}