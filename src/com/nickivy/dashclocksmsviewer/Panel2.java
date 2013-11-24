package com.nickivy.dashclocksmsviewer;

import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import com.nickivy.dashclocksmsviewer.R;

import android.net.Uri;
import android.preference.PreferenceManager;

public class Panel2 extends DashClockExtension {
	
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
        Intent clickIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:" + SMSViewer.panel2_address));

        publishUpdate(new ExtensionData()
                .visible(SMSViewer.panel2_visible)
                .icon(R.drawable.ic_icon)
                .status(Integer.toString(SMSViewer.nummsg[1]))
                .expandedTitle(switcher?  SMSViewer.panel2_contents : SMSViewer.panel2_title)
                .expandedBody(switcher? SMSViewer.panel2_title : SMSViewer.panel2_contents)
                .clickIntent(clickIntent));
    	   
    }
}

