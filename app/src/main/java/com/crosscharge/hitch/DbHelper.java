package com.crosscharge.hitch;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by YASH on 02/04/2016.
 *
 * db handler
 *
 */

public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = DbHelper.class.getSimpleName();

    private Context context;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "HitchTag.db";

    public static final String TABLE_HITCH = Constants.DB.TABLE_HITCH;

    public static final String HITCH_UUID = Constants.DB.HITCH_UUID;
    public static final String HITCH_NAME = Constants.DB.HITCH_NAME;
    public static final String HITCH_TYPE = Constants.DB.HITCH_TYPE;
    public static final String HITCH_THEME_COLOR = "color";
    public DbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String DATABASE_CREATE = "create table " + Constants.DB.TABLE_HITCH + " ("
                + HITCH_UUID + " text primary key not null, "
                + HITCH_NAME + " text not null, "+ HITCH_THEME_COLOR + " text not null, "
                + HITCH_TYPE + " text not null );";
        db.execSQL(DATABASE_CREATE);
        Log.d(TAG, "Creating Database...");
    }

    /*
    If you have any idea abt sqlite, please correct the onUpgrade method.
    I don't think this is the correct way.
     */

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Updating Database...");

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HITCH);
        onCreate(db);
    }

    public void addNewHitchTag(BluetoothDevice device){
        // adds new HitchTag object to database.

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(HITCH_UUID, device.getAddress());
        contentValues.put(HITCH_NAME, device.getName());
        contentValues.put(HITCH_TYPE, Constants.TYPE_GENERAL);
        contentValues.put(HITCH_THEME_COLOR,"0");
        db.insert(TABLE_HITCH, null, contentValues);
        db.close();

        Log.i("x", "Inserted HitchTag " + device.getName() + ", " + device.getAddress());
    }
    public void removeHitchTag(HitchTag tag){
        // Removes hitch tag

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
                "DELETE FROM " + TABLE_HITCH +

                        " WHERE " + HITCH_UUID + " = '" + tag.getAddress() + "'");


    }
    public void updateHitchThemeColor(HitchTag tag, int color){
        // Updates HitchTag Theme color

       SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
                "UPDATE " + TABLE_HITCH +
                        " SET " + HITCH_THEME_COLOR + " =" + color +
                        " WHERE " + HITCH_UUID + " = '" + tag.getAddress() + "'");


    }

    public void updateHitchTagType(HitchTag tag, String type){
        // Updates HitchTag type

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
                "UPDATE " + TABLE_HITCH +
                        " SET " + HITCH_TYPE + " = '" + type +
                        "' WHERE " + HITCH_UUID + " = '" + tag.getAddress() + "'");
    }
    public void updateHitchTagName(HitchTag tag, String name){
        // Updates HitchTag Name

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
                "UPDATE " + TABLE_HITCH +
                        " SET " + HITCH_NAME + " = '" + name +
                        "' WHERE " + HITCH_UUID + " = '" + tag.getAddress() + "'");
    }

    public ArrayList<HitchTag> getHitchTagList(){
        // returns list of all HitchTags

        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM "+ TABLE_HITCH;
        ArrayList<HitchTag> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HitchTag tag = new HitchTag(context, cursor.getString(1), cursor.getString(0));
                list.add(tag);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public ArrayList<String> getHitchTagAddresses(){
        // Returns string list of all HitchTags

        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM "+ TABLE_HITCH;
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
    public ArrayList<String> getHitchTagThemeColors(){
        // Returns string list of all Tag Colors

        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM "+ TABLE_HITCH;
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Log.d("Count:",cursor.getColumnCount()+"");
                Log.d("Count:",cursor.getColumnName(2)+"");
                list.add(cursor.getString(2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
    public int getHitchTagCount() {
        String countQuery = "SELECT  * FROM " + TABLE_HITCH;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery,null);
        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }
}
