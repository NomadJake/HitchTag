package com.crosscharge.hitch;

import java.util.UUID;

/**
 * Created by YASH on 16/03/2016.
 *
 * All the constants you need
 */
public class Constants {

    // BLE Constants
    public interface BLE {
        //vaze scan period changed to 5 sec
        int SCAN_PERIOD = 6000;
        int REQUEST_ENABLE_BT = 64;

        int COUNT_RANGE = 3;
        int COUNT_RSSI = 3;
    }

    public interface UUIDS{
        UUID IMMEDIATE_ALERT = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
        UUID LINK_LOSS = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");

        UUID ALERT_LEVEL = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

        UUID BUTTON_2 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

        UUID BUTTON_2_CUSTOM_CHAR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
        UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    }

    static int ALERT_HIGH = 2;
    static int ALERT_MILD = 1;
    static int ALERT_LOW = 0;


    public static String serviceName(UUID uuid){
        if(uuid == UUIDS.IMMEDIATE_ALERT){
            return "Immediate Alert Service";
        }
        else if(uuid == UUIDS.LINK_LOSS){
            return "Link Loss Service";
        }
        else if(uuid == UUIDS.BUTTON_2)
        {
            return "Button 2";
        }
        return "Unknown Service";
    }

    // alarm thread
    static int ALARM_COUNT = 5;

    // track thread
    static int TRACK_COUNT = 5;


    // Database constants
    public interface DB {
        String TABLE_HITCH = "HitchTag";
        String HITCH_THEME_COLOR="color";
        String HITCH_UUID = "uuid";
        String HITCH_NAME = "name";
        String HITCH_TYPE = "type";
    }

    static String TYPE_GENERAL = "general";
    static String TYPE_PET = "pet";
    static String TYPE_KID = "kid";

    public interface IMAGE{
        int CAMERA = 51;
        int GALLERY = 52;
    }

    // Message Handler
    public interface STRINGMESSAGE{
        int TOAST = 60; // toast
        int ALARM = 61; // ??
    }
    public interface INTMESSAGE {
        int FIND = 65; // find;
        int TRACK = 66; // track;
        int TRAIN = 67; // train;
    }

    public interface NOTIFICATION {
        int ALARM_SERVICE = 101;

        String MAIN_ACTION = "com.crosscharge.hitch.action.main";
        String PREV_ACTION = "com.crosscharge.hitch.action.prev";
        String PLAY_ACTION = "com.crosscharge.hitch.action.play";
        String NEXT_ACTION = "com.crosscharge.hitch.action.next";
        String STARTFOREGROUND_ACTION = "com.crosscharge.hitch.action.startforeground";
        String STOPFOREGROUND_ACTION = "com.crosscharge.hitch.action.stopforeground";
    }

    public static final boolean D = true;
    public static String PREFS_NAME="HitchTagPrefs";
}
