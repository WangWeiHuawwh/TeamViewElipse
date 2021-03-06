package com.example.teamview;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.xutils.x;
import org.xutils.common.Callback;
import org.xutils.common.Callback.CommonCallback;
import org.xutils.http.RequestParams;

/**
 * Created by Administrator on 2015/11/30.
 */
public class WatchService extends AccessibilityService {
	private static final int STATE_BEGIN = 1;
	private static final int STATE_ACTIVATIONING = 2;
	private static final int STATE_GET_ID_SUCCESS = 3;
	private static final int STATE_PC_ID_SUCCESS = 4;
	private static final int STATE_ALLOW = 5;
	private static final int STATE_REJECT = 6;
	private static final int STATC_CONNECTION_SUCCESS = 7;
	private static final int STATC_CONNECTION_OVER = 8;
	private static final String TAG = "DemoLog";
	private static final String MAIN_ACTIVITY = "com.teamviewer.quicksupport.ui.MainActivity";
	private static final String GET_ID_CLASS = "android.widget.TextView";
	private static final String GET_UID_PC = "android.app.Dialog";
	private static final String paName = "com.teamviewer.quicksupport.market";
	private TeamViewData mTeamViewData = new TeamViewData();
	private static final String FRAMELAYOUT = "android.widget.FrameLayout";
	private PowerManager.WakeLock mWakeLock;
	public static volatile int state = STATE_BEGIN;
	public static final int UPDATE_TIME = 1;
	public static final int SHUT_DOWN_TEAM = 2;// 结束进程
	public static final int SHUT_DOWN_CONNECTION = 3;// 结束连接
	public static final int UPDATE_ZHUANGTAI = 4;
	public static final int UPDATE_SHUTDOWN_ZHUANGTAI = 5;
	public static final int UPDATE_BEGIN_ZHUANGTAI = 6;
	public static final int BEGIN_BEGIN = 7;
	public static final int OPEN = 8;
	public static final int UPDATE_STARTEAM = 99;
	public static final int UPDATE_TIME_TIME = 30 * 1000;
	public static final int UPDATE_BEGIN_TIME_TIME = 60 * 1000;// 1分钟写一次
	public static final int SHUT_DOWN_TEAM_TIME = 3600 * 1000;
	public static final int RESTART = 5 * 60 * 1000;// 5分钟打开一次
	public ProcessWatcher processWatcher;
	public volatile int zhuangtaiTimes = 0;
	public volatile int shutDownTimes = 0;
	public static final int TIMES = 5;
	public static final int TRY_TIME = 2 * 1000;
	SettingUtils settingUtils;
	public volatile int getTimeTimes = 0;
	public static final int timeerrorTime = 1000;
	public ShutDownListener shutDownListener = new ShutDownListener() {

		@Override
		public void shutDown(boolean is) {
			// TODO Auto-generated method stub
			log("TeamView进程结束");
			mTeamViewData.pidId = 0;
			// handler.removeCallbacksAndMessages(null);
			// handler.sendEmptyMessageDelayed(UPDATE_STARTEAM,5*60*1000);
		}

	};
	public Handler handler = new Handler() {

		@Override
		public void handleMessage(Message message) {
			// TODO Auto-generated method stub
			super.handleMessage(message);
			final String zhuangtai = (String) message.obj;
			switch (message.what) {
			case UPDATE_TIME:

				RequestParams params = new RequestParams(
						UrlData.URL_UPDATE_TIME);
				params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
				params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
				params.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
				params.addBodyParameter("设备ID", mTeamViewData.mIdText);
				params.addBodyParameter("使用时间", mTeamViewData.BEGIN_TIME);
				log("用户ID=" + mTeamViewData.mPCIDTEXT + ";设备ID="
						+ mTeamViewData.mIdText + "连接判断=" + "使用时间="
						+ mTeamViewData.BEGIN_TIME);
				x.http().post(params, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						try {
							String response = XMLParse.parseResponseCheck(sbs);
							log("连接判断 response=" + response);
							if (response.equals("成功")) {
								handler.removeMessages(UPDATE_TIME);
								handler.sendEmptyMessageDelayed(UPDATE_TIME,
										UPDATE_TIME_TIME);
							} else {
								handler.removeMessages(UPDATE_TIME);
								handler.sendEmptyMessageDelayed(UPDATE_TIME,
										UPDATE_TIME_TIME);
							}
						} catch (Exception e) {
						}

					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("连接判断 error" + ex.getMessage());

					}

					@Override
					public void onCancelled(CancelledException cex) {

					}

					@Override
					public void onFinished() {

					}
				});

				break;
			case SHUT_DOWN_TEAM:
				if (state == STATC_CONNECTION_SUCCESS) {
					handler.removeMessages(SHUT_DOWN_CONNECTION);
					handler.sendEmptyMessage(SHUT_DOWN_CONNECTION);
				}

				break;
			case UPDATE_STARTEAM:
				startTeamView();
				break;
			case OPEN:
				log("restart state=" + state);
				if (state != STATC_CONNECTION_SUCCESS) {
					Intent mIntent = new Intent();
					mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					ComponentName comp = new ComponentName(paName,
							MAIN_ACTIVITY);
					mIntent.setComponent(comp);
					mIntent.setAction("android.intent.action.VIEW");
					startActivity(mIntent);
				}
				handler.removeMessages(OPEN);
				handler.sendEmptyMessageDelayed(OPEN, RESTART);
				break;
			case SHUT_DOWN_CONNECTION:
				state = STATC_CONNECTION_OVER;
				handler.removeCallbacksAndMessages(null);
				// 杀死进程，重启teamview
				log("kill killBackgroundProcesses");
				try {
					Process suProcess = Runtime.getRuntime().exec("su");
					DataOutputStream os = new DataOutputStream(
							suProcess.getOutputStream());
					os.writeBytes("adb shell" + "\n");
					os.flush();
					os.writeBytes("am force-stop " + paName + "\n");
					os.flush();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log(e.getMessage());
				}
				if (processWatcher != null) {
					processWatcher.stop();
				}
				handler.removeMessages(UPDATE_STARTEAM);
				handler.sendEmptyMessageDelayed(UPDATE_STARTEAM, 1000 * 8);

				RequestParams params2 = new RequestParams(
						UrlData.URL_UPDATE_TIME_END);
				params2.addBodyParameter("授权用户", UrlData.ADMIN_UID);
				params2.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
				params2.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
				params2.addBodyParameter("设备ID", mTeamViewData.mIdText);
				params2.addBodyParameter("使用时间", mTeamViewData.BEGIN_TIME);
				log("用户ID=" + mTeamViewData.mPCIDTEXT + ";设备ID="
						+ mTeamViewData.mIdText + "使用时间="
						+ mTeamViewData.BEGIN_TIME);
				x.http().post(params2, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						try {
							String response = XMLParse.parseResponseCheck(sbs);
							log("结束时间 response=" + response);
							if (response.equals("成功")) {// 结束的时候不用更改状态写入结束信息即可
								// Message msg = new Message();
								// msg.what = UPDATE_ZHUANGTAI;
								// msg.obj = (String) "空闲";
								// handler.removeMessages(UPDATE_ZHUANGTAI);
								// handler.sendMessage(msg);
								handler.removeMessages(UPDATE_SHUTDOWN_ZHUANGTAI);
								handler.sendEmptyMessage(UPDATE_SHUTDOWN_ZHUANGTAI);
							}
						} catch (Exception e) {

						}

					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("结束时间 error" + ex.getMessage());

					}

					@Override
					public void onCancelled(CancelledException cex) {

					}

					@Override
					public void onFinished() {

					}
				});

				break;
			case UPDATE_ZHUANGTAI:

				log("连接状态=" + zhuangtai);
				RequestParams params3 = new RequestParams(
						UrlData.URL_UPDATE_ZHUANGTAI);
				params3.addBodyParameter("授权用户", UrlData.ADMIN_UID);
				params3.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
				params3.addBodyParameter("连接状态", zhuangtai);
				params3.addBodyParameter("设备ID", mTeamViewData.mIdText);
				log("连接状态=" + zhuangtai + ";设备ID=" + mTeamViewData.mIdText);
				x.http().post(params3, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						try {
							String response = XMLParse.parseResponseCheck(sbs);
							log("更新状态 response=" + response);
							if (response.equals("成功")) {
								zhuangtaiTimes = 0;
							} else {
								if (zhuangtaiTimes < TIMES) {
									Message msg = new Message();
									msg.what = UPDATE_ZHUANGTAI;
									msg.obj = zhuangtai;
									handler.removeMessages(UPDATE_ZHUANGTAI);
									handler.sendMessageDelayed(msg, TRY_TIME);
									zhuangtaiTimes++;
								}
							}
						} catch (Exception e) {
							log("ZHUANGTAI=" + e.getMessage());
						}

					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("更新状态 error" + ex.getMessage());

					}

					@Override
					public void onCancelled(CancelledException cex) {

					}

					@Override
					public void onFinished() {

					}
				});

				break;
			case UPDATE_SHUTDOWN_ZHUANGTAI:
				RequestParams params4 = new RequestParams(
						UrlData.URL_SHUT_DOWN_ZHUANGTAI);
				params4.addBodyParameter("授权用户", UrlData.ADMIN_UID);
				params4.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
				params4.addBodyParameter("设备ID", mTeamViewData.mIdText);
				log("设备ID=" + mTeamViewData.mIdText);
				x.http().post(params4, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						try {
							String response = XMLParse.parseResponseCheck(sbs);
							log("更新结束信息 response=" + response);
							if (response.equals("成功")) {
								shutDownTimes = 0;
								mTeamViewData.mPCIDTEXT = "";
								state = STATE_BEGIN;
							} else {
								if (shutDownTimes < TIMES) {
									handler.removeMessages(UPDATE_SHUTDOWN_ZHUANGTAI);
									handler.sendEmptyMessageDelayed(
											UPDATE_SHUTDOWN_ZHUANGTAI, TRY_TIME);
									shutDownTimes++;
								}
							}
						} catch (Exception e) {
						}

					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("更新结束信息 error" + ex.getMessage());

					}

					@Override
					public void onCancelled(CancelledException cex) {

					}

					@Override
					public void onFinished() {

					}
				});

				break;
			case UPDATE_BEGIN_ZHUANGTAI:// 写状态时直接调用状态的handler即可
				log("UPDATE_BEGIN_ZHUANGTAI=" + state + ";id="
						+ mTeamViewData.mIdText);
				if (state == STATE_BEGIN || state == STATE_ACTIVATIONING)// 没有获取到id，正在激活,离线状态
				{
					// 没有获取到id，写入离线状态，并同时再次进行此handler
					// 如果没有获取到id，如何写离线状态？怎么找到对应id的记录写呢?
					mTeamViewData.mIdText = Dbmanager.getUserId();
					Message msg = new Message();
					msg.what = UPDATE_ZHUANGTAI;
					msg.obj = (String) "离线";
					handler.removeMessages(UPDATE_ZHUANGTAI);
					handler.sendMessage(msg);
					handler.removeMessages(UPDATE_BEGIN_ZHUANGTAI);
					handler.sendEmptyMessageDelayed(UPDATE_BEGIN_ZHUANGTAI,
							UPDATE_BEGIN_TIME_TIME);
				} else if (state == STATE_GET_ID_SUCCESS)// 获取到id，正在空闲状态
				{
					// 成功获取到id，写入空闲状态，并停止handler
					Message msg = new Message();
					msg.what = UPDATE_ZHUANGTAI;
					msg.obj = (String) "空闲";
					handler.removeMessages(UPDATE_ZHUANGTAI);
					handler.sendMessage(msg);
					handler.removeMessages(UPDATE_BEGIN_ZHUANGTAI);
					handler.sendEmptyMessageDelayed(UPDATE_BEGIN_ZHUANGTAI,
							UPDATE_BEGIN_TIME_TIME);
				}
				break;
			case BEGIN_BEGIN:
				state = STATC_CONNECTION_SUCCESS;
				log("connection success");

				RequestParams params5 = new RequestParams(
						UrlData.URL_BEGIN_CONNECT);
				params5.addBodyParameter("授权用户", UrlData.ADMIN_UID);
				params5.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
				params5.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
				params5.addBodyParameter("设备ID", mTeamViewData.mIdText);
				params5.addBodyParameter("使用时间", mTeamViewData.BEGIN_TIME);
				log("用户ID=" + mTeamViewData.mPCIDTEXT + ";设备ID="
						+ mTeamViewData.mIdText + "使用时间="
						+ mTeamViewData.BEGIN_TIME);
				x.http().post(params5, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						try {
							String response = XMLParse.parseResponseCheck(sbs);
							Log.d("DemoLog", "开始时间 response=" + response);
							if (response.equals("成功")) {
								// 点击自动允许
								if (mTeamViewData.allowButton != null) {
									mTeamViewData.allowButton
											.performAction(AccessibilityNodeInfo.ACTION_CLICK);
									// state = STATE_ALLOW;
								} else {
									log("allowButton==null");
								}
								Message msg = new Message();
								msg.what = UPDATE_ZHUANGTAI;
								msg.obj = (String) "忙碌";
								handler.removeMessages(UPDATE_ZHUANGTAI);
								handler.sendMessage(msg);
								handler.removeMessages(UPDATE_TIME);
								handler.sendEmptyMessageDelayed(UPDATE_TIME,
										UPDATE_TIME_TIME);
								handler.removeMessages(SHUT_DOWN_TEAM);
								handler.sendEmptyMessageDelayed(SHUT_DOWN_TEAM,
										SHUT_DOWN_TEAM_TIME);
								Intent i = new Intent(Intent.ACTION_MAIN);
								i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								i.addCategory(Intent.CATEGORY_HOME);
								startActivity(i);
							}
						} catch (Exception e) {
							log(e.getMessage());
						}

					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						Log.d("DemoLog", "开始时间 error" + ex.getMessage());

					}

					@Override
					public void onCancelled(CancelledException cex) {

					}

					@Override
					public void onFinished() {

					}
				});

				break;

			}
		}

	};

	public void execShell(String cmd) {
		try {
			// 权限设置
			Process p = Runtime.getRuntime().exec("su");
			// 获取输出流
			OutputStream outputStream = p.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(
					outputStream);
			// 将命令写入
			dataOutputStream.writeBytes(cmd);
			// 提交命令
			dataOutputStream.flush();
			// 关闭流操作
			dataOutputStream.close();
			outputStream.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mWakeLock != null) {
			mWakeLock.release();
		}
		if (!TextUtils.isEmpty(mTeamViewData.mIdText)
				&& !TextUtils.isEmpty(mTeamViewData.mPCIDTEXT)) {
			RequestParams params = new RequestParams(UrlData.URL_SHUT_DOWN_OWN);
			params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
			params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
			params.addBodyParameter("设备ID", mTeamViewData.mIdText);
			params.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
			log("违例结束   设备ID=" + mTeamViewData.mIdText + "用户id="
					+ mTeamViewData.mPCIDTEXT);
			x.http().post(params, new CommonCallback<String>() {
				@Override
				public void onSuccess(String result) {
					InputStream sbs = new ByteArrayInputStream(result
							.getBytes());
					String response = "";
					try {
						response = XMLParse.parseResponseCheck(sbs);
						log("违例结束=" + response);
					} catch (Exception e) {

					}
				}

				@Override
				public void onError(Throwable ex, boolean isOnCallback) {
					log("违例结束   error" + ex.getMessage());

				}

				@Override
				public void onCancelled(CancelledException cex) {

				}

				@Override
				public void onFinished() {

				}
			});

		}
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
		try {
			Process suProcess = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(
					suProcess.getOutputStream());
			os.writeBytes("adb shell" + "\n");
			os.flush();
			os.writeBytes("am force-stop " + paName + "\n");
			os.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log(e.getMessage());
		}
		if (processWatcher != null) {
			processWatcher.stop();
		}
		if (settingUtils != null) {
			settingUtils.reset();
		}
	}

	private LogWriter mLogWriter;

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
		mWakeLock.acquire();
		File logf = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "DemoLog.txt");

		try {
			mLogWriter = LogWriter.open(logf.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generatedo catch block
			log(e.getMessage());
		}
		log("onCreate()");
		// 测试杀掉teamview再打开功能
		try {
			Runtime.getRuntime().exec(
					new String[] { "/system/bin/su", "-c", "ls -al" });
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!isAppInstalled(WatchService.this, paName)) {
			Log.d("DemoLog", "no install");
			ApkController apkController = new ApkController(WatchService.this);
			apkController.install();
			handler.removeMessages(UPDATE_BEGIN_ZHUANGTAI);
			handler.sendEmptyMessage(UPDATE_BEGIN_ZHUANGTAI);
		} else {
			try {
				startTeamView();
			} catch (Exception e) {

			}
		}
		// 亮度调节
		settingUtils = new SettingUtils(WatchService.this);
		settingUtils.setBrightness(0);

	}

	private boolean isAppInstalled(Context context, String uri) {
		PackageManager pm = context.getPackageManager();
		boolean installed = false;
		try {
			pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		return installed;
	}

	public void startTeamView() {
		log("startTeamView");
		Intent mIntent = new Intent();
		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ComponentName comp = new ComponentName(paName, MAIN_ACTIVITY);
		mIntent.setComponent(comp);
		mIntent.setAction("android.intent.action.VIEW");
		startActivity(mIntent);
		state = STATE_BEGIN;
		mTeamViewData.pidId = 0;
		handler.removeMessages(UPDATE_BEGIN_ZHUANGTAI);
		handler.sendEmptyMessageDelayed(UPDATE_BEGIN_ZHUANGTAI,
				UPDATE_BEGIN_TIME_TIME);
		// 先删除上一次的启动tv
		handler.removeMessages(UPDATE_STARTEAM);
		// 自动打开
		handler.removeMessages(OPEN);
		handler.sendEmptyMessageDelayed(OPEN, RESTART);
	}

	public void log(String msg) {
		Log.e(TAG, msg);
		try {
			mLogWriter.print(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, e.getMessage());
		}
	}

	public void log(String msg, int i) {
		Log.d(TAG, msg);
		try {
			mLogWriter.print(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, e.getMessage());
		}
	}

	@Override
	protected void onServiceConnected() {
		log("config success!");
	}

	@SuppressLint("NewApi")
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		// TODO Auto-generated method stub
		int eventType = event.getEventType();
		// AccessibilityNodeInfo my = event.getSource();
		AccessibilityNodeInfo rowNode = getRootInActiveWindow();
		if (rowNode == null) {
			log("noteInfo is　null", 1);
			return;
		} else {
			log("class=" + event.getClassName().toString(), 1);
			// List<AccessibilityNodeInfo> nodeInfos = rowNode
			// .findAccessibilityNodeInfosByText("稍后再说");
			// for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
			// nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
			// }
			recycle(rowNode);
		}
		String eventText = "";
		if (mTeamViewData.pidId == 0) {
			getProcessPid();
		}
		log("==============Start====================", 1);
		switch (eventType) {
		case AccessibilityEvent.TYPE_VIEW_CLICKED:
			eventText = "TYPE_VIEW_CLICKED";
			break;
		case AccessibilityEvent.TYPE_VIEW_FOCUSED:
			eventText = "TYPE_VIEW_FOCUSED";
			break;
		case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
			eventText = "TYPE_VIEW_LONG_CLICKED";
			break;
		case AccessibilityEvent.TYPE_VIEW_SELECTED:
			eventText = "TYPE_VIEW_SELECTED";
			break;
		case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
			eventText = "TYPE_VIEW_TEXT_CHANGED";
			break;
		case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
			eventText = "TYPE_WINDOW_STATE_CHANGED";
			if (event.getClassName().toString().equals(MAIN_ACTIVITY)) {
				int hash = findIDTextView(rowNode);
				if (hash != 0) {
					log("have hash success=" + mTeamViewData.mIdTextHash);
					state = STATE_ACTIVATIONING;
				}
			}
			if (event.getClassName().toString().equals(GET_UID_PC)) {
				if (ReadyPCID(rowNode)) {
					log("have pcid success=" + mTeamViewData.mPCIDTEXT);
					// state = STATE_PC_ID_SUCCESS;
					RequestParams params = new RequestParams(
							UrlData.URL_CHECK_ID);
					params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
					params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
					params.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
					params.addBodyParameter("设备ID", mTeamViewData.mIdText);
					log("用户ID=" + mTeamViewData.mPCIDTEXT + ";设备ID="
							+ mTeamViewData.mIdText);
					x.http().post(params,
							new Callback.CommonCallback<String>() {
								@Override
								public void onSuccess(String result) {
									log("success");
									InputStream sbs = new ByteArrayInputStream(
											result.getBytes());
									try {
										String response = XMLParse
												.parseResponseCheck(sbs);
										log("response=" + response);
										if (!response.equals("失败")) {
											mTeamViewData.BEGIN_TIME = WatchService
													.toTime2(response);
											log("mTeamViewData.BEGIN_TIME="
													+ mTeamViewData.BEGIN_TIME);
											// 写开始时间
											handler.removeMessages(BEGIN_BEGIN);
											handler.sendEmptyMessage(BEGIN_BEGIN);
										} else {
											if (mTeamViewData.rejectButton != null) {
												mTeamViewData.rejectButton
														.performAction(AccessibilityNodeInfo.ACTION_CLICK);
												// state = STATE_REJECT;
												Message msg = new Message();
												msg.what = UPDATE_ZHUANGTAI;
												msg.obj = (String) "空闲";
												handler.removeMessages(UPDATE_ZHUANGTAI);
												handler.sendMessage(msg);
											} else {
												log("rejectButton==null");
											}
										}
									} catch (Exception e) {
										log(e.getMessage());
									}

								}

								@Override
								public void onError(Throwable ex,
										boolean isOnCallback) {
									log("error" + ex.getMessage());
									Toast.makeText(x.app(), ex.getMessage(),
											Toast.LENGTH_LONG).show();
								}

								@Override
								public void onCancelled(CancelledException cex) {
									Toast.makeText(x.app(), "cancelled",
											Toast.LENGTH_LONG).show();
								}

								@Override
								public void onFinished() {

								}
							});

				}
			}
			break;
		case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
			eventText = "TYPE_NOTIFICATION_STATE_CHANGED";
			Notification notification = (Notification) event
					.getParcelableData();
			List<String> textList = getText(notification);
			if (null != textList && textList.size() > 0) {
				for (String text : textList) {
					if (!TextUtils.isEmpty(text)) {
						log("Notification=" + text);
					}
					if (!TextUtils.isEmpty(text) && text.contains("会话已结束")) {
						log("会话已结束Service");
						handler.removeMessages(SHUT_DOWN_CONNECTION);
						handler.sendEmptyMessage(SHUT_DOWN_CONNECTION);
						break;
					}
				}
			}
			break;
		case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
			eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_END";
			break;
		case AccessibilityEvent.TYPE_ANNOUNCEMENT:
			eventText = "TYPE_ANNOUNCEMENT";
			break;
		case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
			eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_START";
			break;
		case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
			eventText = "TYPE_VIEW_HOVER_ENTER";
			break;
		case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
			eventText = "TYPE_VIEW_HOVER_EXIT";
			break;
		case AccessibilityEvent.TYPE_VIEW_SCROLLED:
			eventText = "TYPE_VIEW_SCROLLED";
			break;
		case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
			eventText = "TYPE_VIEW_TEXT_SELECTION_CHANGED";
			break;
		case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
			eventText = "TYPE_WINDOW_CONTENT_CHANGED";
			log("event.getClassName().toString()="
					+ event.getClassName().toString());
			if (event.getClassName().toString().equals(GET_ID_CLASS)
					|| event.getClassName().toString().equals(FRAMELAYOUT)
					|| event.getClassName().toString()
							.equals("android.widget.EditText")) {
				// if (mTeamViewData.mIdText.equals("")) {// 为空才判断
				if (ReadyID(rowNode)) {
					state = STATE_GET_ID_SUCCESS;
					log("have id success=" + mTeamViewData.mIdText);
					User user = new User();
					user.setmIdText(mTeamViewData.mIdText);
					Dbmanager.saveUser(user);
				}
				// }
			}
			if (event.getClassName().toString()
					.equals("android.widget.FrameLayout")
					|| event.getClassName().toString().equals(GET_ID_CLASS)) {
				if (state == STATE_ALLOW) {
					log("connection ing");
					if (ReadyLoad(rowNode))// 连接成功
					{

					}
				}

			}
			break;
		}
		eventText = eventText + ":" + eventType;
		log(eventText, 1);
		log("=============END=====================", 1);
	}

	public boolean ReadyLoad(AccessibilityNodeInfo info) {

		if (info.getChildCount() == 0) {
			if (info.getText() != null
					&& info.getText().toString().contains("已经连接到您的设备")) {
				return true;
			}
		} else {
			for (int i = 0; i < info.getChildCount(); i++) {
				if (info.getChild(i) != null) {
					if (ReadyLoad(info.getChild(i))) {
						return true;
					}
					;
				}
			}
		}
		return false;
	}

	public boolean ReadyID(AccessibilityNodeInfo info) {
		if (info.getChildCount() == 0) {
			if (info != null
					&& info.getClassName().equals("android.widget.EditText")) {
				if (info.hashCode() == mTeamViewData.mIdTextHash) {
					if (!info.getText().toString().trim().equals("-")) {
						mTeamViewData.mIdText = info.getText().toString()
								.replace(" ", "").trim();
						return true;
					}
				}
			}
		} else {
			for (int i = 0; i < info.getChildCount(); i++) {
				if (info.getChild(i) != null) {
					if (ReadyID(info.getChild(i))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean ReadyPCID(AccessibilityNodeInfo info) {
		if (info.getChildCount() == 4) {
			AccessibilityNodeInfo child = info.getChild(0);
			if (child != null && child.getChildCount() == 0
					&& child.getText() != null
					&& child.getText().toString().trim().equals("允许远程支持吗？")) {
				String temp = info.getChild(1).getText().toString();
				int postion = temp.indexOf("to remote support");
				if (temp.contains("Would you like to allow")) {
					mTeamViewData.mPCIDTEXT = info.getChild(1).getText()
							.toString().substring(24, postion).replace(" ", "")
							.trim();
					mTeamViewData.rejectButton = info.getChild(2);
					mTeamViewData.allowButton = info.getChild(3);
					return true;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	public int findIDTextView(AccessibilityNodeInfo info) {
		if (info.getChildCount() == 0) {
			if (info != null
					&& info.getClassName().equals("android.widget.EditText")) {
				if (info.getText().toString().trim().equals("正在激活")) {
					state = STATE_BEGIN;
				}
				if (info.getText().toString().trim().equals("-")) {
					mTeamViewData.mIdTextHash = info.hashCode();
					return info.hashCode();
				}
			}
		} else {
			for (int i = 0; i < info.getChildCount(); i++) {
				if (info.getChild(i) != null) {
					int hash = findIDTextView(info.getChild(i));
					if (hash != 0) {
						return hash;
					}
				}
			}
		}
		return 0;
	}

	public void recycle(AccessibilityNodeInfo info) {
		if (info.getChildCount() == 0) {
			log("child widget----------------------------"
					+ info.getClassName(), 1);
			log("Text：" + info.getText(), 1);
			log("hash:=" + info.hashCode(), 1);
		} else {
			for (int i = 0; i < info.getChildCount(); i++) {
				if (info.getChild(i) != null) {
					recycle(info.getChild(i));
				}
			}
		}
	}

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub

	}

	public int getProcessPid() {
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> procList = null;
		int result = -1;
		procList = activityManager.getRunningAppProcesses();
		for (Iterator<RunningAppProcessInfo> iterator = procList.iterator(); iterator
				.hasNext();) {
			RunningAppProcessInfo procInfo = iterator.next();
			// log("processName=" + procInfo.processName);
			if (procInfo.processName
					.equals("com.teamviewer.quicksupport.market")) {
				mTeamViewData.pidId = procInfo.pid;
				log("get pid success=" + mTeamViewData.pidId);
				processWatcher = new ProcessWatcher(mTeamViewData.pidId,
						shutDownListener);
				processWatcher.start();
				break;
			}
		}
		return result;
	}

	public List<String> getText(Notification notification) {
		if (null == notification)
			return null;
		RemoteViews views = notification.contentView;
		if (views == null)
			return null;

		// Use reflection to examine the m_actions member of the given
		// RemoteViews object.
		// It's not pretty, but it works.
		List<String> text = new ArrayList<String>();
		try {
			Field field = views.getClass().getDeclaredField("mActions");
			field.setAccessible(true);

			@SuppressWarnings("unchecked")
			ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field
					.get(views);
			// Find the setText() and setTime() reflection actions
			for (Parcelable p : actions) {
				Parcel parcel = Parcel.obtain();
				p.writeToParcel(parcel, 0);
				parcel.setDataPosition(0);

				// The tag tells which type of action it is (2 is
				// ReflectionAction, from the source)
				int tag = parcel.readInt();
				if (tag != 2)
					continue;

				// View ID
				parcel.readInt();

				String methodName = parcel.readString();
				if (null == methodName) {
					continue;
				} else if (methodName.equals("setText")) {
					// Parameter type (10 = Character Sequence)
					parcel.readInt();

					// Store the actual string
					String t = TextUtils.CHAR_SEQUENCE_CREATOR
							.createFromParcel(parcel).toString().trim();
					text.add(t);
				}
				parcel.recycle();
			}
		} catch (Exception e) {
			log("getText" + e.getMessage());
		}

		return text;
	}

	/**
	 * 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm:ss
	 * 
	 * @param dateDate
	 * @return
	 */
	public static String toTime(String time) {
		Date date = null;
		SimpleDateFormat format1 = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS");
		try {
			date = format1.parse(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (date != null) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			String dateString = formatter.format(date);
			return dateString;
		} else {
			return "";
		}
	}

	public static String toTime2(String time) {
		Date date = null;
		String newTime = "";
		String[] aa = time.split(" ");
		if (aa.length == 2) {
			newTime = aa[0] + " " + aa[1];
		} else if (aa.length == 3) {
			newTime = aa[0] + " " + aa[2];
		}
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		try {
			date = format1.parse(newTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (date != null) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			String dateString = formatter.format(date);
			return dateString;
		} else {
			return "";
		}
	}

	public static boolean isTimeTRUE(String time) {
		try {
			if (Integer.parseInt(time.substring(0, 4)) > 2014) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}

	}

	public void timeErrorRestart() {
		// 重启
		handler.removeCallbacksAndMessages(null);
		log("kill killBackgroundProcesses");
		try {
			Process suProcess = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(
					suProcess.getOutputStream());
			os.writeBytes("adb shell" + "\n");
			os.flush();
			os.writeBytes("am force-stop " + paName + "\n");
			os.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log(e.getMessage());
		}
		if (processWatcher != null) {
			processWatcher.stop();
		}
		handler.removeMessages(UPDATE_STARTEAM);
		handler.sendEmptyMessageDelayed(UPDATE_STARTEAM, 1000 * 8);
	}
}
