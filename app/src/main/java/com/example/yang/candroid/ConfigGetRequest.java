package com.example.yang.candroid;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class ConfigGetRequest extends JsonObjectRequest {

	private static final String URL =
		"http://128.46.213.232:3000/.well-known/oada-configuration";
	private static final String TAG = "ConfigGetRequest";
	protected String mAuthorizationEndpoint;
	protected String mRegistrationEndpoint;

	public ConfigGetRequest() {
		super(Method.GET, URL, null, new ErrorListener());
	}

	@Override
	protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
		Log.i(TAG, response.toString());
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
			Log.i(TAG, jsonString);
			JSONObject jsonObject = new JSONObject(jsonString);
			mAuthorizationEndpoint = jsonObject.getString("authorization_endpoint");
			mRegistrationEndpoint = jsonObject.getString("registration_endpoint");
			Log.i(TAG, mAuthorizationEndpoint);
			Log.i(TAG, mRegistrationEndpoint);
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
	}

	private static class ErrorListener implements Response.ErrorListener {

		@Override
		public void onErrorResponse(VolleyError error) {
			VolleyLog.d("Error: " + error.getMessage());
		}
	}
}
