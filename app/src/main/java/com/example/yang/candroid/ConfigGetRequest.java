package com.example.yang.candroid;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import java.util.Map;

public class ConfigGetRequest extends Request<Void> {

	private static final String URL =
		"http://128.46.213.232:3000/.well-known/oada-configuration";

	public ConfigGetRequest() {
		super(Method.GET, URL, new ErrorListener());
	}

	@Override
	protected Response<Void> parseNetworkResponse(NetworkResponse response) {
		// Don't Care about response
		return null;
	}

	@Override
	protected void deliverResponse(Void response) {
		// Don't Care about response
	}

	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		return null;
	}

	private static class ErrorListener implements Response.ErrorListener {

		@Override
		public void onErrorResponse(VolleyError error) {
			VolleyLog.d("Error: " + error.getMessage());
		}
	}
}
