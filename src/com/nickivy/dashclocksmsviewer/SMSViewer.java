package com.nickivy.dashclocksmsviewer;

import android.content.Intent;
import android.net.Uri;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import com.nickivy.dashclocksmsviewer.R;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;

/**
 * This code is based off of the base SMS extension that comes with DashClock.
 * Instead of displaying the number of unread conversations in the 'status'
 * section and the senders in the 'body' section, this shows the sender in the
 * 'status' section and the text of the most recent message in the 'body' section,
 * unless there is more than one unread conversation.
 */
public class SMSViewer extends DashClockExtension {
//    private static final String TAG = LogUtils.makeLogTag(SmsExtension.class);

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        if (!isReconnect) {
            addWatchContentUris(new String[]{
                    TelephonyProviderConstants.MmsSms.CONTENT_URI.toString(),
            });
        }
    }

    @Override
    protected void onUpdateData(int reason) {
        int unreadConversations = 0;
        StringBuilder names = new StringBuilder();
        Cursor cursor = openMmsSmsCursor();
        long threadId = 0;

        while (cursor.moveToNext()) {
            ++unreadConversations;

            // Get display name. SMS's are easy; MMS's not so much.
            long id = cursor.getLong(MmsSmsQuery._ID);
            long contactId = cursor.getLong(MmsSmsQuery.PERSON);
            String address = cursor.getString(MmsSmsQuery.ADDRESS);
            threadId = cursor.getLong(MmsSmsQuery.THREAD_ID);

            if (contactId == 0 && TextUtils.isEmpty(address) && id != 0) {
                // Try MMS addr query
                Cursor addrCursor = openMmsAddrCursor(id);
                if (addrCursor.moveToFirst()) {
                    contactId = addrCursor.getLong(MmsAddrQuery.CONTACT_ID);
                    address = addrCursor.getString(MmsAddrQuery.ADDRESS);
                }
                addrCursor.close();
            }

            String displayName = address;

            if (contactId > 0) {
                Cursor contactCursor = openContactsCursorById(contactId);
                if (contactCursor.moveToFirst()) {
                    displayName = contactCursor.getString(RawContactsQuery.DISPLAY_NAME);
                } else {
                    contactId = 0;
                }
                contactCursor.close();
            }

            if (contactId <= 0) {
                Cursor contactCursor = tryOpenContactsCursorByAddress(address);
                if (contactCursor != null) {
                    if (contactCursor.moveToFirst()) {
                        displayName = contactCursor.getString(ContactsQuery.DISPLAY_NAME);
                    }
                    contactCursor.close();
                }
            }

            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(displayName);
        }
        cursor.close();

        Intent clickIntent;
        if (unreadConversations == 1 && threadId > 0) {
            clickIntent = new Intent(Intent.ACTION_VIEW,
                    TelephonyProviderConstants.MmsSms.CONTENT_CONVERSATIONS_URI.buildUpon()
                            .appendPath(Long.toString(threadId)).build());
        } else {
            clickIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_MESSAGING);
        }
        
        //Display number if more than one convo, text of latest message if just one
        String body = "";
        if (unreadConversations > 1){
        	body = getResources().getQuantityString(
        		   R.plurals.sms_title_template, unreadConversations,
        		   unreadConversations);
        }
        if(unreadConversations == 1)
        	body = getMessageText();

        publishUpdate(new ExtensionData()
                .visible(unreadConversations > 0)
                .icon(R.drawable.ic_icon)
                .status(Integer.toString(unreadConversations))
                .expandedTitle(names.toString())
                .expandedBody(body)
                .clickIntent(clickIntent));
    	   
    }


    private Cursor openMmsSmsCursor() {
        return getContentResolver().query(
                TelephonyProviderConstants.MmsSms.CONTENT_CONVERSATIONS_URI,
                MmsSmsQuery.PROJECTION,
                TelephonyProviderConstants.Mms.READ + "=0 AND "
                        + TelephonyProviderConstants.Mms.THREAD_ID + "!=0 AND ("
                        + TelephonyProviderConstants.Mms.MESSAGE_BOX + "="
                        + TelephonyProviderConstants.Mms.MESSAGE_BOX_INBOX + " OR "
                        + TelephonyProviderConstants.Sms.TYPE + "="
                        + TelephonyProviderConstants.Sms.MESSAGE_TYPE_INBOX + ")",
                null,
                null);
    }

    private Cursor openMmsAddrCursor(long mmsMsgId) {
        return getContentResolver().query(
                TelephonyProviderConstants.Mms.CONTENT_URI.buildUpon()
                        .appendPath(Long.toString(mmsMsgId))
                        .appendPath("addr")
                        .build(),
                MmsAddrQuery.PROJECTION,
                TelephonyProviderConstants.Mms.Addr.MSG_ID + "=?",
                new String[]{Long.toString(mmsMsgId)},
                null);
    }

    private Cursor openContactsCursorById(long contactId) {
        return getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                        .appendPath(Long.toString(contactId))
                        .build(),
                RawContactsQuery.PROJECTION,
                null,
                null,
                null);
    }

    private Cursor tryOpenContactsCursorByAddress(String phoneNumber) {
        try {
            return getContentResolver().query(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                            .appendPath(Uri.encode(phoneNumber)).build(),
                    ContactsQuery.PROJECTION,
                    null,
                    null,
                    null);
        } catch (IllegalArgumentException e) {
            // Can be called by the content provider (from Google Play crash/ANR console)
            // java.lang.IllegalArgumentException: URI: content://com.android.contacts/phone_lookup/
//            LogUtils.LOGW(TAG, "Error looking up contact name", e);
//            Log.w(TAG, "Error looking up contact name", e);
            return null;
        }
    }

    private interface MmsSmsQuery {
        String[] PROJECTION = {
                TelephonyProviderConstants.Sms._ID,
                TelephonyProviderConstants.Sms.ADDRESS,
                TelephonyProviderConstants.Sms.PERSON,
                TelephonyProviderConstants.Sms.THREAD_ID,
        };

        int _ID = 0;
        int ADDRESS = 1;
        int PERSON = 2;
        int THREAD_ID = 3;
    }

    private interface MmsAddrQuery {
        String[] PROJECTION = {
                TelephonyProviderConstants.Mms.Addr.ADDRESS,
                TelephonyProviderConstants.Mms.Addr.CONTACT_ID,
        };

        int ADDRESS = 0;
        int CONTACT_ID = 1;
    }

    private interface RawContactsQuery {
        String[] PROJECTION = {
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
        };

        int DISPLAY_NAME = 0;
    }

    private interface ContactsQuery {
        String[] PROJECTION = {
                ContactsContract.Contacts.DISPLAY_NAME,
        };

        int DISPLAY_NAME = 0;
    }
    
    private String getMessageText(){
    	Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
    	cursor.moveToFirst();
    	//12 is where message text is stored
    	return cursor.getString(12);
    }
}
