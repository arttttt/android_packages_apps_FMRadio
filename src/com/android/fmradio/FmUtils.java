/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.fmradio;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * This class provider interface to compute station and frequency, get project
 * string
 */
public class FmUtils {
    private static final String TAG = "FmUtils";

    // FM station variables
    public static final int DEFAULT_STATION = 1000;
    public static final float DEFAULT_STATION_FLOAT = computeFrequency(DEFAULT_STATION);
    // convert rate: stations are in 100 kHz
    private static final int CONVERT_RATE = 10;
    private static final int KHZ_PER_STATION = 1000 / CONVERT_RATE;

    // minimum storage space for record (512KB).
    // Need to check before starting recording and during recording to avoid
    // recording keeps going but there is no free space in sdcard.
    public static final long LOW_SPACE_THRESHOLD = 512 * 1024;
    private static final String FM_IS_FIRST_TIME_PLAY = "fm_is_first_time_play";
    private static final String FM_IS_FIRST_ENTER_STATION_LIST = "fm_is_first_enter_station_list";
    // StorageManager For FM record
    private static StorageManager sStorageManager = null;

    /**
     * Whether the frequency is valid.
     *
     * @param station The FM station
     *
     * @return true if the frequency is in the valid scale, otherwise return
     *         false
     */
    public static boolean isValidStation(int station) {
        boolean isValid = (station >= getLowestStation() && station <= getHighestStation());
        return isValid;
    }

    /**
     * Compute increase station frequency
     *
     * @param station The station frequency
     *
     * @return station The frequency after increased
     */
    public static int computeIncreaseStation(int station) {
        int result = station + getStep();
        if (result > getHighestStation()) {
            result = getLowestStation();
        }
        return result;
    }

    /**
     * Compute decrease station frequency
     *
     * @param station The station frequency
     *
     * @return station The frequency after decreased
     */
    public static int computeDecreaseStation(int station) {
        int result = station - getStep();
        if (result < getLowestStation()) {
            result = getHighestStation();
        }
        return result;
    }

    /**
     * Compute station value with given frequency
     *
     * @param frequency The station frequency
     *
     * @return station The result value
     */
    public static int computeStation(float frequency) {
        return (int) (frequency * CONVERT_RATE);
    }

    /**
     * Compute frequency value with given station
     *
     * @param station The station value
     *
     * @return station The frequency
     */
    /**
     * The station for a frequency, to the nearest: a float such as 91.1
     * times CONVERT_RATE can fall just under 911
     *
     * @param frequency The frequency, in MHz
     * @return The station
     */
    public static int computeStationRounded(float frequency) {
        return Math.round(frequency * CONVERT_RATE);
    }

    /**
     * The region's step between stations
     *
     * @return The step, in stations
     */
    public static int getStep() {
        return FmRegion.current().stepKhz / KHZ_PER_STATION;
    }

    /**
     * A station within the region's band: the nearest of its ends if not
     *
     * @param station The station
     * @return The station, in the band
     */
    public static int clampStation(int station) {
        return Math.max(getLowestStation(), Math.min(getHighestStation(), station));
    }

    /**
     * The top of the band
     *
     * @return The highest station
     */
    public static int getHighestStation() {
        return FmRegion.current().highKhz / KHZ_PER_STATION;
    }

    /**
     * The bottom of the band
     *
     * @return The lowest station
     */
    public static int getLowestStation() {
        return FmRegion.current().lowKhz / KHZ_PER_STATION;
    }

    public static float computeFrequency(int station) {
        return (float) station / CONVERT_RATE;
    }

    /**
     * According station to get frequency string
     *
     * @param station for 100KZ, range 875-1080
     *
     * @return string like 87.5
     */
    public static String formatStation(int station) {
        float frequency = (float) station / CONVERT_RATE;
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        return decimalFormat.format(frequency);
    }

    /**
     * Get the phone storage path
     *
     * @return The phone storage path
     */
    public static String getDefaultStoragePath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * Get the default storage state
     *
     * @return The default storage state
     */
    public static String getDefaultStorageState(Context context) {
        ensureStorageManager(context);
        String state = sStorageManager.getVolumeState(getDefaultStoragePath());
        return state;
    }

    private static void ensureStorageManager(Context context) {
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
    }

    /**
     * Get the FM play list path
     *
     * @param context The context
     *
     * @return The FM play list path
     */
    public static String getPlaylistPath(Context context) {
        ensureStorageManager(context);
        String[] externalStoragePaths = sStorageManager.getVolumePaths();
        String path = externalStoragePaths[0] + "/Playlists/";
        return path;
    }

    /**
     * Check if has enough space for record
     *
     * @param recordingSdcard The recording sdcard path
     *
     * @return true if has enough space for record
     */
    public static boolean hasEnoughSpace(String recordingSdcard) {
        boolean ret = false;
        try {
            StatFs fs = new StatFs(recordingSdcard);
            long blocks = fs.getAvailableBlocks();
            long blockSize = fs.getBlockSize();
            long spaceLeft = blocks * blockSize;
            ret = spaceLeft > LOW_SPACE_THRESHOLD ? true : false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "hasEnoughSpace, sdcard may be unmounted:" + recordingSdcard);
        }
        return ret;
    }

    /**
     * check it is the first time to use Fm
     */
    public static boolean isFirstTimePlayFm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstTime = prefs.getBoolean(FM_IS_FIRST_TIME_PLAY, true);
        return isFirstTime;
    }

    /**
     * Called when first time play FM.
     * @param context The context
     */
    public static void setIsFirstTimePlayFm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FM_IS_FIRST_TIME_PLAY, false);
        editor.commit();
    }

    /**
     * check it is the first time enter into station list page
     */
    public static boolean isFirstEnterStationList(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstEnter = prefs.getBoolean(FM_IS_FIRST_ENTER_STATION_LIST, true);
        if (isFirstEnter) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(FM_IS_FIRST_ENTER_STATION_LIST, false);
            editor.commit();
        }
        return isFirstEnter;
    }

    /**
     * Create the notification large icon bitmap from layout
     * @param c The context
     * @param text The frequency text
     * @return The large icon bitmap with frequency text
     */
    public static Bitmap createNotificationLargeIcon(Context c, String text) {
        Resources res = c.getResources();
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        LinearLayout iconLayout = new LinearLayout(c);
        iconLayout.setOrientation(LinearLayout.VERTICAL);
        iconLayout.setBackgroundColor(c.getResources().getColor(R.color.theme_primary_color));
        iconLayout.setDrawingCacheEnabled(true);
        iconLayout.layout(0, 0, width, height);
        TextView iconText = new TextView(c);
        iconText.setTextSize(24.0f);
        iconText.setTextColor(res.getColor(R.color.theme_title_color));
        iconText.setText(text);
        iconText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int left = (int) ((width - iconText.getMeasuredWidth()) * 0.5);
        int top = (int) ((height - iconText.getMeasuredHeight()) * 0.5);
        iconText.layout(left, top, iconText.getMeasuredWidth() + left,
                iconText.getMeasuredHeight() + top);
        iconLayout.addView(iconText);
        iconLayout.layout(0, 0, width, height);

        iconLayout.buildDrawingCache();
        Bitmap largeIcon = Bitmap.createBitmap(iconLayout.getDrawingCache());
        iconLayout.destroyDrawingCache();
        return largeIcon;
    }
}
