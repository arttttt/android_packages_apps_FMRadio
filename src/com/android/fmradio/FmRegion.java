/*
 * Copyright (C) 2026 Artem Bambalov
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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The FM band of a part of the world: its limits, the step its stations
 * keep to, and the transmitters' de-emphasis. The user picks one; until
 * then, the one of the country the device is in, by its time zone (a
 * tablet may have no SIM to tell), or by its language.
 */
public enum FmRegion {
    // most of the world
    WORLD(R.string.region_world, 87500, 108000, 100, 50),
    // ITU Region 2, Korea and the Philippines: odd tenths of a MHz, 75 us
    AMERICAS(R.string.region_americas, 87500, 107900, 200, 75),
    JAPAN(R.string.region_japan, 76000, 95000, 100, 50);

    private static final String TAG = "FmRegion";
    private static final String PREF_REGION = "fm_region";

    // countries on the Americas' band: ITU Region 2, South Korea and the
    // Philippines
    private static final List<String> AMERICAS_COUNTRIES = Arrays.asList(
            "US", "CA", "MX", "GT", "BZ", "SV", "HN", "NI", "CR", "PA",
            "CU", "JM", "HT", "DO", "PR", "BS", "BB", "TT", "AG", "DM",
            "GD", "KN", "LC", "VC", "AI", "AW", "BM", "BQ", "CW", "KY",
            "MS", "SX", "TC", "VG", "VI", "GP", "MQ", "BL", "MF", "PM",
            "CO", "VE", "GY", "SR", "GF", "EC", "PE", "BO", "BR", "PY",
            "UY", "AR", "CL", "FK", "GL", "KR", "PH");

    private static FmRegion sCurrent = null;

    public final int nameRes;
    public final int lowKhz;
    public final int highKhz;
    public final int stepKhz;
    public final int deemphasisUs;

    FmRegion(int nameRes, int lowKhz, int highKhz, int stepKhz, int deemphasisUs) {
        this.nameRes = nameRes;
        this.lowKhz = lowKhz;
        this.highKhz = highKhz;
        this.stepKhz = stepKhz;
        this.deemphasisUs = deemphasisUs;
    }

    /**
     * The region in use: the user's, or the country's
     *
     * @param context The context
     * @return The region
     */
    public static synchronized FmRegion get(Context context) {
        if (sCurrent == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String name = prefs.getString(PREF_REGION, null);
            sCurrent = byName(name);
            if (sCurrent == null) {
                sCurrent = forCountry(country());
                Log.d(TAG, "no region set, the country's: " + sCurrent);
            }
        }
        return sCurrent;
    }

    /**
     * The region in use, once get(Context) has read it: the band for code
     * with no context at hand
     *
     * @return The region, WORLD before it is read
     */
    public static synchronized FmRegion current() {
        return sCurrent != null ? sCurrent : WORLD;
    }

    /**
     * The user picks a region
     *
     * @param context The context
     * @param region The region
     */
    public static synchronized void set(Context context, FmRegion region) {
        sCurrent = region;
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_REGION, region.name()).apply();
    }

    private static FmRegion byName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static FmRegion forCountry(String country) {
        if ("JP".equals(country)) {
            return JAPAN;
        }
        if (AMERICAS_COUNTRIES.contains(country)) {
            return AMERICAS;
        }
        return WORLD;
    }

    /* The country the device is in: by its time zone, else its language */
    private static String country() {
        String country = null;
        try {
            country = android.icu.util.TimeZone.getRegion(
                    java.util.TimeZone.getDefault().getID());
        } catch (IllegalArgumentException e) {
            // not a zone ICU knows
        }
        // "001" and the like: not a country
        if (country == null || country.length() != 2) {
            country = Locale.getDefault().getCountry();
        }
        return country.toUpperCase(Locale.ROOT);
    }
}
