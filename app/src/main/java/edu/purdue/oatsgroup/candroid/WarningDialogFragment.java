package edu.purdue.oatsgroup.candroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

public class WarningDialogFragment extends DialogFragment {
	public String mWarningMsg = "default";
	private static final String TAG = "WarningDialog";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog d = new AlertDialog.Builder(getActivity())
				.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							MainActivity callingActivity =
								(MainActivity) getActivity();
							callingActivity.onAddNewFilter();
							WarningDialogFragment.this.getDialog().cancel();
						}
				})
				.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Toast.makeText(getActivity(), "continue logging...",
									Toast.LENGTH_SHORT).show();
							WarningDialogFragment.this.getDialog().cancel();
						}
				})
				.setTitle("Warning")
				.setMessage(mWarningMsg)
				.create();

		return d;
	}
}
