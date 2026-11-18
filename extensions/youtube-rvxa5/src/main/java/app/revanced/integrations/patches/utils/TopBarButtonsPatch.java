package app.revanced.integrations.patches.utils;

import static app.revanced.integrations.utils.StringRef.str;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import app.revanced.integrations.settings.SettingsEnum;
import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ReVancedUtils;
import app.revanced.integrations.utils.ResourceUtils;

/**
 * Adds a button to the top bar (left of the notification button) that only exists while a tab of the bottom bar is selected:
 * <ul>
 *     <li>Subscriptions tab: a button with the subscriptions icon that opens the list of the subscribed channels.</li>
 *     <li>You tab: a gear button that opens the settings.</li>
 * </ul>
 * The old UI of 16.40.36 does not show these buttons that the newer server layout sends.
 * <p>
 * Injection points: {@link #onPivotTabSelected(String)}, {@link #onPaneChanged(boolean)} and
 * {@link #onMenuPopulated(Menu, int)}.
 * <p>
 * The settings button opens the settings activity of the app through its shell activity (it adds the account). The shell
 * activity gets a transparent theme from the "Add missing resources" patch, so no black window shows up before the settings.
 */
@SuppressWarnings("unused")
public class TopBarButtonsPatch {
    // Item ids of the menu items. They must not clash with the resource ids of the app (0x7f......).
    private static final int ITEM_ID_SUBSCRIPTIONS = 0x5A5A0001;
    private static final int ITEM_ID_SETTINGS = 0x5A5A0002;

    /**
     * Category index 5 of the framework menu sorts before every other item: the button is the left most one of the
     * action buttons, so it is left of the notification button (the cast button, when shown, stays left of it).
     */
    private static final int ORDER_FIRST = 0x50000;

    private static final String CHANNELS_URL = "https://m.youtube.com/feed/channels";
    private static final String SETTINGS_ACTIVITY = "com.google.android.apps.youtube.app.application.Shell_SettingsActivity";

    private enum Tab {
        NONE,
        SUBSCRIPTIONS,
        LIBRARY
    }

    /**
     * Identifier of the selected tab of the bottom bar ("FEwhat_to_watch", "FEsubscriptions", "FElibrary", ...).
     */
    private static String selectedTabId = "";
    /**
     * If the page that is shown is the first page of a tab (not a channel, a playlist, search results, ...).
     */
    private static boolean atTabRoot = true;

    /**
     * Every pane (Home, Subscriptions, You ...) has its own top bar with its own menu, and the app keeps the panes of the
     * tabs alive: a menu is filled once and is not filled again when its tab is selected again. So the buttons are updated in
     * all menus that were filled (menu -> color of the icons). The menus are weak keys, they go away with their top bar.
     */
    private static final Map<Menu, Integer> menus = new WeakHashMap<>();

    /**
     * Injection point. A tab of the bottom bar got selected.
     *
     * @param tabId identifier of the selected tab, empty if no tab is selected.
     */
    public static void onPivotTabSelected(String tabId) {
        try {
            String newTabId = tabId == null ? "" : tabId;
            // Selecting (or selecting again) a tab shows the first page of that tab.
            boolean changed = !newTabId.equals(selectedTabId) || !atTabRoot;

            LogHelper.info(TopBarButtonsPatch.class, "Pivot tab selected: " + newTabId);
            selectedTabId = newTabId;
            atTabRoot = true;
            if (changed) refreshAllMenus();
        } catch (Exception ex) {
            LogHelper.printException(TopBarButtonsPatch.class, "onPivotTabSelected failure", ex);
        }
    }

    /**
     * Injection point. The page that is shown changed.
     *
     * @param isTabRoot true if the new page is the first page of a tab of the bottom bar.
     */
    public static void onPaneChanged(boolean isTabRoot) {
        try {
            if (isTabRoot == atTabRoot) return;

            LogHelper.info(TopBarButtonsPatch.class, "Page is the root of a tab: " + isTabRoot);
            atTabRoot = isTabRoot;
            refreshAllMenus();
        } catch (Exception ex) {
            LogHelper.printException(TopBarButtonsPatch.class, "onPaneChanged failure", ex);
        }
    }

    /**
     * Injection point. The menu of a top bar was filled with the buttons of the app.
     *
     * @param menu      the menu of the top bar.
     * @param iconColor the color of the icons of the top bar.
     */
    public static void onMenuPopulated(Menu menu, int iconColor) {
        try {
            if (menu == null) return;

            synchronized (menus) {
                menus.put(menu, iconColor);
            }
            updateMenu(menu, iconColor);
        } catch (Exception ex) {
            LogHelper.printException(TopBarButtonsPatch.class, "onMenuPopulated failure", ex);
        }
    }

