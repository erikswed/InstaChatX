package com.appsrox.instachat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
	private AlertDialog disclaimer;
	ListView listView;
	private ActionBar actionBar;
	private ContactCursorAdapter ContactCursorAdapter;
	public static PhotoCache photoCache;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.contactslist);
		listView.setOnItemClickListener(this);
		ContactCursorAdapter = new ContactCursorAdapter(this, null);
		listView.setAdapter(ContactCursorAdapter);
		actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.show();		
		photoCache = new PhotoCache(this);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME, ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setTitle("You are");
	    actionBar.setSubtitle(Common.getPreferredEmail());
	    
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
//		
//		ArrayAdapter<CharSequence> dropdownAdapter = ArrayAdapter.createFromResource(this, R.array.dropdown_arr, android.R.layout.simple_list_item_1);
//		actionBar.setListNavigationCallbacks(dropdownAdapter, new ActionBar.OnNavigationListener() {
//			
//			@Override
//			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
//				getLoaderManager().restartLoader(0, getArgs(itemPosition), MainActivity.this);
//				return true;
//			}
//		});
		
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
    public boolean onCreateOptionsMenu (Menu menu) {
    	getMenuInflater().inflate(R.menu.main, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			AddContactDialog newFragment = AddContactDialog.newInstance();
			newFragment.show(getSupportFragmentManager(), "AddContactDialog");
			return true;
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;			
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
		Intent intent = new Intent(this, ChatActivity.class);
		intent.putExtra(Common.PROFILE_ID, String.valueOf(arg3));
		startActivity(intent);
	}
	
	@Override
	protected void onDestroy() {
		if (disclaimer != null) 
			disclaimer.dismiss();
		super.onDestroy();
	}	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = new CursorLoader(this, 
				DataProvider.CONTENT_URI_PROFILE, 
				new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_EMAIL, DataProvider.COL_COUNT}, 
				null, 
				null, 
				DataProvider.COL_ID + " DESC"); 
		return loader;
	}

	@Override
	public void onLoadFinished(android.support.v4.content.Loader<Cursor> arg0, Cursor arg1) {
		ContactCursorAdapter.swapCursor(arg1);
	}

	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
		ContactCursorAdapter.swapCursor(null);
	}
	
	public class ContactCursorAdapter extends CursorAdapter {

		private LayoutInflater mInflater;

		public ContactCursorAdapter(Context context, Cursor c) {
			super(context, c, 0);
			this.mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override public int getCount() {
			return getCursor() == null ? 0 : super.getCount();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View itemLayout = mInflater.inflate(R.layout.main_list_item, parent, false);
			ViewHolder holder = new ViewHolder();
			itemLayout.setTag(holder);
			holder.text1 = (TextView) itemLayout.findViewById(R.id.text1);
			holder.text2 = (TextView) itemLayout.findViewById(R.id.text2);
			holder.textEmail = (TextView) itemLayout.findViewById(R.id.textEmail);
			holder.avatar = (ImageView) itemLayout.findViewById(R.id.avatar);
			return itemLayout;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.text1.setText(cursor.getString(cursor.getColumnIndex(DataProvider.COL_NAME)));
			holder.textEmail.setText(cursor.getString(cursor.getColumnIndex(DataProvider.COL_EMAIL)));
			int count = cursor.getInt(cursor.getColumnIndex(DataProvider.COL_COUNT));
			if (count > 0){
				holder.text2.setVisibility(View.VISIBLE);
				holder.text2.setText(String.format("%d new message%s", count, count==1 ? "" : "s"));
			}else
				holder.text2.setVisibility(View.GONE);

			photoCache.DisplayBitmap(requestPhoto(cursor.getString(cursor.getColumnIndex(DataProvider.COL_EMAIL))), holder.avatar);

		}
	}

	private static class ViewHolder {
		TextView text1;
		TextView text2;
		TextView textEmail;
		ImageView avatar;
	}
	
	@SuppressLint("InlinedApi")
	private Uri requestPhoto(String email){
		Cursor emailCur = null;
		Uri uri = null;
		try{
			int SDK_INT = android.os.Build.VERSION.SDK_INT;
			if(SDK_INT >= 11){
				String[] projection = { ContactsContract.CommonDataKinds.Email.PHOTO_URI };
				ContentResolver cr = getContentResolver();
				emailCur = cr.query(
						ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection,
						ContactsContract.CommonDataKinds.Email.ADDRESS + " = ?", 
								new String[]{email}, null);
				if (emailCur != null && emailCur.getCount() > 0) {	
					if (emailCur.moveToNext()) {
						String photoUri = emailCur.getString( emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.PHOTO_URI));
						if(photoUri != null)
							uri = Uri.parse(photoUri);
					}
				}
			}else if(SDK_INT < 11) {
				String[] projection = { ContactsContract.CommonDataKinds.Photo.CONTACT_ID };
				ContentResolver cr = getContentResolver();
				emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
						projection,
						ContactsContract.CommonDataKinds.Email.ADDRESS + " = ?",
						new String[]{email}, null);
				if (emailCur.moveToNext()) {
					int columnIndex = emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Photo.CONTACT_ID);
					long contactId = emailCur.getLong(columnIndex);
					uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,	contactId);
					uri = Uri.withAppendedPath(uri,	ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
				}
			}	
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				if(emailCur != null)
					emailCur.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return uri;
	}
}
