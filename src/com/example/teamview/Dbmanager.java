package com.example.teamview;

import java.io.File;
import java.util.List;

import org.xutils.DbManager;
import org.xutils.x;
import org.xutils.ex.DbException;

import android.util.Log;

public class Dbmanager {
	static DbManager.DaoConfig daoConfig = new DbManager.DaoConfig()
			.setDbName("teamview").setDbDir(new File("/sdcard"))
			.setDbVersion(1)
			.setDbUpgradeListener(new DbManager.DbUpgradeListener() {
				@Override
				public void onUpgrade(DbManager arg0, int arg1, int arg2) {
					// TODO Auto-generated method stub

				}
			});
	static DbManager db = x.getDb(daoConfig);

	public static void saveUser(User user) {
		try {
			db.delete(User.class);
			db.save(user);
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("DemoLog", "save db error " + e.getMessage());
		}

	}

	public static String getUserId() {
		List<User> users = null;
		try {
			users = db.selector(User.class).findAll();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("DemoLog", "get db error " + e.getMessage());
		}
		if (users.size() > 0) {
			Log.e("DemoLog", "get db userid=" + users.get(0).getmIdText());
			return users.get(0).getmIdText();
		} else {
			return "0";
		}
	}

}