    private static void refreshAllMenus() {
        List<Menu> toUpdate = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        synchronized (menus) {
            for (Map.Entry<Menu, Integer> entry : menus.entrySet()) {
                Menu menu = entry.getKey();
                if (menu == null) continue;

                toUpdate.add(menu);
                colors.add(entry.getValue());
            }
        }

        for (int i = 0, size = toUpdate.size(); i < size; i++) {
            try {
                updateMenu(toUpdate.get(i), colors.get(i));
            } catch (Exception ex) {
                LogHelper.printException(TopBarButtonsPatch.class, "Menu update failure", ex);
            }
        }
    }

    private static Tab getCurrentTab() {
        if (!atTabRoot) return Tab.NONE;

        String id = selectedTabId.toLowerCase(Locale.ENGLISH);
        if (id.contains("subscriptions")) return Tab.SUBSCRIPTIONS;
        if (id.contains("library")) return Tab.LIBRARY;
        return Tab.NONE;
    }

    private static void updateMenu(Menu menu, int iconColor) {
        menu.removeItem(ITEM_ID_SUBSCRIPTIONS);
        menu.removeItem(ITEM_ID_SETTINGS);

        // Not a toolbar menu with the buttons of the app.
        if (menu.size() == 0) return;

        Context context = ReVancedUtils.getContext();
        if (context == null) return;

        switch (getCurrentTab()) {
            case SUBSCRIPTIONS:
                if (SettingsEnum.TOP_BAR_SUBSCRIPTIONS_BUTTON.getBoolean()) {
                    addItem(menu, context, iconColor, ITEM_ID_SUBSCRIPTIONS,
                            new String[]{"revanced_top_bar_subscriptions", "yt_outline_subscriptions_black_24"},
                            str("revanced_top_bar_subscriptions_button_description"),
                            TopBarButtonsPatch::openChannelsList);
                }
                break;
            case LIBRARY:
                if (SettingsEnum.TOP_BAR_SETTINGS_BUTTON.getBoolean()) {
                    addItem(menu, context, iconColor, ITEM_ID_SETTINGS,
                            new String[]{"revanced_top_bar_settings", "yt_outline_gear_black_24"},
                            str("revanced_top_bar_settings_button_description"),
                            TopBarButtonsPatch::openSettings);
                }
                break;
            default:
                break;
        }
    }

    private static void addItem(Menu menu, Context context, int iconColor, int itemId,
                                String[] iconNames, CharSequence title, Runnable onClick) {
        MenuItem item;
        try {
            item = menu.add(Menu.NONE, itemId, ORDER_FIRST, title);
        } catch (IllegalArgumentException ex) {
            // The menu does not know the category: use the normal order.
            item = menu.add(Menu.NONE, itemId, Menu.NONE, title);
        }

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        Drawable icon = loadIcon(context, iconNames, iconColor);
        if (icon != null) item.setIcon(icon);

        item.setOnMenuItemClickListener(clicked -> {
            try {
                onClick.run();
            } catch (Exception ex) {
                LogHelper.printException(TopBarButtonsPatch.class, "Top bar button failure", ex);
            }
            return true;
        });
    }

    /**
     * @param names names of the drawable, the first one that exists is used (the first name is the alias that
     *              "Add missing resources" adds, the others are the icons of the app itself).
     */
    private static Drawable loadIcon(Context context, String[] names, int color) {
        for (String name : names) {
            int id = ResourceUtils.identifier(name, "drawable", context);
            if (id == 0) continue;

            Drawable icon = context.getResources().getDrawable(id);
            if (icon == null) continue;

            icon = icon.mutate();
            // The icons of the app are black: they are tinted like the other buttons of the top bar.
            icon.setColorFilter(Color.alpha(color) == 0 ? Color.BLACK : color, PorterDuff.Mode.SRC_IN);
            return icon;
        }

        LogHelper.info(TopBarButtonsPatch.class, "Icon not found: " + names[0]);
        return null;
    }

    private static void startActivity(Intent intent) {
        Context context = ReVancedUtils.getContext();
        if (context == null) return;

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    private static void openChannelsList() {
        Context context = ReVancedUtils.getContext();
        if (context == null) return;

        // The URL is resolved by the app itself ("All subscriptions" page).
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(CHANNELS_URL));
        intent.setPackage(context.getPackageName());
        startActivity(intent);
    }

    private static void openSettings() {
        Context context = ReVancedUtils.getContext();
        if (context == null) return;

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(context.getPackageName(), SETTINGS_ACTIVITY);
        startActivity(intent);
    }
}
