/*
 * Copyright (c) 2016 Carmen Alvarez
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

package ca.rmen.android.poetassistant.wotd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import javax.inject.Inject;

import ca.rmen.android.poetassistant.Constants;
import ca.rmen.android.poetassistant.DaggerHelper;
import ca.rmen.android.poetassistant.settings.SettingsPrefs;

public class WotdBootReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.TAG + WotdBootReceiver.class.getSimpleName();

    @Inject
    SettingsPrefs mSettingsPrefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive: intent = " + intent);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            DaggerHelper.getAppComponent(context).inject(this);
            Wotd.reschedule(context, mSettingsPrefs);
        }
    }

}
