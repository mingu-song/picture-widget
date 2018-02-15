package sa.devming.picturewidget.database;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.ArrayList;

public class PictureDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "picture_widget.db";

    public static class PictureDB {
        public static final String TABLE_NAME = "picture_widget";
        public static final String COLUMN_NAME_WIDGET_ID = "widget_id";
        public static final String COLUMN_NAME_POSITION = "position";
        public static final String COLUMN_NAME_IMG_URI = "img_uri";
        public static final String COLUMN_NAME_IS_LOAD = "is_load";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PictureDB.TABLE_NAME + " (" +
                    PictureDB.COLUMN_NAME_WIDGET_ID + INTEGER_TYPE + COMMA_SEP +
                    PictureDB.COLUMN_NAME_POSITION + INTEGER_TYPE + COMMA_SEP +
                    PictureDB.COLUMN_NAME_IMG_URI + TEXT_TYPE + COMMA_SEP +
                    PictureDB.COLUMN_NAME_IS_LOAD + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PictureDB.TABLE_NAME;

    public PictureDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // 하나의 이미지 저장
    public long addPictureData(PictureData pictureData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PictureDB.COLUMN_NAME_WIDGET_ID, pictureData.getWidgetId());
        values.put(PictureDB.COLUMN_NAME_POSITION, pictureData.getPosition());
        values.put(PictureDB.COLUMN_NAME_IMG_URI, pictureData.getImageUri());
        values.put(PictureDB.COLUMN_NAME_IS_LOAD, pictureData.getIsLoad());

        return db.insert(PictureDB.TABLE_NAME, null, values);
    }

    // 현재 로드된 이미지 조회
    public PictureData getCurrPictureData(int widgetId) {
        //load = Y 인 position 이 젤 큰거
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                PictureDB.TABLE_NAME,                      // The table to query
                new String[] {PictureDB.COLUMN_NAME_WIDGET_ID, PictureDB.COLUMN_NAME_POSITION, PictureDB.COLUMN_NAME_IMG_URI},       // The columns to return
                PictureDB.COLUMN_NAME_WIDGET_ID + " = ? AND " + PictureDB.COLUMN_NAME_IS_LOAD + " = ?",  // The columns for the WHERE clause
                new String[] {String.valueOf(widgetId), "Y"},   // The values for the WHERE clause
                null,                                      // group by
                null,                                      // having
                PictureDB.COLUMN_NAME_POSITION + " DESC",            // order by
                "1"                                       // limit
        );

        PictureData pictureData = null;
        if (cursor != null && cursor.moveToFirst()) {
            pictureData = new PictureData(
                    cursor.getInt(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_WIDGET_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_POSITION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_IMG_URI)));
        }
        return pictureData;
    }

    // 다음 로드될 이미지 조회
    public PictureData getNextPictureData(int widgetId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                PictureDB.TABLE_NAME,                      // The table to query
                new String[] {PictureDB.COLUMN_NAME_WIDGET_ID, PictureDB.COLUMN_NAME_POSITION, PictureDB.COLUMN_NAME_IMG_URI},       // The columns to return
                PictureDB.COLUMN_NAME_WIDGET_ID + " = ? AND " + PictureDB.COLUMN_NAME_IS_LOAD + " = ?",  // The columns for the WHERE clause
                new String[] {String.valueOf(widgetId), "N"},   // The values for the WHERE clause
                null,                                      // group by
                null,                                      // having
                PictureDB.COLUMN_NAME_POSITION + " ASC",            // order by
                "1"                                       // limit
        );

        PictureData pictureData = null;
        if (cursor != null && cursor.moveToFirst()) {
            pictureData = new PictureData(
                    cursor.getInt(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_WIDGET_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_POSITION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_IMG_URI)));
        }
        return pictureData;
    }

    // 로드 되었다는 처리(Y) 업데이트
    public int updateLoadPictureData(PictureData pictureData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PictureDB.COLUMN_NAME_IS_LOAD, "Y");

        return db.update(PictureDB.TABLE_NAME,
                values,
                PictureDB.COLUMN_NAME_WIDGET_ID+ " = ? AND " + PictureDB.COLUMN_NAME_POSITION + "= ?",
                new String[] {String.valueOf(pictureData.getWidgetId()), String.valueOf(pictureData.getPosition())}
        );
    }

    // 로드 되지 않았다는 처리(N) 업데이트
    public int updateUnloadPictureData(PictureData pictureData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PictureDB.COLUMN_NAME_IS_LOAD, "N");

        return db.update(PictureDB.TABLE_NAME,
                values,
                PictureDB.COLUMN_NAME_WIDGET_ID+ " = ? AND " + PictureDB.COLUMN_NAME_POSITION + "= ?",
                new String[] {String.valueOf(pictureData.getWidgetId()), String.valueOf(pictureData.getPosition())}
        );
    }

    // 모두 로드 되었다면 초기화 처리
    public int updateAllPictureData(int widgetId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PictureDB.COLUMN_NAME_IS_LOAD, "N");

        return db.update(PictureDB.TABLE_NAME,
                values,
                PictureDB.COLUMN_NAME_WIDGET_ID+ " = ?",
                new String[] {String.valueOf(widgetId)}
        );
    }

    // 모두 로드되지 않았다면 전체로드(Y) 처리
    public int updateAllLoadPictureData(int widgetId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PictureDB.COLUMN_NAME_IS_LOAD, "Y");

        return db.update(PictureDB.TABLE_NAME,
                values,
                PictureDB.COLUMN_NAME_WIDGET_ID+ " = ?",
                new String[] {String.valueOf(widgetId)}
        );
    }

    // 로드할 수 있는(N) 이미지가 존재하는지 존재
    public boolean existLoadPictureData(int widgetId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                PictureDB.TABLE_NAME,                      // The table to query
                new String[] {PictureDB.COLUMN_NAME_WIDGET_ID,
                        PictureDB.COLUMN_NAME_POSITION,
                        PictureDB.COLUMN_NAME_IMG_URI},       // The columns to return
                PictureDB.COLUMN_NAME_WIDGET_ID + " = ? AND " + PictureDB.COLUMN_NAME_IS_LOAD + "= ?",  // The columns for the WHERE clause
                new String[] {String.valueOf(widgetId), "N"},   // The values for the WHERE clause
                null,                                      // group by
                null,                                      // having
                null,                                      // order by
                null                                       // limit
        );
        if (cursor == null) {
            return false;
        } else {
            return cursor.getCount() > 0;
        }
    }

    // 모든 이미지 삭제
    public void deletePictureData(int widgetId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PictureDB.TABLE_NAME,
                PictureDB.COLUMN_NAME_WIDGET_ID + " = ?",
                new String[] {String.valueOf(widgetId)});
    }

    public int getAllCount(int widgetId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            //신규건
            String selectQuery = "SELECT * FROM " + PictureDB.TABLE_NAME;
            cursor = db.rawQuery(selectQuery, null);
        } else {
            //업데이트 (업데이트 ID 제외하고 조회)
            cursor = db.query(
                    PictureDB.TABLE_NAME,                      // The table to query
                    new String[] {PictureDB.COLUMN_NAME_IMG_URI},       // The columns to return
                    PictureDB.COLUMN_NAME_WIDGET_ID + " != ?",  // The columns for the WHERE clause
                    new String[] {String.valueOf(widgetId)},   // The values for the WHERE clause
                    null,                                      // group by
                    null,                                      // having
                    null,                                      // order by
                    null                                       // limit
            );
        }

        return (cursor == null)? 0 : cursor.getCount();
    }

    // widget id별 모든 이미지 로드
    public ArrayList<Uri> getAllPictureDate(int widgetId) {
        ArrayList<Uri> imageList = new ArrayList();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                PictureDB.TABLE_NAME,                      // The table to query
                new String[] {PictureDB.COLUMN_NAME_IMG_URI},       // The columns to return
                PictureDB.COLUMN_NAME_WIDGET_ID + " = ?",  // The columns for the WHERE clause
                new String[] {String.valueOf(widgetId)},   // The values for the WHERE clause
                null,                                      // group by
                null,                                      // having
                null,                                      // order by
                null                                       // limit
        );
        if (cursor.moveToFirst()) {
            do {
                imageList.add(Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_IMG_URI))));
            } while (cursor.moveToNext());
        }
        return imageList;
    }

    // db전체 이미지 로드
    public ArrayList<Uri> getAllDB() {
        ArrayList<Uri> imageList = new ArrayList();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                PictureDB.TABLE_NAME,                      // The table to query
                new String[] {PictureDB.COLUMN_NAME_IMG_URI},       // The columns to return
                null,  // The columns for the WHERE clause
                null,   // The values for the WHERE clause
                null,                                      // group by
                null,                                      // having
                null,                                      // order by
                null                                       // limit
        );
        if (cursor.moveToFirst()) {
            do {
                imageList.add(Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(PictureDB.COLUMN_NAME_IMG_URI))));
            } while (cursor.moveToNext());
        }
        return imageList;
    }
}
