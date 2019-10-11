package com.wix.reactnativenotifications.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by SK on 2016-10-18.
 */

public class PreferenceHolder {
    private String PREF_NAME;

    public final String N_POST_CONTENT_ID = "N_POST_CONTENT_ID";
    public final String N_POST_REPLY_ID = "N_POST_REPLY_ID";
    public final String N_POST_TODAK_ID = "N_POST_TODAK_ID";
    public final String N_TALK_ID = "N_TALK_ID";
    public final String N_RADIO_ID = "N_RADIO_ID";
    public final String N_OTHER_ID = "N_OTHER_ID";


    static Context mContext;
    public PreferenceHolder(Context c) {
        mContext = c;
        PREF_NAME = "com.cotoone.pref";         //초기화 할 프리퍼런스
    }

    public PreferenceHolder(Context c, String unInit) {
        mContext = c;
        PREF_NAME = "com.cotoone.pref_unInit";  //초기화 안 할 프리퍼런스
    }

    public void put(String key, String value) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putString(key, value);
            editor.commit();
        }
    }

    public void put(String key, int value) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putInt(key, value);
            editor.commit();
        }
    }

    public void put(String key, long value) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putLong(key, value);
            editor.commit();
        }
    }

    public void put(String key, boolean value) {
        if(mContext!=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putBoolean(key, value);
            editor.commit();
        }
    }

    public String getValue(String key, String dftValue) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);

            try {
                return pref.getString(key, dftValue);
            } catch (Exception e) {
                return dftValue;
            }
        }else return "";
    }

    public int getValue(String key, int dftValue) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);

            try {
                return pref.getInt(key, dftValue);
            } catch (Exception e) {
                return dftValue;
            }
        }else return 0;
    }

    public long getValue(String key, long dftValue) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);

            try {
                return pref.getLong(key, dftValue);
            } catch (Exception e) {
                return dftValue;
            }
        }else return 0;
    }

    public boolean getValue(String key, boolean dftValue) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);
            try {
                return pref.getBoolean(key, dftValue);
            } catch (Exception e) {
                return dftValue;
            }
        }else{
            return false;
        }
    }

    // 해당 키만 초기화 시키는 함수
    public void clear(String key) {
        if(mContext !=null) {
            SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.remove(key).apply();
        }
    }
}
