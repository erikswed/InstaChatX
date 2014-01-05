package com.appsrox.instachat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
/**
 * Keeps contact photos cached in memory.
 * Can also cache to disk (not in use because
 * contact images changed during sync)
 *
 */
public class PhotoCache {

	private static final String TAG = PhotoCache.class.getName();

	CacheMemory cacheMemory = new CacheMemory();

	//FileCache fileCache; // TODO not used

	private Map<ImageView, Uri> imageViewsMap = Collections.synchronizedMap(new WeakHashMap<ImageView, Uri>());

	ExecutorService executorService;

	private Context context; 

	final int stub_id = R.drawable.ic_contact_picture;

	public PhotoCache(Context context){
		//fileCache = new FileCache(context);
		executorService = Executors.newFixedThreadPool(5);
		this.context = context;
	}

	/**
	 * Request Bitmap to be loaded in background
	 * 
	 * @param uri
	 * @param imageView
	 */
	public void DisplayBitmap(Uri uri, ImageView imageView){
		// can be null if SyncManager fails 
		if(uri == null){
			imageView.setImageResource(stub_id);
			return;
		}
		// Store it
		imageViewsMap.put(imageView, uri);
		Bitmap bitmap = cacheMemory.get(uri);
		if(bitmap != null)
			imageView.setImageBitmap(bitmap);
		else{
			PhotoStub p = new PhotoStub(uri, imageView);
			executorService.submit(new PhotoStubLoader(p));			
			imageView.setImageResource(stub_id);
		}
	}

	/**
	 * Task for the queue
	 *
	 */
	private class PhotoStub {
		public Uri url;
		public ImageView imageView;

		public PhotoStub(Uri u, ImageView i){
			url=u; 
			imageView = i;
		}
	}

	/**
	 * Task for the queue
	 *
	 */
	class PhotoStubLoader implements Runnable {
		PhotoStub photoToLoad;
		PhotoStubLoader(PhotoStub photoToLoad){
			this.photoToLoad = photoToLoad;
		}

		@Override
		public void run() {
			if(imageViewReused(photoToLoad))
				return;
			Bitmap bmp = getBitmap(photoToLoad.url);
			cacheMemory.put(photoToLoad.url, bmp);
			if(imageViewReused(photoToLoad))
				return;
			BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
			((Activity)context).runOnUiThread(bd);
		}
	}

	/**
	 * Create Bitmap
	 * 
	 * @param uri
	 * @return
	 */
	private Bitmap getBitmap(Uri uri) {
		// check cache 
		//File f = fileCache.getFile(uri); // TODO not used
		// try and use the bitmap if it existed 
		//		Bitmap bitmap = decodeFile(f);
		//		if(bitmap != null)
		//			return bitmap;
		//from content resolver recourse 
		Bitmap b;
		try {
			BufferedInputStream bufferedInputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
			b = decodeRecourse(bufferedInputStream);
		} catch (Exception e) {
			b = null;
		}
		if(b != null)
			return b;
		return null;
		//from web
		//        try {
		//            Bitmap bitmap = null;
		//            URL imageUrl = new URL(url);
		//            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
		//            conn.setConnectTimeout(30000);
		//            conn.setReadTimeout(30000);
		//            conn.setInstanceFollowRedirects(true);
		//            InputStream is = conn.getInputStream();
		//            OutputStream os = new FileOutputStream(f);
		//            Utils.CopyStream(is, os);
		//            os.close();
		//            bitmap = decodeFile(f);
		//            return bitmap;
		//        } catch (Throwable ex){
		//           ex.printStackTrace();
		//           if(ex instanceof OutOfMemoryError)
		//               cacheMemory.clear();
		//           return null;
		//        }
	}

	/**
	 * Decodes File and scales
	 * 
	 * @param f
	 * @return
	 */
	//	private Bitmap decodeFile(File f) {
	//		try {
	//			//decode image size
	//			BitmapFactory.Options o = new BitmapFactory.Options();
	//			o.inJustDecodeBounds = true;
	//			BitmapFactory.decodeStream(new FileInputStream(f), null, o);
	//
	//			//Find the correct scale value. It should be the power of 2.
	//			final int REQUIRED_SIZE = 70;
	//			int width_tmp = o.outWidth, height_tmp = o.outHeight;
	//			int scale = 1;
	//			while(true){
	//				if(width_tmp /2 < REQUIRED_SIZE || height_tmp /2 < REQUIRED_SIZE)
	//					break;
	//				width_tmp /= 2;
	//				height_tmp /= 2;
	//				scale *= 2;
	//			}
	//
	//			//decode with inSampleSize
	//			BitmapFactory.Options o2 = new BitmapFactory.Options();
	//			o2.inSampleSize = scale;
	//			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
	//		} catch (FileNotFoundException e) {
	//
	//		}
	//		return null;
	//	}

