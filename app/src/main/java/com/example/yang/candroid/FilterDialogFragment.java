package com.example.yang.candroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.isoblue.can.CanSocketJ1939.Filter;

public class FilterDialogFragment extends DialogFragment {
	private TextView mName;
	private TextView mAddr;
	private TextView mPgn;
	private static final String TAG = "FilterDialog";
	private static final String dTitle = "Please Add Filters";
	private static final String dMsg = "Press 'Go' without any filter will " +
		"give you all J1939 messages";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		final View dView = inflater.inflate(R.layout.dialog_startup, null);
		mName = (EditText) dView.findViewById(R.id.filter_name);
		mAddr = (EditText) dView.findViewById(R.id.filter_addr);
		mPgn = (EditText) dView.findViewById(R.id.filter_pgn);
		final AlertDialog d = new AlertDialog.Builder(getActivity())
				.setView(dView)
				.setPositiveButton("Add", null)
				.setNegativeButton("Go",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							MainActivity callingActivity =
								(MainActivity) getActivity();
							callingActivity.onGo();
							FilterDialogFragment.this.getDialog().cancel();
						}
				})
				.setTitle(dTitle)
				.setMessage(dMsg)
				.create();

		d.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (paramsCheck(mName.getText().toString(),
										mAddr.getText().toString(),
										mPgn.getText().toString()) == -1) {
								Toast.makeText(getActivity(), "field(s) cannot"
											+ " be empty",
											Toast.LENGTH_SHORT).show();
						} else {
							int name = Integer.parseInt(
								mName.getText().toString());
							int addr = Integer.parseInt(
								mAddr.getText().toString());
							int pgn = Integer.parseInt(
								mPgn.getText().toString());
							MainActivity.mFilterOn = true;
							MainActivity.mFilter = new Filter(name, addr, pgn);
							MainActivity.mFilters.add(MainActivity.mFilter);
							Log.d(TAG, "add filter, " +
										"name: " + mName.getText().toString() +
										"addr: " + mAddr.getText().toString() +
										"pgn: " + mPgn.getText().toString());
							Toast.makeText(getActivity(), "filter added",
								Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		});
		return d;
	}

	public int paramsCheck(String name, String addr, String pgn) {
		if (name.isEmpty() || addr.isEmpty() || pgn.isEmpty()) {
			return -1;
		} else {
			return 0;
		}
	}
}
