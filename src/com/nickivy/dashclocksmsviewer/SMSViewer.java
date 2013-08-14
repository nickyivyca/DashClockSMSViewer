package com.nickivy.dashclocksmsviewer;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import com.nickivy.dashclocksmsviewer.R;

import android.database.Cursor;
import android.preference.PreferenceManager;
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
	private long messageID = 0; 
	
	public static final String PREF_SWITCH = "pref_switch";
	public static final String PREF_MULTI = "pref_multi";
	
	//Public variables for Panels 2, 3 to read from
	
	public static String panel2_title = ""; //title and contents refer to default where
	public static String panel3_title = ""; //sender is on top and contents on bottom
	
	public static String panel2_contents = "";
	public static String panel3_contents = "";
	
	public static long panel2_threadId = 0;
	public static long panel3_threadId = 0;
	
	public static String panel2_address = "";
	public static String panel3_address = "";
	
	public static boolean panel2_visible = false;
	public static boolean panel3_visible = false;
	
	public static int[] nummsg = new int[3];
	
	public static int unreadConversations = 0;
	

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
    	unreadConversations = 0;
        StringBuilder names = new StringBuilder();
        ArrayList<String> namelist = new ArrayList<String>();
        ArrayList<String> addrlist = new ArrayList<String>();
        ArrayList<Long> idlist = new ArrayList<Long>();
        Cursor cursor = openMmsSmsCursor();
        long threadId = 0;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean switcher = sp.getBoolean(PREF_SWITCH, false);
        boolean multi = sp.getBoolean(PREF_MULTI, false);
        int status = 0;

        while (cursor.moveToNext()) {
            ++unreadConversations;

            // Get display name. SMS's are easy; MMS's not so much.
            long id = cursor.getLong(MmsSmsQuery._ID);
            messageID = id;
            long contactId = cursor.getLong(MmsSmsQuery.PERSON);
            String address = cursor.getString(MmsSmsQuery.ADDRESS);
            threadId = cursor.getLong(MmsSmsQuery.THREAD_ID);
            idlist.add(threadId);
            

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
            addrlist.add(address);

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
            namelist.add(displayName);
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
        String title = "";
        if (!multi){
            if (unreadConversations > 1){
            	body = getResources().getQuantityString(
            		   R.plurals.sms_title_template, unreadConversations,
            		   unreadConversations);
            }
            if(unreadConversations == 1)
            	body = getMessageText(addrlist.get(0),0);
            title = names.toString();
            if (body == null)
            	body = getResources().getString(R.string.no_body_sub);
            status = unreadConversations;
        }else{
            if (unreadConversations > 3){
            	body = getResources().getQuantityString(
            		   R.plurals.sms_title_template, unreadConversations,
            		   unreadConversations);
            	title = names.toString();
            }
            if(unreadConversations > 0 && unreadConversations < 4){
            	title = namelist.get(0);
            	body = getMessageText(addrlist.get(0),0);
            	panel2_visible = false;
            	panel3_visible = false;
        		status = nummsg[0];
            	if(unreadConversations > 1 && multi){
            		panel2_title = namelist.get(1);
            		panel2_contents = getMessageText(addrlist.get(1),1);
            		panel2_threadId = idlist.get(1);
            		panel2_visible = true;
            		if (unreadConversations > 2){
                		panel3_title = namelist.get(2);
                		panel3_contents = getMessageText(addrlist.get(2),2);
                		panel3_threadId = idlist.get(2);
            			panel3_visible = true;
            		}
            	}
            }
            if (body == null)
            	body = getResources().getString(R.string.no_body_sub);
        	
        }

        publishUpdate(new ExtensionData()
                .visible(unreadConversations > 0)
                .icon(R.drawable.ic_icon)
                .status(Integer.toString(status))
                .expandedTitle(switcher? body : title)
                .expandedBody(switcher? title : body)
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
    
    public String getMessageText(long messageId){
    	//BIG thanks to ShortFuse!
        String[] selectColumns = new String[] { "body" };
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), 
            selectColumns, "_id=?", new String[] { String.valueOf(messageId) }, null);
        if (cursor == null)
            return null; //handle this outside with default text        
        if (!cursor.moveToFirst())
        {
            cursor.close();
            return null;
        }
        String body = cursor.getString(0); //we only passed one column so it'll always be 0;
        cursor.close();
        return body;
    }
    
    //Testing for grabbing text when more than one unread message
    //
    public String getMessageText(){
    	Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[] { "body" } , "read = 0", null, null);
    	if (cursor == null){
    		return null;
    	}
        if (!cursor.moveToFirst())
        {
            cursor.close();
            return null;
        }
        cursor.moveToLast();
        String body = "";
        int numproc = 0;
        while(numproc != cursor.getCount()){
        	if (numproc == 0)
        		body = body + cursor.getString(cursor.getColumnIndexOrThrow("body"));
        	else
            	body = body + "\n" + cursor.getString(cursor.getColumnIndexOrThrow("body"));
        	numproc++;
        	cursor.moveToPrevious();
        }
        cursor.close();
    	
    	return body;
    }

    //For when multiple contacts exist and you want to get messages from only one
    public String getMessageText(String addr, int num){
    	Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[] { "address","body", } , "read = 0", null, null);
    	if (cursor == null){
    		return null;
    	}
        if (!cursor.moveToFirst())
        {
            cursor.close();
            return null;
        }
        cursor.moveToLast();
        String body = "";
        int numproc = 0;
        nummsg[num] = 0;
        while(numproc != cursor.getCount()){
        	if(cursor.getString(cursor.getColumnIndexOrThrow("address")).equals(addr)){
        		if (body.equals(""))
        			body = body + cursor.getString(cursor.getColumnIndexOrThrow("body"));
        		else
        			body = body + "\n" + cursor.getString(cursor.getColumnIndexOrThrow("body"));
            	nummsg[num]++;
        	}
    		numproc++;
    		cursor.moveToPrevious();
        }
        cursor.close();
    	
    	return body;
    }
}
