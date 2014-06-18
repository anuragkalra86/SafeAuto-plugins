package com.sa.plugin.cardio.android;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class PluginActivity extends CordovaPlugin {

	public static final String MY_ACTION = "takePicture";
	// actions available
	private static final String SCAN_ACTION = "scan";
	private static final String CAN_SCAN_ACTION = "canScan";
	private static final String VERSION_ACTION = "version";


	/**
	 * App token received after registring at www.card.io
	 * Reading from Configuration file
	 */
	private static final String MY_CARDIO_APP_TOKEN = "c920b19d46854090af1b1a84e8618022";
	private int MY_SCAN_REQUEST_CODE = 100; // arbitrary int
	private Activity myActivity = null;
	private CallbackContext callbackContextInstance = null;

	private static final String TAG = PluginActivity.class.getName();

/*	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, "Entering execute method");

		if (MY_ACTION.equals(action)) {
			LOG.d(TAG, "Correct action was passed");
			myActivity = cordova.getActivity();
			// this tells Cordova that this class contains the Callback method which should be called once Activity completes
			cordova.setActivityResultCallback(this);
			Log.d(TAG, "Calling onScanPress()");
			onScanPress(null);
			Log.d(TAG, "Returned from onScanPress() into execute");
			callbackContextInstance = callbackContext;
			return true;
		}
		else {
			LOG.e(TAG, "Unknown action was passed via JavaScript interface");
			callbackContext.error("Unknown action was passed via JavaScript interface");
		}
		return false;
	}*/

	@Override
	public boolean execute(String action, CordovaArgs args,
			CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, "Entering execute method");
		Log.d(TAG, "Action is: " + action);

		if (SCAN_ACTION.equalsIgnoreCase(action) ){
			myActivity = cordova.getActivity();
			// this tells Cordova that this class contains the Callback method which should be called once Activity completes
			cordova.setActivityResultCallback(this);
			Log.d(TAG, "Calling onScanPress()");
			onScanPress(args);
			Log.d(TAG, "Returned from onScanPress() into execute");
			callbackContextInstance = callbackContext;
			return true;
		} else if (CAN_SCAN_ACTION.equalsIgnoreCase(action)) {

		} else if (VERSION_ACTION.equalsIgnoreCase(action)) {

		} else {
			Log.e(TAG, "No matching action was sent to Plugin. Exiting by calling CallbackContext.error");
			callbackContext.error("No matching action was sent to Plugin"); 
		}

		return false;
	}

	private void onScanPress(CordovaArgs args) {
		Log.d(TAG, "Entering onScanPress method");

		try {
			if (args == null || args.isNull(0)) {
				callbackContextInstance.error("Card.io SDK token was not sent to plugin");
				return;
			}
			String sdkToken = args.getString(0);
			if (sdkToken == null || sdkToken.length() < 1){
				callbackContextInstance.error("Card.io SDK token was not sent to plugin");
				return;
			}
			Log.d(TAG, "my sdkToken: " + sdkToken);

			// default values for our app
			boolean collect_expiry = false;
			boolean collect_cvv = false;
			boolean collect_zip = true;
			boolean disable_manual_entry_buttons = false;
			String languageOrLocale = "en";

			if (! args.isNull(1)) {
				JSONObject cardIO_Options = args.getJSONObject(1);
				if (!cardIO_Options.isNull("collect_expiry")) {
					collect_expiry = cardIO_Options.getBoolean("collect_expiry");
				}
				if (!cardIO_Options.isNull("collect_cvv")) {
					collect_cvv = cardIO_Options.getBoolean("collect_cvv");
				}
				if (!cardIO_Options.isNull("collect_zip")) {
					collect_zip = cardIO_Options.getBoolean("collect_zip");
				}
				if (!cardIO_Options.isNull("disable_manual_entry_buttons")) {
					disable_manual_entry_buttons = cardIO_Options.getBoolean("disable_manual_entry_buttons");
				}
				if (!cardIO_Options.isNull("languageOrLocale")) {
					languageOrLocale = cardIO_Options.getString("languageOrLocale");
				}
			}

			Intent scanIntent = new Intent(myActivity, CardIOActivity.class);

			// required for authentication with card.io
			scanIntent.putExtra(CardIOActivity.EXTRA_APP_TOKEN, sdkToken);

			// customize these values to suit your needs.
			scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, collect_expiry); // default: true
			scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, collect_cvv); // default: false
			scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, collect_zip); // default: false
			scanIntent.putExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE, languageOrLocale);

			// hides the manual entry button
			// if set, developers should provide their own manual entry mechanism in the app
			scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, disable_manual_entry_buttons); // default: false

			scanIntent.putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, true);

			// MY_SCAN_REQUEST_CODE is arbitrary and is only used within this activity.
			myActivity.startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
		} catch (Exception e) {
			Log.e(TAG, "Some error happened. Calling failure callback method of app", e);
		}
		Log.d(TAG, "Exiting onScanPress method without waiting for onActivityResult to be called");
	}

	/*
	 * Example response_json
	 * {
	 *  card_type: 
	 *  redacted_card_number: 
	 *  expiry_month:
	 *  card_number: 123465789
	 *  expiry_year: 
	 *  cvv: 
	 *  zip: 12345
	 * }
	 * Currently, we'll be sending card_number and zip only in response_json
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.d(TAG, "Entering into onActivityResult");
		Log.d(TAG, "requestCode: " + requestCode + " resultCode: " + resultCode + " intent: " + intent);
		super.onActivityResult(requestCode, resultCode, intent);
		String resultStr;
		if (intent != null && intent.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
			CreditCard scanResult = intent.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
			JSONObject responseJsonObj = new JSONObject();
			// Never log a raw card number. Avoid displaying it, but if necessary use getFormattedCardNumber()
			resultStr = "CardIOActivity returned result SUCCESS";
			try {
				responseJsonObj.put("card_number", scanResult.cardNumber);
				/*if (scanResult.isExpiryValid()) {
//					resultStr += "Expiration Date: " + scanResult.expiryMonth + "/" + scanResult.expiryYear + "\n"; 
				}
				jsonObj.put("expiryMonth", scanResult.expiryMonth);
				jsonObj.put("expiryYear", scanResult.expiryYear);
				jsonObj.put("cvv", scanResult.cvv);
				if (scanResult.cvv != null) { 
					// Never log or display a CVV
					resultStr += "CVV has " + scanResult.cvv.length() + " digits.\n";
				}*/

				if (scanResult.postalCode != null) {
					responseJsonObj.put("zip", scanResult.postalCode);
				}
				callbackContextInstance.success(responseJsonObj);

			} catch (JSONException e) {
				Log.e(TAG, "Exception while getting card details..", e);
			}


		}
		else {
			resultStr = "Scan was canceled.";
			callbackContextInstance.error("Scan was cancelled");
		}
		Log.d(TAG, "My resultStr: " + resultStr);
	}

}
