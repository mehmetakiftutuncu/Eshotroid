package com.mehmetakiftutuncu.eshotroid.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mehmetakiftutuncu.eshotroid.models.Bus;
import com.mehmetakiftutuncu.eshotroid.utilities.Log;
import com.mehmetakiftutuncu.eshotroid.utilities.option.None;
import com.mehmetakiftutuncu.eshotroid.utilities.option.Option;
import com.mehmetakiftutuncu.eshotroid.utilities.option.Some;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {
    public static final String TAG = "Database";

    public static final String DATABASE_NAME = "eshotroidplus";
    public static final int DATABASE_VERSION = 1;

    public class TableBus {
        public static final String TABLE_NAME = "bus";

        public static final String COLUMN_ID         = "id";
        public static final String COLUMN_DEPARTURE  = "departure";
        public static final String COLUMN_ARRIVAL    = "arrival";
        public static final String COLUMN_IS_STARRED = "isStarred";

        public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY, " +
            COLUMN_DEPARTURE + " TEXT NOT NULL, " +
            COLUMN_ARRIVAL + " TEXT NOT NULL, " +
            COLUMN_IS_STARRED + " INTEGER NOT NULL" +
        ");";
    }

    private Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static Database with(Context context) {
        return new Database(context);
    }

    public Option<ArrayList<Bus>> getBusList() {
        try {
            ArrayList<Bus> busList = new ArrayList<>();

            SQLiteDatabase database = getReadableDatabase();

            Cursor cursor = database.rawQuery("SELECT * FROM " + TableBus.TABLE_NAME, null);

            if (cursor != null && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    int id            = cursor.getInt(cursor.getColumnIndex(TableBus.COLUMN_ID));
                    String departure  = cursor.getString(cursor.getColumnIndex(TableBus.COLUMN_DEPARTURE));
                    String arrival    = cursor.getString(cursor.getColumnIndex(TableBus.COLUMN_ARRIVAL));
                    boolean isStarred = cursor.getInt(cursor.getColumnIndex(TableBus.COLUMN_IS_STARRED)) == 1;

                    Bus bus = new Bus(id, departure, arrival, isStarred);

                    busList.add(bus);

                    cursor.moveToNext();
                }

                cursor.close();
            }

            database.close();

            return new Some<>(busList);
        } catch (Throwable t) {
            Log.error(TAG, t, "Failed to get bus list from database!");

            return new None<>();
        }
    }

    public boolean saveBusList(ArrayList<Bus> busList) {
        try {
            String[] parameters = new String[busList.size() * 2];
            StringBuilder insertSQLBuilder = new StringBuilder("INSERT INTO ")
                .append(TableBus.TABLE_NAME)
                .append(" (")
                .append(TableBus.COLUMN_ID)
                .append(", ")
                .append(TableBus.COLUMN_DEPARTURE)
                .append(", ")
                .append(TableBus.COLUMN_ARRIVAL)
                .append(", ")
                .append(TableBus.COLUMN_IS_STARRED)
                .append(") VALUES ");

            for (int i = 0, size = busList.size(); i < size; i++) {
                Bus bus = busList.get(i);

                insertSQLBuilder.append("(").append(bus.id).append(", ?, ?, ").append(bus.isStarred ? 1 : 0).append(")");

                parameters[i * 2]       = bus.departure;
                parameters[(i * 2) + 1] = bus.arrival;

                if (i < size - 1) {
                    insertSQLBuilder.append(", ");
                }
            }

            SQLiteDatabase database = getWritableDatabase();

            boolean result = true;

            try {
                database.beginTransaction();
                database.execSQL("DELETE FROM " + TableBus.TABLE_NAME);
                database.execSQL(insertSQLBuilder.toString(), parameters);
                database.setTransactionSuccessful();
            } catch (Throwable t) {
                Log.error(TAG, t, "Failed to save bus list to database, transaction failed!");

                result = false;
            } finally {
                database.endTransaction();
            }

            database.close();

            return result;
        } catch (Throwable t) {
            Log.error(TAG, t, "Failed to save bus list to database!");

            return false;
        }
    }

    public boolean updateBus(Bus bus) {
        try {
            String[] parameters = new String[2];
            StringBuilder updateSQLBuilder = new StringBuilder("UPDATE ")
                    .append(TableBus.TABLE_NAME)
                    .append(" SET ")
                    .append(TableBus.COLUMN_DEPARTURE)
                    .append(" = ?, ")
                    .append(TableBus.COLUMN_ARRIVAL)
                    .append(" = ?, ")
                    .append(TableBus.COLUMN_IS_STARRED)
                    .append(" = ")
                    .append(bus.isStarred ? 1 : 0)
                    .append(" WHERE ")
                    .append(TableBus.COLUMN_ID)
                    .append(" = ")
                    .append(bus.id);

            parameters[0] = bus.departure;
            parameters[1] = bus.arrival;

            SQLiteDatabase database = getWritableDatabase();

            boolean result = true;

            try {
                database.execSQL(updateSQLBuilder.toString(), parameters);
            } catch (Throwable t) {
                Log.error(TAG, t, "Failed to update bus '%s' on database, query failed!", bus.toString());

                result = false;
            }

            database.close();

            return result;
        } catch (Throwable t) {
            Log.error(TAG, t, "Failed to update bus '%s' on database!", bus.toString());

            return false;
        }
    }

    @Override public void onCreate(SQLiteDatabase database) {
        database.execSQL(TableBus.CREATE_TABLE_SQL);
    }

    @Override public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.warn(TAG, "Upgrading database %s from version %d to %d!", DATABASE_NAME, oldVersion, newVersion);

        database.execSQL("DROP TABLE IF EXISTS " + TableBus.TABLE_NAME);

        onCreate(database);
    }
}
