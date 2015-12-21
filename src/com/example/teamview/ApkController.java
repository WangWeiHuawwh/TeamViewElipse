package com.example.teamview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

public class ApkController {
	public static Context context;
	public static String mypath = "/data/data/com.example.teamview/files/QuickSupport.apk";

	// file:/android_asset/QuickSupport.apk
	// /data/data/com.example.teamview/files/QuickSupport.apk
	public ApkController(Context mcontext) {
		context = mcontext;
		File file = new File(mypath);
		if (!file.exists()) {
			Log.d("Demolog", "no exists");
			getAssetFile();
		}
	}

	public static void getAssetFile() {
		AssetManager asset = context.getAssets();
		try {
			InputStream is = asset.open("QuickSupport.apk");
			FileOutputStream fos = context.openFileOutput("QuickSupport.apk",
					Context.MODE_PRIVATE + Context.MODE_WORLD_READABLE);
			byte[] buffer = new byte[1024];
			int len = 0;

			while ((len = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}

			fos.flush();

			is.close();

			fos.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 描述: 安装 修改人: 吴传龙 最后修改时间:2015年3月8日 下午9:07:50
	 */
	public static boolean install() {
		// 先判断手机是否有root权限
		if (hasRootPerssion()) {
			// 有root权限，利用静默安装实现
			return clientInstall(mypath);
		} else {
			Log.d("Demolog", "no root");
			// 没有root权限，利用意图进行安装
			File file = new File(mypath);
			if (!file.exists())
				return false;
			Intent intent = new Intent();
			intent.setAction("android.intent.action.VIEW");
			intent.addCategory("android.intent.category.DEFAULT");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(Uri.fromFile(file),
					"application/vnd.android.package-archive");
			context.startActivity(intent);
			return true;
		}
	}

	/**
	 * 描述: 卸载 修改人: 吴传龙 最后修改时间:2015年3月8日 下午9:07:50
	 */
	public static boolean uninstall(String packageName, Context context) {
		if (hasRootPerssion()) {
			// 有root权限，利用静默卸载实现
			return clientUninstall(packageName);
		} else {
			Uri packageURI = Uri.parse("package:" + packageName);
			Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,
					packageURI);
			uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(uninstallIntent);
			return true;
		}
	}

	/**
	 * 判断手机是否有root权限
	 */
	private static boolean hasRootPerssion() {
		PrintWriter PrintWriter = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("su");
			PrintWriter = new PrintWriter(process.getOutputStream());
			PrintWriter.flush();
			PrintWriter.close();
			// int value = process.waitFor();
			int value = 1;
			Log.d("DemoLog", "value=" + value);
			return returnResult(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return false;
	}

	/**
	 * 静默安装
	 */
	private static boolean clientInstall(String apkPath) {
		PrintWriter PrintWriter = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("su");
			PrintWriter = new PrintWriter(process.getOutputStream());
			PrintWriter.println("chmod 777 " + apkPath);
			PrintWriter
					.println("export LD_LIBRARY_PATH=/vendor/lib:/system/lib");
			PrintWriter.println("pm install -r " + apkPath);
			// PrintWriter.println("exit");
			PrintWriter.flush();
			PrintWriter.close();
			int value = process.waitFor();
			return returnResult(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return false;
	}

	/**
	 * 静默卸载
	 */
	private static boolean clientUninstall(String packageName) {
		PrintWriter PrintWriter = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("su");
			PrintWriter = new PrintWriter(process.getOutputStream());
			PrintWriter.println("LD_LIBRARY_PATH=/vendor/lib:/system/lib ");
			PrintWriter.println("pm uninstall " + packageName);
			PrintWriter.flush();
			PrintWriter.close();
			int value = process.waitFor();
			return returnResult(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return false;
	}

	/**
	 * 启动app com.exmaple.client/.MainActivity
	 * com.exmaple.client/com.exmaple.client.MainActivity
	 */
	public static boolean startApp(String packageName, String activityName) {
		boolean isSuccess = false;
		String cmd = "am start -n " + packageName + "/" + activityName + " \n";
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			int value = process.waitFor();
			return returnResult(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return isSuccess;
	}

	private static boolean returnResult(int value) {
		// 代表成功
		if (value == 0) {
			return true;
		} else if (value == 1) { // 失败
			return false;
		} else { // 未知情况
			return false;
		}
	}
}
