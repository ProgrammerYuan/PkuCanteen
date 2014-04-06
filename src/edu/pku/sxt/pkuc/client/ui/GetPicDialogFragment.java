package edu.pku.sxt.pkuc.client.ui;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.ImageModification;

/**
 * Choosing get image by importing existing image or capturing new image.
 * @author songxintong
 *
 */
public class GetPicDialogFragment extends DialogFragment {
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		String items[] = {getString(R.string.capture), getString(R.string.picture)};
		final Activity activity = getActivity();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder
		.setTitle(getString(R.string.choose_pic_from))
		.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){	// capture
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					File f = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tmp.jpg");
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
					activity.startActivityForResult(intent, ImageModification.REQUEST_CODE_CAPTURE);
				} else {	// picture
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("image/*");
					activity.startActivityForResult(intent, ImageModification.REQUEST_CODE_IMPORT);
				}
			}
		});
		return builder.create();
	}
}
