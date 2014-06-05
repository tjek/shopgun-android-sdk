package com.eTilbudsavis.etasdk.Utils;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.eTilbudsavis.etasdk.Eta;
import com.eTilbudsavis.etasdk.FixedArrayList;

/**
 * EventLog class have been created to simplify logging within the ETA SDK.
 * You will notice that this class is being used extensively throughout the SDK.
 * And have been especially useful, while debugging networking issues.
 * 
 * @author Danny Hvam - danny@etilbudsavis.dk
 *
 */
public class EventLog {
	
	public static final String TAG = "EventLog";
	
	public static final String TYPE_REQUEST = "request";
	public static final String TYPE_EXCEPTION = "exception";
	public static final String TYPE_VIEW = "view";
	public static final String TYPE_LOG = "log";
	
	List<Event> mEvents;
	
	/**
	 * Create a new EventLog, with no size limitations. Please be aware that
	 * these logs can grow to a considerable size, and may use an unreasonable
	 * amount of memory, and is therefore only recommended under development.
	 */
	public EventLog() {
		mEvents = Collections.synchronizedList(new ArrayList<Event>());
	}
	
	/**
	 * Create a new log with a fixed size. The EventLig will be using FIFO ordering of events.
	 * @param logSize the desired size of the log
	 */
	public EventLog(int logSize) {
		mEvents = Collections.synchronizedList(new FixedArrayList<Event>(logSize));
	}
	
	/**
	 * Add a new Event to the log, based simply on a name. This is nice for tracing
	 * when, where and in what order the events have occurred.
	 * @param name of the log entry
	 */
	public void add(String name) {
		add(name, null, null);
	}
	
	/**
	 * Add a new Event to the log. Adding a special type, is nice for saving generic error correcting data
	 * to a log. This can be {@link #TYPE_VIEW view}, {@link #TYPE_EXCEPTION exception}, or {@link #TYPE_REQUEST request} events.
	 * but essentially any string will do.
	 * @param type of event to add
	 * @param data data accompanying the event
	 */
	public void add(String type, JSONObject data) {
		add(type, type, data);
	}
	
	/**
	 * 
	 * @param name
	 * @param type
	 * @param data
	 */
	private void add(String name, String type, JSONObject data) {
		long time = System.currentTimeMillis();
		String user = Eta.getInstance().getUser().getErn();
		String token = Eta.getInstance().getSessionManager().getSession().getToken();
		add(new Event(name, time, type, user, token, data));
	}
	
	/**
	 * Add a new Event to the log.
	 * @param e event to add
	 */
	public void add(Event e) {
		mEvents.add(e);
	}
	
	/**
	 * Get the current list of events
	 * @return
	 */
	public List<Event> getEvents() {
		return mEvents;
	}
	
	/**
	 * Clear the current list of events in the EventLog
	 */
	public void clear() {
		mEvents.clear();
	}
	
	/**
	 * Get all logs of a given type
	 * @param type of event to get
	 * @return a list of events
	 */
	public List<Event> getType(String type) {
		List<Event> tmp = new ArrayList<Event>();
		for (Event e : mEvents) {
			if (e.type.equals(type)) {
				tmp.add(e);
			}
		}
		return null;
	}
	
	/**
	 * Prints the timing of events in this EventLog
	 * @param name to use as print prefix
	 */
	public String getString(String name) {
		
		if (mEvents.isEmpty()) {
			EtaLog.d(TAG, String.format("[%+6d ms] %s", 0, name));
		}
		
		StringBuilder sb = new StringBuilder();
		long prevTime = mEvents.get(0).time;
		sb.append(String.format("     [%+6d ms] %s", getTotalDuration(), name)).append("\n");
		for (int i = 0; i < mEvents.size() ; i++) {
			Event e = mEvents.get(i);
			sb.append(String.format("[%2d] [%+6d ms] %s", i,(e.time - prevTime), e.name)).append("\n");
			prevTime = e.time;
		}
		
		return sb.toString();
		
	}
	
	public JSONArray toJSON(boolean rawTime) {
		return toJSON(mEvents, rawTime);
	}
	
	/**
	 * Create a JSONArray of the currents events
	 * @param events to torn into a JSONArray
	 * @return a JSONArray
	 */
	public static JSONArray toJSON(List<Event> events, boolean rawTime) {
		JSONArray jArray = new JSONArray();
		if (events != null && !events.isEmpty()) {
			for (Event e : events) {
				jArray.put(e.toJSON(rawTime));
			}
		}
		return jArray;
	}
	
	/**
	 * Get the total duration from the first Event were recorded till the last one.
	 * @return a non-negative number
	 */
	public long getTotalDuration() {
		if (mEvents.isEmpty()) {
			return 0;
		}
		
		long first = mEvents.get(0).time;
		long last = mEvents.get(mEvents.size() - 1).time;
		return last - first;
		
	}
	
	public static Comparator<Event> timestamp  = new Comparator<Event>() {
		
		public int compare(Event e1, Event e2) {
			
			if (e1 == null || e2 == null) {
				return e1 == null ? (e2 == null ? 0 : 1) : -1;
			} else {
				return e1.time < e2.time ? -1 : 1;
			}
			
		}

	};
	
	/**
	 * Simple helper class, for usage in EventLog
	 * @author Danny Hvam - danny@etilbudsavis.dk
	 *
	 */
	public static class Event {
		
		public final long time;
		public final String type;
		public final String token;
		public final String user;
		public final String name;
		public final JSONObject data;
		
		public Event(String name, long time, String type, String user, String token, JSONObject data) {
			this.name = name == null ? (type == null ? "unknown" : type) : name;
			this.time = time;
			this.type = type;
			this.user = user;
			this.token = token;
			this.data = data;
		}
		
		public JSONObject toJSON(boolean rawTime) {
			JSONObject o = new JSONObject();
			try {
				o.put("timestamp", (rawTime ? time : Utils.parseDate(new Date(time))) );
				o.put("type", type);
				o.put("token", token);
				o.put("userid", user);
				o.put("name", name);
				o.put("data", data);
			} catch (JSONException e) {
				EtaLog.e(TAG, e);
			}
			return o;
		}
		
	}

}