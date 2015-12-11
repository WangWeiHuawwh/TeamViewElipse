package com.example.teamview;

import org.xutils.x;

import android.app.Application;

/**
 * Created by Administrator on 2015/11/30.
 */
public class myApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		x.Ext.init(this);
		x.Ext.setDebug(true); // 是否输出debug日志
	}
}
