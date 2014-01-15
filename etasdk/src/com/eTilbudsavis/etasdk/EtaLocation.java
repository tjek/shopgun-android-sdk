/**
 * @fileoverview	Location.
 * @author			Danny Hvam <danny@eTilbudsavis.dk>
 */
package com.eTilbudsavis.etasdk;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.eTilbudsavis.etasdk.EtaObjects.Store;
import com.eTilbudsavis.etasdk.NetworkInterface.Request;

public class EtaLocation extends Location {

	public static final String TAG = "EtaLocation";
	
	private static final String ETA_PROVIDER	= "etasdk";
	
	private static final int RADIUS_MIN = 0;
	private static final int RADIUS_MAX = 700000;
	private static final double BOUND_DEFAULT = 0.0;
	
	// Location.
	private static int mRadius = RADIUS_MAX;
	private static boolean mSensor = false;
	private static String mAddress = null;
	private static double mBoundNorth = BOUND_DEFAULT;
	private static double mBoundEast = BOUND_DEFAULT;
	private static double mBoundSouth = BOUND_DEFAULT;
	private static double mBoundWest = BOUND_DEFAULT;
	private static Eta mEta;
	private static boolean mPushNotifications = false;
	private static ArrayList<LocationListener> mSubscribers = new ArrayList<LocationListener>();
	
	public EtaLocation(Eta eta) {
		super(ETA_PROVIDER);
		mEta = eta;
		restoreState();
	}

	@Override
	public void set(Location l) {
		super.set(l);
		mSensor = (getProvider().equals(LocationManager.GPS_PROVIDER) || getProvider().equals(LocationManager.NETWORK_PROVIDER) );
		mPushNotifications = true;
		save();
	}
	
	/**
	 * Set location for an address that has been geocoded to a latitude, longitude format<br /><br />
	 * NOTE: This implicitly implies that, no {@link #setSensor(boolean) sensor} has been used.
	 * https://developers.google.com/maps/documentation/geocoding/ for more info
	 * 
	 * @param address that has been geocoded
	 * @param latitude of the address
	 * @param longitude of the address
	 * @return this object
	 */
	public EtaLocation set(String address, double latitude, double longitude) {
		mAddress = address;
		setLatitude(latitude);
		setLongitude(longitude);
		setSensor(false);
		save();
		return this;
	}
	
	/**
	 * Set the current search radius.
	 * @param radius in meters <li> Min value = 0 <li> Max value = 700000
	 * @return this Object, for easy chaining of set methods.
	 */
	public EtaLocation setRadius(int radius) {
		mRadius =  radius < RADIUS_MIN ? RADIUS_MIN : ( radius > RADIUS_MAX ? RADIUS_MAX : radius );
		save();
		return this;
	}

	/**
	 * Get current radius
	 * @return radius in meters.
	 */
	public int getRadius() {
		return mRadius;
	}

	public EtaLocation setSensor(boolean sensor) {
		mSensor = sensor;
		save();
		return this;
	}
	
	public boolean isSensor() {
		return mSensor;
	}

	/**
	 * Set an postal address of a location.<br /><br />
	 * <b>NOTICE</b> The address is purely a convenience for the developers.<br />
	 * The SDK does NOT USE the data for anything, only latitude, longitude, radius and sensor are used, and hence they must be set.
	 * @param address
	 * @return This EtaLocation object
	 */
	public EtaLocation setAddress(String address) {
		mAddress = address;
		save();
		return this;
	}
	
	
	public String getAddress() {
		return mAddress;
	}

	public boolean isSet() {
		return isValidLocation(getLatitude(), getLongitude());
	}
	
	public static boolean isValidLocation(double lat, double lng) {
		return isValid(lat) && isValid(lng);
	}

	/*
	 * A coordinate is valid is it's not in the range of -0.1 to 0.1
	 */
	private static boolean isValid(double coordinate) {
		return !(-0.1 < coordinate && coordinate < 0.1);
	}
	
	public boolean isBoundsSet() {
		return (mBoundNorth != BOUND_DEFAULT && 
				mBoundSouth != BOUND_DEFAULT && 
				mBoundEast != BOUND_DEFAULT && 
				mBoundWest != BOUND_DEFAULT);
	}

