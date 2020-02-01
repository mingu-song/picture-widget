package sa.devming.picturewidget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import sa.devming.picturewidget.database.PictureData;
import sa.devming.picturewidget.database.PictureDbHelper;

public class WidgetProvider extends AppWidgetProvider {
    private static final String TAG = WidgetProvider.class.getSimpleName();
    public final static String SHARE_CLICK = "sa.devming.picturewidget.SHARE_CLICK";
    public final static String BACK_CLICK = "sa.devming.picturewidget.BACK_CLICK";
    public final static int IMAGE_SIZE = 800;
    private PictureDbHelper mDBHelper;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds){
            if (mDBHelper == null) {
                mDBHelper = new PictureDbHelper(context);
            }
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        //DB가 존재하면 수행
        if (mDBHelper == null) {
            mDBHelper = new PictureDbHelper(context);
        }
        //지금 가져올 사진이 없다면 전체를 N으로 업데이트
        if (!mDBHelper.existLoadPictureData(appWidgetId)) {
            mDBHelper.updateAllPictureData(appWidgetId);
        }
        //이미지 업데이트
        PictureData pictureData = null;
        if (mDBHelper.existLoadPictureData(appWidgetId)) {
            pictureData = mDBHelper.getNextPictureData(appWidgetId);
            Uri uri = Uri.parse(pictureData.getImageUri());
            updateViews.setImageViewBitmap(R.id.widget_picture, WidgetProvider.decodeBitmapFromList(context, uri));
            mDBHelper.updateLoadPictureData(pictureData);
        }

        //클릭 이벤트
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, WidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.widget_picture, pendingIntent);

        //뒤로가기 이벤트
        Intent back = new Intent(WidgetProvider.BACK_CLICK, null, context, WidgetProvider.class);
        back.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntentB = PendingIntent.getBroadcast(context, appWidgetId, back, PendingIntent.FLAG_CANCEL_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.widget_back, pendingIntentB);

        //open folder 이벤트
        if (pictureData != null) {
            Intent open;
            String filePath = getPath(context, Uri.parse(pictureData.getImageUri()));
            //Log.e(TAG,"filePath = "+filePath);
            if (filePath != null) { //파일경로가 있다면 해당 폴더를 열어준다.
                Uri targetUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String targetDir = filePath.substring(0, filePath.lastIndexOf("/"));
                targetUri = targetUri.buildUpon().appendQueryParameter("bucketId", String.valueOf(targetDir.toLowerCase().hashCode())).build();
                open = new Intent(Intent.ACTION_VIEW, targetUri);
            } else { //파일 경로가 없다면 해당 이미지만 열어준다.
                open = new Intent(Intent.ACTION_VIEW);
                open.setDataAndType(Uri.parse(pictureData.getImageUri()), "image/*");
                open.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //must for reading data from directory*/
            }
            PendingIntent pendingIntentOpen = PendingIntent.getActivity(context, appWidgetId, open, PendingIntent.FLAG_CANCEL_CURRENT);
            updateViews.setOnClickPendingIntent(R.id.widget_open, pendingIntentOpen);
        }

        //share 이벤트
        /*PictureData pictureData = mDBHelper.getCurrPictureData(appWidgetId);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image*//*");
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(pictureData.getImageUri()));
        Intent share2 = Intent.createChooser(share, context.getString(R.string.share_text));
        PendingIntent pendingIntentS = PendingIntent.getActivity(context, appWidgetId, share2, PendingIntent.FLAG_CANCEL_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.widget_share, pendingIntentS);*/
        Intent share = new Intent(WidgetProvider.SHARE_CLICK, null, context, WidgetProvider.class);
        share.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntentS = PendingIntent.getBroadcast(context, appWidgetId, share, PendingIntent.FLAG_CANCEL_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.widget_share, pendingIntentS);

        //setting 이벤트
        Intent setting = new Intent(context, WidgetConfig.class);
        setting.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntentT = PendingIntent.getActivity(context, appWidgetId, setting, PendingIntent.FLAG_CANCEL_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.widget_setting, pendingIntentT);

        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //Toast.makeText(context, "action = " + action, Toast.LENGTH_LONG).show();

        if (SHARE_CLICK.equalsIgnoreCase(action)) {
            Bundle bundle = intent.getExtras();
            int widgetId = bundle.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
            if (mDBHelper == null) {
                mDBHelper = new PictureDbHelper(context);
            }
            PictureData pictureData = mDBHelper.getCurrPictureData(widgetId);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(pictureData.getImageUri()));
            Intent chooser = Intent.createChooser(share, context.getString(R.string.share_text));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(chooser);

        } else if (BACK_CLICK.equalsIgnoreCase(action)) {
            // Y로 처리된 마지막 사진을 N으로 업데이트를 두번
            Bundle bundle = intent.getExtras();
            int widgetId = bundle.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
            if (mDBHelper == null) {
                mDBHelper = new PictureDbHelper(context);
            }
            PictureData pictureData = mDBHelper.getCurrPictureData(widgetId);
            mDBHelper.updateUnloadPictureData(pictureData);
            pictureData = mDBHelper.getCurrPictureData(widgetId);
            if (pictureData != null) {
                mDBHelper.updateUnloadPictureData(pictureData);
            } else {
                //전체를 로드 Y 처리 후 마지막놈 다시 unload 처리
                mDBHelper.updateAllLoadPictureData(widgetId);
                pictureData = mDBHelper.getCurrPictureData(widgetId);
                mDBHelper.updateUnloadPictureData(pictureData);
            }

            // 위젯 업데이트
            Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, WidgetProvider.class);
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetId});
            context.sendBroadcast(update);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int i : appWidgetIds) {
            try {
                if (mDBHelper == null) {
                    mDBHelper = new PictureDbHelper(context);
                }
                mDBHelper.deletePictureData(i);
            } catch (Exception e) {
                Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    public static Bitmap decodeBitmapFromList(Context context, Uri uri) {
        Bitmap bitmap = null;
        InputStream input;
        try {
            input = context.getContentResolver().openInputStream(uri);

            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();

            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }
            //int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
            //double ratio = (originalSize > WidgetProvider.IMAGE_SIZE) ? (originalSize / WidgetProvider.IMAGE_SIZE) : 1.0;
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = calculateInSampleSize(onlyBoundsOptions); //getPowerOfTwoForSampleRatio(ratio);
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmapOptions.inJustDecodeBounds = false;
            input = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageRotate(context, uri, bitmap);
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    private static Bitmap imageRotate(Context context, Uri uri, Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getImageRotateN(context, uri, bitmap);
        } else {
            return getImageRotate(context, uri, bitmap);
        }
    }

    private static Bitmap getImageRotate(Context context, Uri uri, Bitmap bitmap) {
        int exifDegree = 0;
        String filePath = getPath(context, uri);
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) exifDegree = 90;
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) exifDegree = 180;
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) exifDegree = 270;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        if (exifDegree != 0) {
            //rotate
            bitmap = rotate(bitmap, exifDegree);
        }
        return bitmap;
    }

    private static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) { //제조사에서 정해놓은 internal 메모리 위치
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else { // 사용자에 의한 sdcard 는 접근 방법이 없나?
                    Log.e(TAG,"external document nut not primary, so find SD card path");
                    return getExternalStorageDirectories(context)[0] + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /* returns external storage paths (directory of external memory card) as array of Strings */
    private static String[] getExternalStorageDirectories(final Context context) {
        List<String> results = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above
            File[] externalDirs = ContextCompat.getExternalFilesDirs(context, null);
            for (File file : externalDirs) {
                String path = file.getPath().split("/Android")[0];

                boolean addPath = false;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addPath = Environment.isExternalStorageRemovable(file);
                } else {
                    addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
                }

                if(addPath){
                    results.add(path);
                }
            }
        }

        if(results.isEmpty()) { //Method 2 for all versions
            // better variation of: http://stackoverflow.com/a/40123073/5002496
            String output = "";
            try {
                final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold").redirectErrorStream(true).start();
                process.waitFor();
                final InputStream is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    output = output + new String(buffer);
                }
                is.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            if(!output.trim().isEmpty()) {
                String devicePoints[] = output.split("\n");
                for(String voldPoint: devicePoints) {
                    results.add(voldPoint.split(" ")[2]);
                }
            }
        }

        //Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                    Log.d(TAG, results.get(i) + " might not be extSDcard");
                    results.remove(i--);
                }
            }
        } else {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
                    Log.d(TAG, results.get(i)+" might not be extSDcard");
                    results.remove(i--);
                }
            }
        }

        String[] storageDirectories = new String[results.size()];
        for(int i=0; i<results.size(); ++i)
            storageDirectories[i] = results.get(i);

        return storageDirectories;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Bitmap getImageRotateN(Context context, Uri uri, Bitmap bitmap) {
        //need > compile "com.android.support:exifinterface:25.1.0"
        int exifDegree = 0;
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(in);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) exifDegree = 90;
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) exifDegree = 180;
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) exifDegree = 270;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
        if (exifDegree != 0) {
            //rotate
            bitmap = rotate(bitmap, exifDegree);
        }
        return bitmap;
    }

    private static Bitmap rotate(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, (float)bitmap.getWidth()/2, (float)bitmap.getHeight()/2);
        try {
            Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (bitmap != converted) {
                bitmap.recycle();
                bitmap = converted;
            }
        } catch (OutOfMemoryError e) {

        }
        return bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > WidgetProvider.IMAGE_SIZE || width > WidgetProvider.IMAGE_SIZE) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= WidgetProvider.IMAGE_SIZE
                    || (halfWidth / inSampleSize) >= WidgetProvider.IMAGE_SIZE) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
