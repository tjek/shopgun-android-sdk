package com.eTilbudsavis.etasdk.Network.Impl;

import com.eTilbudsavis.etasdk.Log.EtaLog;
import com.eTilbudsavis.etasdk.Network.RequestDebugger;
import com.eTilbudsavis.etasdk.Network.Request;

public class PerformanceDebugger implements RequestDebugger {
	
	public String TAG = PerformanceDebugger.class.getSimpleName();
	
	public void debug(Request<?> req) {
		EtaLog.d(TAG, req.getLog().getString(getClass().getSimpleName()));
	}
	
}