	/**
	 * Returns a JSONObject, with mapped values for, what is needed for an API request:
	 * <li>Latitude
	 * <li>Longitude
	 * <li>Sensor
	 * <li>Radius
	 * @return The mapped JSONObject
	 */
	public JSONObject toJSON() {
		JSONObject o = new JSONObject();
		try {
			o.put(Request.Param.LATITUDE, getLatitude());
			o.put(Request.Param.LONGITUDE, getLongitude());
			o.put(Request.Param.SENSOR, isSensor());
			o.put(Request.Param.RADIUS, getRadius());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return o;
	}

	public int distanceToStore(Store store) {
		Location tmp = new Location(EtaLocation.ETA_PROVIDER);
		tmp.setLatitude(store.getLatitude());
		tmp.setLongitude(store.getLongitude());
		double dist = distanceTo(tmp);
		return (int)dist;
	}

	/**
	 * Set the bounds for your search.
	 * All parameters should be GPS coordinates.
	 * @param boundEast
	 * @param boundWest
	 * @param boundEast
	 * @param boundEast
	 */
	public void setBounds(double boundNorth, double boundEast, double boundSouth, double boundWest) {
		mBoundEast = boundEast;
		mBoundNorth = boundNorth;
		mBoundSouth = boundSouth;
		mBoundWest = boundWest;
		save();
	}

	public double getBoundEast() {
		return mBoundEast;
	}
	
	public double getBoundNorth() {
		return mBoundNorth;
	}
	
	public double getBoundSouth() {
		return mBoundSouth;
	}
	
	public double getBoundWest() {
		return mBoundWest;
	}
	
	public Bundle getQuery() {
		
		Bundle b = new Bundle();

		b.putDouble(Request.Param.LATITUDE, getLatitude());
		b.putDouble(Request.Param.LONGITUDE, getLongitude());
		b.putBoolean(Request.Param.SENSOR, isSensor());
		b.putInt(Request.Param.RADIUS, getRadius());

		// Determine whether to include bounds.
		if (isBoundsSet()) {
			b.putDouble(Request.Param.BOUND_EAST, getBoundEast());
			b.putDouble(Request.Param.BOUND_NORTH, getBoundNorth());
			b.putDouble(Request.Param.BOUND_SOUTH, getBoundSouth());
			b.putDouble(Request.Param.BOUND_WEST, getBoundWest());
		}
		return b;
//		List<NameValuePair> query = new ArrayList<NameValuePair>();
//		
//		query.add(Utils.getNameValuePair(LATITUDE, getLatitude()));
//		query.add(Utils.getNameValuePair(LONGITUDE, getLongitude()));
//		query.add(Utils.getNameValuePair(SENSOR, isSensor()));
//		query.add(Utils.getNameValuePair(RADIUS, getRadius()));
//
//		// Determine whether to include bounds.
//		if (isBoundsSet()) {
//			query.add(Utils.getNameValuePair(BOUND_EAST, getBoundEast()));
//			query.add(Utils.getNameValuePair(BOUND_NORTH, getBoundNorth()));
//			query.add(Utils.getNameValuePair(BOUND_SOUTH, getBoundSouth()));
//			query.add(Utils.getNameValuePair(BOUND_WEST, getBoundWest()));
//		}
//		return query;
	}


	public void saveState() {
		SharedPreferences.Editor editor = mEta.getSettings().getPrefs().edit();
    	editor
		.putBoolean(Settings.LOC_SENSOR, mSensor)
		.putInt(Settings.LOC_RADIUS, mRadius)
		.putFloat(Settings.LOC_LATITUDE, (float)getLatitude())
		.putFloat(Settings.LOC_LONGITUDE, (float)getLongitude())
		.putFloat(Settings.LOC_BOUND_EAST, (float)mBoundEast)
		.putFloat(Settings.LOC_BOUND_WEST, (float)mBoundWest)
		.putFloat(Settings.LOC_BOUND_NORTH, (float)mBoundNorth)
		.putFloat(Settings.LOC_BOUND_SOUTH, (float)mBoundSouth)
		.putString(Settings.LOC_ADDRESS, mAddress)
		.putLong(Settings.LOC_TIME, getTime())
		.commit();
	}
	
	public boolean restoreState() {
		
		SharedPreferences prefs = mEta.getSettings().getPrefs();
		if (prefs.contains(Settings.LOC_SENSOR) && prefs.contains(Settings.LOC_RADIUS) && prefs.contains(Settings.LOC_LATITUDE) && 
				prefs.contains(Settings.LOC_LONGITUDE) && prefs.contains(Settings.LOC_ADDRESS)  && prefs.contains(Settings.LOC_TIME) ) {
			
			mSensor = prefs.getBoolean(Settings.LOC_SENSOR, false);
			mRadius = prefs.getInt(Settings.LOC_RADIUS, Integer.MAX_VALUE);
			setLatitude(prefs.getFloat(Settings.LOC_LATITUDE, 0.0f));
			setLongitude(prefs.getFloat(Settings.LOC_LONGITUDE, 0.0f));
			mBoundEast = prefs.getFloat(Settings.LOC_BOUND_EAST, 0.0f);
			mBoundWest = prefs.getFloat(Settings.LOC_BOUND_WEST, 0.0f);
			mBoundNorth = prefs.getFloat(Settings.LOC_BOUND_NORTH, 0.0f);
			mBoundSouth = prefs.getFloat(Settings.LOC_BOUND_SOUTH, 0.0f);
			mAddress = prefs.getString(Settings.LOC_ADDRESS, null);
			setTime(prefs.getLong(Settings.LOC_TIME, System.currentTimeMillis()));
			return true;
		} 
		return false;
		
	}
	
	public void save() {
		setTime(System.currentTimeMillis());
		saveState();
		notifySubscribers();
	}
	
	/**
	 * Invoke notifications to subscribers of this location object.<br><br>
	 * This object automatically notifies all subscribers on changes.
	 */
	public void notifySubscribers() {
		if (!mPushNotifications)
			return;
		
		for (LocationListener l : mSubscribers) {
			try {
				l.onChange();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Subscribe to events and changes in location.
	 * @param listener for callbacks
	 */
	public void subscribe(LocationListener listener) {
		if (!mSubscribers.contains(listener))
			mSubscribers.add(listener);
	}

	/**
	 * Unsubscribe from events and changes in location.
	 * @param listener to remove
	 * @return true if this Collection is modified, false otherwise.
	 */
	public boolean unSubscribe(LocationListener listener) {
		return mSubscribers.remove(listener);
	}
	
	@Override 
	public String toString() {
        return "Location[mProvider=" + getProvider() +
                ",mTime=" + getTime() +
                ",mLatitude=" + getLatitude() +
                ",mLongitude=" + getLongitude() +
                ",mRadius=" + mRadius +
                ",mSensor=" + mSensor + "]";
	}
	
	public interface LocationListener {
		public void onChange();
	}
	
}