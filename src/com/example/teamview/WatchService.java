package com.example.teamview;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
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
	private TeamViewData mTeamViewData = new TeamViewData();
	private static final String FRAMELAYOUT = "android.widget.FrameLayout";
	private PowerManager.WakeLock mWakeLock;
	public static volatile int state = STATE_BEGIN;
	public static final int UPDATE_TIME = 1;
	public static final int SHUT_DOWN_TEAM = 2;// 结束进程
	public static final int SHUT_DOWN_CONNECTION = 3;// 结束连接
	public static final int UPDATE_ZHUANGTAI = 4;
	public static final int UPDATE_SHUTDOWN_ZHUANGTAI = 5;
	public static final int UPDATE_TIME_TIME = 30 * 1000;
	public static final int SHUT_DOWN_TEAM_TIME = 3600 * 1000;
	public ProcessWatcher processWatcher;
	public volatile int zhuangtaiTimes = 0;
	public volatile int shutDownTimes = 0;
	public static final int TIMES = 5;
	public static final int TRY_TIME = 2 * 1000;
	public ShutDownListener shutDownListener = new ShutDownListener() {

		@Override
		public void shutDown(boolean is) {
			// TODO Auto-generated method stub
			log("TeamView进程结束");
			handler.removeMessages(SHUT_DOWN_TEAM);
			handler.sendEmptyMessage(SHUT_DOWN_TEAM);
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
				RequestParams timeparams = new RequestParams(
						UrlData.URL_GET_TIME);
				x.http().post(timeparams, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						String response = "";
						try {
							response = XMLParse.parseResponseCheck(sbs);
							log("获取时间 response=" + response);
						} catch (Exception e) {

						}
						if (response.equals("")) {
							return;
						}
						RequestParams params = new RequestParams(
								UrlData.URL_UPDATE_TIME);
						params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
						params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
						params.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
						params.addBodyParameter("设备ID", mTeamViewData.mIdText);
						params.addBodyParameter("连接判断", toTime(response));
						params.addBodyParameter("使用时间",
								mTeamViewData.BEGIN_TIME);
						log("用户ID=" + mTeamViewData.mPCIDTEXT + ";设备ID="
								+ mTeamViewData.mIdText + "连接判断="
								+ toTime(response) + "使用时间="
								+ mTeamViewData.BEGIN_TIME);
						x.http().post(params, new CommonCallback<String>() {
							@Override
							public void onSuccess(String result) {
								InputStream sbs = new ByteArrayInputStream(
										result.getBytes());
								try {
									String response = XMLParse
											.parseResponseCheck(sbs);
									log("连接判断 response=" + response);
									if (response.equals("成功")) {
										handler.removeMessages(UPDATE_TIME);
										handler.sendEmptyMessageDelayed(
												UPDATE_TIME, UPDATE_TIME_TIME);
									} else {
										handler.removeMessages(UPDATE_TIME);
										handler.sendEmptyMessageDelayed(
												UPDATE_TIME, UPDATE_TIME_TIME);
									}
								} catch (Exception e) {
								}

							}

							@Override
							public void onError(Throwable ex,
									boolean isOnCallback) {
								log("连接判断 error" + ex.getMessage());

							}

							@Override
							public void onCancelled(CancelledException cex) {

							}

							@Override
							public void onFinished() {

							}
						});
					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("get time error=" + ex.getMessage());

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
				state = STATE_BEGIN;
				mTeamViewData.pidId = 0;
				break;
			case SHUT_DOWN_CONNECTION:
				state = STATC_CONNECTION_OVER;
				mTeamViewData.pidId = 0;
				handler.removeCallbacksAndMessages(null);
				RequestParams timeparams2 = new RequestParams(
						UrlData.URL_GET_TIME);
				x.http().post(timeparams2, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						String response = "";
						try {
							response = XMLParse.parseResponseCheck(sbs);
							log("获取时间 response=" + response);
						} catch (Exception e) {

						}
						if (response.equals("")) {
							return;
						}
						RequestParams params = new RequestParams(
								UrlData.URL_UPDATE_TIME_END);
						params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
						params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
						params.addBodyParameter("用户ID", mTeamViewData.mPCIDTEXT);
						params.addBodyParameter("设备ID", mTeamViewData.mIdText);
						params.addBodyParameter("结束时间", toTime(response));
						params.addBodyParameter("使用时间",
								mTeamViewData.BEGIN_TIME);
						log("用户ID=" + mTeamViewData.mPCIDTEXT + ";设备ID="
								+ mTeamViewData.mIdText + "结束时间="
								+ toTime(response) + "使用时间="
								+ mTeamViewData.BEGIN_TIME);
						x.http().post(params, new CommonCallback<String>() {
							@Override
							public void onSuccess(String result) {
								InputStream sbs = new ByteArrayInputStream(
										result.getBytes());
								try {
									String response = XMLParse
											.parseResponseCheck(sbs);
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
							public void onError(Throwable ex,
									boolean isOnCallback) {
								log("结束时间 error" + ex.getMessage());

							}

							@Override
							public void onCancelled(CancelledException cex) {

							}

							@Override
							public void onFinished() {

							}
						});
					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("get time error=" + ex.getMessage());

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
				RequestParams timeparams3 = new RequestParams(
						UrlData.URL_GET_TIME);
				x.http().post(timeparams3, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						String response = "";
						try {
							response = XMLParse.parseResponseCheck(sbs);
							log("获取时间 response2=" + response);
						} catch (Exception e) {

						}
						if (response.equals("")) {
							return;
						}

						log("连接状态=" + zhuangtai);
						RequestParams params = new RequestParams(
								UrlData.URL_UPDATE_ZHUANGTAI);
						params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
						params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
						params.addBodyParameter("连接状态", zhuangtai);
						params.addBodyParameter("设备ID", mTeamViewData.mIdText);
						params.addBodyParameter("同步时间", toTime(response));
						log("连接状态=" + zhuangtai + ";设备ID="
								+ mTeamViewData.mIdText + "同步时间="
								+ toTime(response));
						x.http().post(params, new CommonCallback<String>() {
							@Override
							public void onSuccess(String result) {
								InputStream sbs = new ByteArrayInputStream(
										result.getBytes());
								try {
									String response = XMLParse
											.parseResponseCheck(sbs);
									log("更新状态 response=" + response);
									if (response.equals("成功")) {
										zhuangtaiTimes = 0;
									} else {
										if (zhuangtaiTimes < TIMES) {
											Message msg = new Message();
											msg.what = UPDATE_ZHUANGTAI;
											msg.obj = zhuangtai;
											handler.removeMessages(UPDATE_ZHUANGTAI);
											handler.sendMessageDelayed(msg,
													TRY_TIME);
											zhuangtaiTimes++;
										}
									}
								} catch (Exception e) {
									log("ZHUANGTAI=" + e.getMessage());
								}

							}

							@Override
							public void onError(Throwable ex,
									boolean isOnCallback) {
								log("更新状态 error" + ex.getMessage());

							}

							@Override
							public void onCancelled(CancelledException cex) {

							}

							@Override
							public void onFinished() {

							}
						});
					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("get time error2=" + ex.getMessage());

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
				RequestParams timeparams4 = new RequestParams(
						UrlData.URL_GET_TIME);
				x.http().post(timeparams4, new CommonCallback<String>() {
					@Override
					public void onSuccess(String result) {
						InputStream sbs = new ByteArrayInputStream(result
								.getBytes());
						String response = "";
						try {
							response = XMLParse.parseResponseCheck(sbs);
							log("获取时间 response4=" + response);
						} catch (Exception e) {

						}
						if (response.equals("")) {
							return;
						}
						RequestParams params = new RequestParams(
								UrlData.URL_SHUT_DOWN_ZHUANGTAI);
						params.addBodyParameter("授权用户", UrlData.ADMIN_UID);
						params.addBodyParameter("密码", UrlData.ADMIN_PASSWORD);
						params.addBodyParameter("设备ID", mTeamViewData.mIdText);
						params.addBodyParameter("同步时间", toTime(response));
						log("设备ID=" + mTeamViewData.mIdText + "同步时间="
								+ toTime(response));
						x.http().post(params, new CommonCallback<String>() {
							@Override
							public void onSuccess(String result) {
								InputStream sbs = new ByteArrayInputStream(
										result.getBytes());
								try {
									String response = XMLParse
											.parseResponseCheck(sbs);
									log("更新结束信息 response=" + response);
									if (response.equals("成功")) {
										shutDownTimes = 0;
										mTeamViewData.mPCIDTEXT = "";
									} else {
										if (shutDownTimes < TIMES) {
											handler.removeMessages(UPDATE_SHUTDOWN_ZHUANGTAI);
											handler.sendEmptyMessageDelayed(
													UPDATE_SHUTDOWN_ZHUANGTAI,
													TRY_TIME);
											shutDownTimes++;
										}
									}
								} catch (Exception e) {
								}

							}

							@Override
							public void onError(Throwable ex,
									boolean isOnCallback) {
								log("更新结束信息 error" + ex.getMessage());

							}

							@Override
							public void onCancelled(CancelledException cex) {

							}

							@Override
							public void onFinished() {

							}
						});
					}

					@Override
					public void onError(Throwable ex, boolean isOnCallback) {
						log("get time error2=" + ex.getMessage());

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

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mWakeLock != null) {
			mWakeLock.release();
		}
		if(!TextUtils.isEmpty(mTeamViewData.mIdText)&&!TextUtils.isEmpty(mTeamViewData.mPCIDTEXT)){
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
				public void onError(Throwable ex,
						boolean isOnCallback) {
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
	}

	private LogWriter mLogWriter;

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		mWakeLock.acquire();
		File logf = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "DemoLog.txt");

		try {
			mLogWriter = LogWriter.open(logf.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log(e.getMessage());
		}
		log("onCreate()");

	}

	public void log(String msg) {
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
			log("noteInfo is　null");
			return;
		} else {
			log("class=" + event.getClassName().toString());
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
		log("==============Start====================");
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
					state = STATE_PC_ID_SUCCESS;
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
											if (mTeamViewData.allowButton != null) {
												mTeamViewData.allowButton
														.performAction(AccessibilityNodeInfo.ACTION_CLICK);
												state = STATE_ALLOW;
											} else {
												log("allowButton==null");
											}
										} else {
											if (mTeamViewData.rejectButton != null) {
												mTeamViewData.rejectButton
														.performAction(AccessibilityNodeInfo.ACTION_CLICK);
												state = STATE_REJECT;
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
					|| event.getClassName().toString().equals(FRAMELAYOUT)) {
				if (mTeamViewData.mIdText.equals("")) {// 为空才判断
					if (ReadyID(rowNode)) {
						state = STATE_GET_ID_SUCCESS;
						log("have id success=" + mTeamViewData.mIdText);
					}
				}
			}
			if (event.getClassName().toString()
					.equals("android.widget.FrameLayout")
					|| event.getClassName().toString().equals(GET_ID_CLASS)) {
				if (state == STATE_ALLOW) {
					log("connection ing");
					if (ReadyLoad(rowNode))// 连接成功
					{
						state = STATC_CONNECTION_SUCCESS;
						log("connection success");
						processWatcher = new ProcessWatcher(
								mTeamViewData.pidId, shutDownListener);
						processWatcher.start();
						RequestParams timeparams = new RequestParams(
								UrlData.URL_GET_TIME);
						x.http().post(timeparams,
								new Callback.CommonCallback<String>() {
									@Override
									public void onSuccess(String result) {
										InputStream sbs = new ByteArrayInputStream(
												result.getBytes());
										String response = "";
										try {
											response = XMLParse
													.parseResponseCheck(sbs);
											log("获取时间 response=" + response);
										} catch (Exception e) {

										}
										if (response.equals("")) {
											return;
										}
										RequestParams params = new RequestParams(
												UrlData.URL_BEGIN_CONNECT);
										params.addBodyParameter("授权用户",
												UrlData.ADMIN_UID);
										params.addBodyParameter("密码",
												UrlData.ADMIN_PASSWORD);
										params.addBodyParameter("用户ID",
												mTeamViewData.mPCIDTEXT);
										params.addBodyParameter("设备ID",
												mTeamViewData.mIdText);
										params.addBodyParameter("开始时间",
												toTime(response));
										params.addBodyParameter("使用时间",
												mTeamViewData.BEGIN_TIME);
										log("用户ID=" + mTeamViewData.mPCIDTEXT
												+ ";设备ID="
												+ mTeamViewData.mIdText
												+ "开始时间=" + toTime(response)
												+ "使用时间="
												+ mTeamViewData.BEGIN_TIME);
										x.http()
												.post(params,
														new Callback.CommonCallback<String>() {
															@Override
															public void onSuccess(
																	String result) {
																InputStream sbs = new ByteArrayInputStream(
																		result.getBytes());
																try {
																	String response = XMLParse
																			.parseResponseCheck(sbs);
																	Log.d("DemoLog",
																			"开始时间 response="
																					+ response);
																	if (response
																			.equals("成功")) {
																		Message msg = new Message();
																		msg.what = UPDATE_ZHUANGTAI;
																		msg.obj = (String) "忙碌";
																		handler.removeMessages(UPDATE_ZHUANGTAI);
																		handler.sendMessage(msg);
																		handler.removeMessages(UPDATE_TIME);
																		handler.sendEmptyMessageDelayed(
																				UPDATE_TIME,
																				UPDATE_TIME_TIME);
																		handler.removeMessages(SHUT_DOWN_TEAM);
																		handler.sendEmptyMessageDelayed(
																				SHUT_DOWN_TEAM,
																				SHUT_DOWN_TEAM_TIME);
																		Intent i = new Intent(
																				Intent.ACTION_MAIN);
																		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
																		i.addCategory(Intent.CATEGORY_HOME);
																		startActivity(i);
																	}
																} catch (Exception e) {
																	log(e.getMessage());
																}

															}

															@Override
															public void onError(
																	Throwable ex,
																	boolean isOnCallback) {
																Log.d("DemoLog",
																		"开始时间 error"
																				+ ex.getMessage());

															}

															@Override
															public void onCancelled(
																	CancelledException cex) {

															}

															@Override
															public void onFinished() {

															}
														});
									}

									@Override
									public void onError(Throwable ex,
											boolean isOnCallback) {
										Log.d("DemoLog",
												"get time error="
														+ ex.getMessage());

									}

									@Override
									public void onCancelled(
											CancelledException cex) {

									}

									@Override
									public void onFinished() {

									}
								});
					}
				}

			}
			break;
		}
		eventText = eventText + ":" + eventType;
		log(eventText);
		log("=============END=====================");
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
					+ info.getClassName());
			log("Text：" + info.getText());
			log("hash:=" + info.hashCode());
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
}
