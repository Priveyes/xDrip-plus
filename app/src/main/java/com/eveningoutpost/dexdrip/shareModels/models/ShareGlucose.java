package com.eveningoutpost.dexdrip.shareModels.models;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.BaseColumns;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.google.gson.annotations.Expose;

/**
 * Created by Emma Black on 3/16/15.
 */
@Table(name = "ShareGlucose", id = BaseColumns._ID)
public class ShareGlucose extends Model {
	public Context mContext;
	@Expose
	@Column(name = "DT")
	public String DT;

	@Expose
	@Column(name = "ST")
	public String ST;

	@Expose
	@Column(name = "Trend")
	public double Trend;

	@Expose
	@Column(name = "Value")
	public double Value;

	@Expose
	@Column(name = "WT")
	public String WT;

	public void processShareData(Context context) {
		Log.d("SHARE", "Share Data being processed!"); // TODO maybe set this up??
//        mContext = context;
//        UserError.Log.i("SHARE", "Timestamp before parsing: " + WT);
//        UserError.Log.i("SHARE", "Timestamp before parsing: " + WT.replaceAll("[^\\d.]", ""));
//
//        double timestamp = (Double.parseDouble(WT.replaceAll("[^\\d.]", "")));
//        UserError.Log.i("SHARE", "Timestamp: " + timestamp);
//        if (!Bg.alreadyExists(timestamp)) {
//            UserError.Log.i("SHARE", "Data looks new!!");
//            Bg bg = new Bg();
//            bg.direction = slopeDirection();
//            bg.battery = Integer.toString(getBatteryLevel());
//            bg.bgdelta = calculateDelta(timestamp, Value);
//            bg.datetime = timestamp;
//            bg.sgv = Integer.toString((int) Value);
//            bg.save();
//            DataCollectionService.newDataArrived(mContext, true);
//            UserError.Log.i("SHARE", "Share Data Processed Successfully!");
//        } else {
//            UserError.Log.i("SHARE", "A Bg Value similar to this timestamp already exists.");
//        }
	}

	public String slopeDirection() {
		switch((int) Trend) {
			case 1:
				return "DoubleUp";
			case 2:
				return "SingleUp";
			case 3:
				return "FortyFiveUp";
			case 4:
				return "Flat";
			case 5:
				return "FortyFiveDown";
			case 6:
				return "SingleDown";
			case 7:
				return "DoubleDown";
			default:
				return "";
		}
	}


	public int getBatteryLevel() {
		Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		if(level == -1 || scale == -1) {
			return 50;
		}
		return (int)(((float)level / (float)scale) * 100.0f);
	}

	public double calculateDelta(double timestamp, double currentValue) {
//        Bg bg = Bg.mostRecentBefore(timestamp);
//        if (bg != null && Math.abs(bg.datetime - timestamp) < (60*1000*15)) {
//            return (bg.sgv_double() - currentValue);
//        } else {
		return 0;
//        }
	}
}
