package com.gyg9.android.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by gyliu on 15/11/10.
 */
public class ToastUtil {
	private static Toast toast;

	public static void show(Context context, String content) {
		if (null == toast) {
			toast = Toast.makeText(context, content, Toast.LENGTH_LONG);
		} else {
			toast.setText(content);
		}
		toast.show();
	}
}
