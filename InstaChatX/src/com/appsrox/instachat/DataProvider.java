package com.appsrox.instachat;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class DataProvider extends ContentProvider {

	public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://com.appsrox.instachat.provider/messages");
	public static final Uri CONTENT_URI_PROFILE = Uri.parse("content://com.appsrox.instachat.provider/profile");

	public static final String COL_ID = "_id";

	public enum MessageType {

    	INCOMING, OUTGOING
    }
	
	//parameters recognized by demo server
	public static final String SENDER_EMAIL 		= "senderEmail";
	public static final String RECEIVER_EMAIL 		= "receiverEmail";	
	public static final String REG_ID 				= "regId";
	public static final String MESSAGE 				= "message";
	
	// TABLE MESSAGE
	public static final String TABLE_MESSAGES 		= "messages";
	public static final String COL_TYPE				= "type";
	public static final String COL_SENDER_EMAIL 	= "senderEmail";
	public static final String COL_RECEIVER_EMAIL 	= "receiverEmail";
	public static final String COL_MESSAGE 			= "message";
	public static final String COL_TIME 			= "time";

	// TABLE PROFILE
	public static final String TABLE_PROFILE = "profile";
	public static final String COL_NAME = "name";
	public static final String COL_EMAIL = "email";
	public static final String COL_COUNT = "count";

	private DbHelper dbHelper;

	private static final int MESSAGES_ALLROWS = 1;
	private static final int MESSAGES_SINGLE_ROW = 2;
	private static final int PROFILE_ALLROWS = 3;
	private static final int PROFILE_SINGLE_ROW = 4;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.appsrox.instachat.provider", "messages", MESSAGES_ALLROWS);
		uriMatcher.addURI("com.appsrox.instachat.provider", "messages/#", MESSAGES_SINGLE_ROW);
		uriMatcher.addURI("com.appsrox.instachat.provider", "profile", PROFILE_ALLROWS);
		uriMatcher.addURI("com.appsrox.instachat.provider", "profile/#", PROFILE_SINGLE_ROW);
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			qb.setTables(TABLE_MESSAGES);
			break;			

		case MESSAGES_SINGLE_ROW:
			qb.setTables(TABLE_MESSAGES);
			qb.appendWhere("_id = " + uri.getLastPathSegment());
			break;

		case PROFILE_ALLROWS:
			qb.setTables(TABLE_PROFILE);
			break;			

		case PROFILE_SINGLE_ROW:
			qb.setTables(TABLE_PROFILE);
			qb.appendWhere("_id = " + uri.getLastPathSegment());
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}

		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		long id;
		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			id = db.insertOrThrow(TABLE_MESSAGES, null, values);
			if (values.get(COL_RECEIVER_EMAIL) == null) {
				db.execSQL("update profile set count = count+1 where email = ?", new Object[]{values.get(COL_SENDER_EMAIL)});
				getContext().getContentResolver().notifyChange(CONTENT_URI_PROFILE, null);
			}
			break;

		case PROFILE_ALLROWS:
			id = db.insertOrThrow(TABLE_PROFILE, null, values);
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		Uri insertUri = ContentUris.withAppendedId(uri, id);
		getContext().getContentResolver().notifyChange(insertUri, null);
		return insertUri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int count;
		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			count = db.update(TABLE_MESSAGES, values, selection, selectionArgs);
			break;			

		case MESSAGES_SINGLE_ROW:
			count = db.update(TABLE_MESSAGES, values, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		case PROFILE_ALLROWS:
			count = db.update(TABLE_PROFILE, values, selection, selectionArgs);
			break;			

		case PROFILE_SINGLE_ROW:
			count = db.update(TABLE_PROFILE, values, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int count;
		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			count = db.delete(TABLE_MESSAGES, selection, selectionArgs);
			break;			

		case MESSAGES_SINGLE_ROW:
			count = db.delete(TABLE_MESSAGES, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		case PROFILE_ALLROWS:
			count = db.delete(TABLE_PROFILE, selection, selectionArgs);
			break;			

		case PROFILE_SINGLE_ROW:
			count = db.delete(TABLE_PROFILE, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}	


	private static class DbHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "instachat.db";
		private static final int DATABASE_VERSION = 1;
		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table messages ("
					+ "_id integer primary key autoincrement, "
					+ COL_TYPE        	  +" integer, "					
					+ COL_MESSAGE         +" text, "
					+ COL_SENDER_EMAIL 	  +" text, "
					+ COL_RECEIVER_EMAIL  +" text, "
					+ COL_TIME 			  +" datetime default current_timestamp);");

			db.execSQL("create table profile("
					+ "_id integer primary key autoincrement, "
					+ COL_NAME 	  +" text, "
					+ COL_EMAIL   +" text unique, "
					+ COL_COUNT   +" integer default 0);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}
}
