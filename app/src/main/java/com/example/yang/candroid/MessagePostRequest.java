package com.example.yang.candroid;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonRequest;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MessagePostRequest extends JsonRequest<Void> {
	private String mAccessToken;

	public MessagePostRequest(String accessToken, String url,
		String msg) {
		super(Request.Method.POST, url, makeJson(msg).toString(), new Listener(), new ErrorListener());
		mAccessToken = accessToken;
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "Bearer" + " " + mAccessToken);
		return headers;
	}

	@Override
	protected Response<Void> parseNetworkResponse(NetworkResponse response) {
		// Don't Care about response
		return null;
	}

	private static JSONObject makeJson(String msg) {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("data", msg);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return jsonObject;
	}

	private static class ErrorListener implements Response.ErrorListener {
		@Override
		public void onErrorResponse(VolleyError error) {
			VolleyLog.d("Error: " + error.getMessage());
		}
	}

	private static class Listener implements Response.Listener<Void> {
		@Override
		public void onResponse(Void v) {

		}
	}
}
