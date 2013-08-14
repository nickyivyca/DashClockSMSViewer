package com.nickivy.dashclocksmsviewer;

import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import com.nickivy.dashclocksmsviewer.R;

import android.preference.PreferenceManager;

public class Panel3 extends DashClockExtension {
	
    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        if (!isReconnect) {
            addWatchContentUris(new String[]{
                    TelephonyProviderConstants.MmsSms.CONTENT_URI.toString(),
            });
        }
        setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int reason) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean switcher = sp.getBoolean(SMSViewer.PREF_SWITCH, false);
        Intent clickIntent;
        if (SMSViewer.panel3_threadId > 0) {
            clickIntent = new Intent(Intent.ACTION_VIEW,
                    TelephonyProviderConstants.MmsSms.CONTENT_CONVERSATIONS_URI.buildUpon()
                            .appendPath(Long.toString(SMSViewer.panel3_threadId)).build());
        } else {
            clickIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_MESSAGING);
        }

        publishUpdate(new ExtensionData()
                .visible(SMSViewer.panel3_visible)
                .icon(R.drawable.ic_icon)
                .status(Integer.toString(SMSViewer.nummsg[2]))
                .expandedTitle(switcher?  SMSViewer.panel3_contents : SMSViewer.panel2_title)
                .expandedBody(switcher? SMSViewer.panel3_title : SMSViewer.panel2_contents)
                .clickIntent(clickIntent));
    	   
    }
}

