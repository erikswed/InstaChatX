package com.appsrox.instachat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Chat fragment holding a single conversation.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class MessagesFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final DateFormat[] df = new DateFormat[] {
		DateFormat.getDateInstance(), DateFormat.getTimeInstance()};

	private OnFragmentInteractionListener mListener;
	private CursorAdapter chatCursorAdapter;
	private Date now;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		now = new Date();
		chatCursorAdapter = new ChatCursorAdapter(getActivity(), null);
		setListAdapter(chatCursorAdapter);
	}	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setDivider(null);
		getListView().setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		getListView().setStackFromBottom(true);		
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		getListView().setLayoutParams(params);
		Bundle args = new Bundle();
		args.putString(DataProvider.COL_EMAIL, mListener.getProfileEmail());
		getLoaderManager().initLoader(0, args, this);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface OnFragmentInteractionListener {
		public String getProfileEmail();
	}

	private String getDisplayTime(String datetime) {
		try {
			Date dt = sdf.parse(datetime);
			if (now.getYear()==dt.getYear() && now.getMonth()==dt.getMonth() && now.getDate()==dt.getDate()) {
				return df[1].format(dt);
			}
			return df[0].format(dt);
		} catch (ParseException e) {
			return datetime;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String profileEmail = args.getString(DataProvider.COL_EMAIL);
		CursorLoader loader = new CursorLoader(getActivity(), 
				DataProvider.CONTENT_URI_MESSAGES, 
				null, 
				DataProvider.COL_SENDER_EMAIL + " = ? or " + DataProvider.COL_RECEIVER_EMAIL + " = ?",
				new String[]{profileEmail, profileEmail}, 
				DataProvider.COL_TIME + " ASC"); 
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		chatCursorAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		chatCursorAdapter.swapCursor(null);
	}

	public class ChatCursorAdapter extends CursorAdapter {
	    
		public ChatCursorAdapter(Context context, Cursor c) {
			super(context, c, 0);
		}

		@Override public int getCount() {
			return getCursor() == null ? 0 : super.getCount();
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int _position) {
			Cursor cursor = (Cursor) getItem(_position);
			return getItemViewType(cursor);
		}

	    private int getItemViewType(Cursor _cursor) {
	        int typeIdx = _cursor.getColumnIndex(DataProvider.COL_TYPE);
	        int type = _cursor.getInt(typeIdx);
	        return type == 0 ? 0 : 1;
	    }
	    
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			ViewHolder holder = new ViewHolder();
			View itemLayout = null;
			switch(getItemViewType(cursor)){
		    case 0:
				itemLayout = LayoutInflater.from(context).inflate(R.layout.chat_list_item_left_aligned, parent, false);
		    	break;
		    case 1:
				itemLayout = LayoutInflater.from(context).inflate(R.layout.chat_list_item_right_aligned, parent, false);
		    	break;
			}
			itemLayout.setTag(holder);
			holder.avatar = (ImageView) itemLayout.findViewById(R.id.avatar);			
			holder.text1 = (TextView) itemLayout.findViewById(R.id.text1);
			holder.text2 = (TextView) itemLayout.findViewById(R.id.text2);
			return itemLayout;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			String email = cursor.getString(cursor.getColumnIndex(DataProvider.COL_SENDER_EMAIL));
			holder.text1.setText(getDisplayTime(cursor.getString(cursor.getColumnIndex(DataProvider.COL_TIME))));
			holder.text2.setText(cursor.getString(cursor.getColumnIndex(DataProvider.COL_MESSAGE)));
			MainActivity.photoCache.DisplayBitmap(requestPhoto(email), holder.avatar);
		}
	}

	private static class ViewHolder {
		TextView text1;
		TextView text2;
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
				ContentResolver cr = getActivity().getContentResolver();
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
				ContentResolver cr = getActivity().getContentResolver();
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
