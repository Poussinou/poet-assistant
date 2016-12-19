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

package ca.rmen.android.poetassistant.main.dictionaries.search;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import javax.inject.Inject;

import ca.rmen.android.poetassistant.DaggerHelper;
import ca.rmen.android.poetassistant.main.dictionaries.DaoSession;

public class SuggestionsProvider extends ContentProvider {
    public static final Uri CONTENT_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SuggestionsProvider.AUTHORITY)
            .build();

    private static final String AUTHORITY = "ca.rmen.android.poetassistant.SuggestionsProvider";
    private static final int URI_MATCH_SUGGEST = 1;

    @Inject
    DaoSession mDaoSession;
    private final UriMatcher mUriMatcher;

    public SuggestionsProvider() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_MATCH_SUGGEST);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        DaggerHelper.getAppComponent(context).inject(this);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String sel,
                        String[] selArgs, String sortOrder) {
        String lastPathSegment = uri.getLastPathSegment();
        String filter = null;
        if (!TextUtils.equals(lastPathSegment, SearchManager.SUGGEST_URI_PATH_QUERY)) {
            filter = lastPathSegment;
        }
        return new SuggestionsCursor(getContext(), filter);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if (mUriMatcher.match(uri) == URI_MATCH_SUGGEST) {
            return SearchManager.SUGGEST_MIME_TYPE;
        }
        throw new IllegalArgumentException("Unknown Uri");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        String suggestion = values.getAsString(SearchManager.QUERY);
        if (mDaoSession.getSuggestionDao()
                .queryBuilder()
                .where(SuggestionDao.Properties.Word.eq(suggestion))
                .buildCount()
                .count() == 0) {
            mDaoSession.getSuggestionDao().insertOrReplace(new Suggestion(suggestion));
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        mDaoSession.getSuggestionDao().deleteAll();
        mDaoSession.clear();
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

}