	/**
	 * Decodes inputStream and scales
	 * 
	 * @param inputStream
	 * @return
	 */
	private Bitmap decodeRecourse(InputStream inputStream) {
		try {
			//decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(inputStream, null, o);
			//Find the correct scale value. It should be the power of 2.
			final int REQUIRED_SIZE = 70;
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while(true){
				if(width_tmp /2 < REQUIRED_SIZE || height_tmp /2 < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}
			inputStream.reset(); 
			//decode with inSampleSize
			o.inJustDecodeBounds = false;
			o.inSampleSize = scale;
			return BitmapFactory.decodeStream(inputStream, null, o);
		} catch (Exception e) {
			Log.w(TAG, "decodeRecourse failed"+ e.getMessage());
		}finally{
			if(inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return null;
	}

	/**
	 * If getView has recycled viewHolder 
	 * @param photoToLoad
	 * @return
	 */
	boolean imageViewReused(PhotoStub photoToLoad) {
		Uri tag = imageViewsMap.get(photoToLoad.imageView);
		if(tag == null || !tag.equals(photoToLoad.url)){
			Log.w(TAG, "ViewHolder is reused no need to load photo");
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Display bitmap in the UI thread
	 *
	 */
	class BitmapDisplayer implements Runnable {
		Bitmap bitmap;
		PhotoStub photoToLoad;

		public BitmapDisplayer(Bitmap b, PhotoStub p){
			bitmap = b;
			photoToLoad = p;
		}
		public void run(){
			if(imageViewReused(photoToLoad))
				return;
			if(bitmap != null)
				photoToLoad.imageView.setImageBitmap(bitmap);
			else
				photoToLoad.imageView.setImageResource(stub_id);
		}
	}

	/**
	 * Clear on demand
	 */
	public void clearCache() {
		cacheMemory.clear();
		//fileCache.clear();
	}

	class CacheMemory {
		private static final String TAG = "CacheMemory";
		//Last argument true for LRU ordering
		private Map<Uri, Bitmap> cache = Collections.synchronizedMap( new LinkedHashMap<Uri, Bitmap>(10, 1.5f, true));
		private long size = 0;//current allocated size
		private long limit = 1000000;//max memory in bytes TODO: run test on what's the best value

		public CacheMemory(){
			//use 25% of available heap size
			setLimit(Runtime.getRuntime().maxMemory()/4);
		}

		public void setLimit(long new_limit){
			limit = new_limit;
			Log.i(TAG, "CacheMemory will use up to " + limit/1024./1024. + "MB");
		}

		public Bitmap get(Uri id){
			try{
				if(!cache.containsKey(id))
					return null;
				//NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78
				//MyLog.w(TAG, "Found CacheMemory bitmap for "+ id);
				return cache.get(id);
			}catch(NullPointerException ex){
				ex.printStackTrace();
				return null;
			}
		}

		public void put(Uri id, Bitmap bitmap){
			try{
				if(cache.containsKey(id))
					size -= getSizeInBytes(cache.get(id));
				cache.put(id, bitmap);
				size += getSizeInBytes(bitmap);
				checkSize();
			}catch(Throwable th){
				th.printStackTrace();
			}
		}

		private void checkSize() {
			//MyLog.w(TAG, "cache size="+ size +" length= "+ cache.size());
			if(size > limit){
				//least recently accessed item will be the first one iterated 
				Iterator<Entry<Uri, Bitmap>> iter = cache.entrySet().iterator(); 
				while(iter.hasNext()){
					Entry<Uri, Bitmap> entry = iter.next();
					size -= getSizeInBytes(entry.getValue());
					iter.remove();
					if(size <= limit)
						break;
				}
				//MyLog.i(TAG, "Clean cache. New size "+ cache.size());
			}
		}

		public void clear() {
			try{
				//NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78 
				cache.clear();
				size = 0;
			}catch(NullPointerException ex){
				ex.printStackTrace();
			}
		}

		long getSizeInBytes(Bitmap bitmap) {
			if(bitmap == null)
				return 0;
			return bitmap.getRowBytes() * bitmap.getHeight();
		}
	}

	//	/**
	//	 * Persistent file cache
	//	 *
	//	 */
	//	class FileCache {
	//		private static final String IMAGES_CACHE = "Images_cache";
	//		private File cacheDir;
	//		
	//		public FileCache(Context context){
	//			//Find the dir to save cached images
	//			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
	//				cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), IMAGES_CACHE);
	//			else
	//				cacheDir = context.getCacheDir();
	//			if(!cacheDir.exists())
	//				cacheDir.mkdirs();
	//		}
	//
	//		public File getFile(Uri url){
	//			//I identify images by hashcode. Not a perfect solution, good for the demo.
	//			String filename = String.valueOf(url.hashCode());
	//			//Another possible solution (thanks to grantland)
	//			//String filename = URLEncoder.encode(url);
	//			File f = new File(cacheDir, filename);
	//			MyLog.w(TAG, "Found FileCache file "+ filename +" for "+ url);
	//			return f;
	//		}
	//
	//		public void clear(){
	//			File[] files = cacheDir.listFiles();
	//			if(files == null)
	//				return;
	//			for(File f : files)
	//				f.delete();
	//		}
	//	}
	

}