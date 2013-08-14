package com.nickivy.dashclocksmsviewer;

import android.database.Cursor;
import android.net.Uri;

public class SMSProcessor {

	public SMSProcessor(){
		
	}
	
/*    public String getMessageText(String addr){
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
        	if(cursor.getString(cursor.getColumnIndex("address")) == addr){
        		if (numproc == 0)
        			body = body + cursor.getString(cursor.getColumnIndexOrThrow("body"));
        		else
        			body = body + "\n" + cursor.getString(cursor.getColumnIndexOrThrow("body"));
        		numproc++;
        		cursor.moveToPrevious();
        	}
        }
        cursor.close();
    	
    	return body;
    }*/
}
