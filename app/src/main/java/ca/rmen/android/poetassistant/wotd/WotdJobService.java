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

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import javax.inject.Inject;

import ca.rmen.android.poetassistant.Constants;
import ca.rmen.android.poetassistant.DaggerHelper;
import ca.rmen.android.poetassistant.main.dictionaries.dictionary.Dictionary;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WotdJobService extends JobService {
    private static final String TAG = Constants.TAG + WotdJobService.class.getSimpleName();
    @Inject Dictionary mDictionary;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerHelper.getAppComponent(this).inject(this);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.v(TAG, "onStartJob: params " + params);
        mTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private final AsyncTask<JobParameters, Void, Void> mTask = new AsyncTask<JobParameters, Void, Void>() {

        @Override
        protected Void doInBackground(JobParameters... params) {
            Wotd.notifyWotd(getApplicationContext(), mDictionary);
            jobFinished(params[0], false);
            return null;
        }
    };
}
