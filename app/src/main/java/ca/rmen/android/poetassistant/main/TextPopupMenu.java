/*
 * Copyright (c) 2016-2017 Carmen Alvarez
 *
 * This file is part of Poet Assistant.
 *
 * Poet Assistant is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poet Assistant is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Poet Assistant.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.android.poetassistant.main;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import ca.rmen.android.poetassistant.R;
import ca.rmen.android.poetassistant.main.dictionaries.Share;
import ca.rmen.android.poetassistant.main.dictionaries.rt.OnWordClickListener;
import ca.rmen.android.poetassistant.widget.HackFor23381;
import java8.util.stream.StreamSupport;

public class TextPopupMenu {
    public enum Style {
        /**
         * Denotes popup menu items for looking up a text in the rhymer, thesaurus, or dictionary
         */
        APP,
        /**
         * Denotes popup menu items for copying text, and any other actions provided by the system.
         */
        SYSTEM,
        /**
         * Denotes both APP and SYSTEM popup menu items.
         */
        FULL
    }

    /**
     * Create a popup menu allowing the user perform actions on the whole text of the given text view.
     *
     * @param style    determines what popup menu items will appear. See {@link Style}.
     * @param textView tapping on it will bring up the popup menu
     * @param listener this listener will be notified when the user selects one of the popup menu items
     */
    public static void addPopupMenu(Style style, final TextView textView, final OnWordClickListener listener) {
        textView.setOnClickListener(v -> {
            String text = textView.getText().toString();
            PopupMenu popupMenu = createPopupMenu(textView, text, listener);
            MenuPopupHelper menuHelper = new MenuPopupHelper(textView.getContext(), (MenuBuilder) popupMenu.getMenu(), textView);
            if (style == Style.APP) {
                addAppMenuItems(popupMenu);
            } else if (style == Style.SYSTEM) {
                addSystemMenuItems(textView.getContext(), popupMenu.getMenuInflater(), popupMenu.getMenu(), text);
            } else if (style == Style.FULL) {
                addAppMenuItems(popupMenu);
                SubMenu systemMenu = popupMenu.getMenu().addSubMenu(R.string.menu_more);
                addSystemMenuItems(textView.getContext(), popupMenu.getMenuInflater(), systemMenu, text);
            }
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        });
    }

    /**
     * Create a popup menu allowing the user to lookup the selected text in the given text view, in
     * the rhymer, thesaurus, or dictionary.
     *
     * @param textView if the text view has selected text, tapping on it will bring up the popup menu
     * @param listener this listener will be notified when the user selects one of the popup menu items
     */
    public static void addSelectionPopupMenu(final TextView textView, final OnWordClickListener listener) {
        textView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_word_lookup, menu);
                mode.setTitle(null);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // https://code.google.com/p/android/issues/detail?id=23381
                if (textView instanceof HackFor23381) ((HackFor23381) textView).setWindowFocusWait(true);
                for (int i = 0; i < menu.size(); i++) {
                    MenuItem menuItem = menu.getItem(i);
                    Intent intent = menuItem.getIntent();
                    // Hide our own process text action meant for other apps.
                    if (intent != null
                            && Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())
                            && textView.getContext().getApplicationInfo().packageName.equals(intent.getComponent().getPackageName())) {
                        menuItem.setVisible(false);
                    }
                }
                return false;
            }


            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return handleItemClicked(item.getItemId(), textView, getSelectedWord(textView), listener);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // https://code.google.com/p/android/issues/detail?id=23381
                if (textView instanceof HackFor23381) ((HackFor23381) textView).setWindowFocusWait(false);
            }
        });
    }

    private static boolean handleItemClicked(int itemId, View view, String selectedWord, OnWordClickListener listener) {
        if (itemId == R.id.action_lookup_rhymer) {
            listener.onWordClick(selectedWord, Tab.RHYMER);
        } else if (itemId == R.id.action_lookup_thesaurus) {
            listener.onWordClick(selectedWord, Tab.THESAURUS);
        } else if (itemId == R.id.action_lookup_dictionary) {
            listener.onWordClick(selectedWord, Tab.DICTIONARY);
        } else if (itemId == R.id.action_copy) {
            ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(selectedWord, selectedWord);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(view, R.string.snackbar_copied_text, Snackbar.LENGTH_SHORT).show();
        } else if (itemId == R.id.action_share) {
            Share.share(view.getContext(), selectedWord);
        } else {
            return false;
        }
        return true;
    }

    private static PopupMenu createPopupMenu(View view, String selectedWord, OnWordClickListener listener) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.setOnMenuItemClickListener(item -> {
            handleItemClicked(item.getItemId(), view, selectedWord, listener);
            return false;
        });
        return popupMenu;
    }

    private static void addAppMenuItems(PopupMenu popupMenu) {
        popupMenu.inflate(R.menu.menu_word_lookup);
    }

    private static void addSystemMenuItems(Context context, MenuInflater menuInflater, Menu menu, String text) {
        menuInflater.inflate(R.menu.menu_word_other, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StreamSupport.stream(getSupportedActivities(context, text))
                    .filter(resolveInfo -> !context.getApplicationInfo().packageName.equals(resolveInfo.activityInfo.packageName))
                    .forEach(resolveInfo -> menu.add(
                            R.id.group_system_popup_menu_items, Menu.NONE,
                            Menu.NONE,
                            resolveInfo.loadLabel(context.getPackageManager()))
                            .setIcon(resolveInfo.loadIcon(context.getPackageManager()))
                            .setIntent(createProcessTextIntentForResolveInfo(resolveInfo, text))
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static List<ResolveInfo> getSupportedActivities(Context context, String text) {
        //https://android-developers.googleblog.com/2015/10/in-app-translations-in-android.html?hl=mk
        return context.getPackageManager().queryIntentActivities(createProcessTextIntent(text), 0);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static Intent createProcessTextIntent(String text) {
        //https://android-developers.googleblog.com/2015/10/in-app-translations-in-android.html?hl=mk
        return new Intent()
                .setAction(Intent.ACTION_PROCESS_TEXT)
                .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                .setType("text/plain");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static Intent createProcessTextIntentForResolveInfo(ResolveInfo info, String text) {
        //https://android-developers.googleblog.com/2015/10/in-app-translations-in-android.html?hl=mk
        return createProcessTextIntent(text)
                .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                .setClassName(info.activityInfo.packageName,
                        info.activityInfo.name);
    }

    private static String getSelectedWord(TextView textView) {
        int selectionStart = textView.getSelectionStart();
        int selectionEnd = textView.getSelectionEnd();

        // The user selected some text, use that (even if it contains partial words)
        if (selectionStart < selectionEnd) {
            String text = textView.getText().toString();
            return text.substring(selectionStart, selectionEnd);
        }

        return null;
    }

}
