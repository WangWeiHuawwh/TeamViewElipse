package com.example.teamview;

import android.os.Build;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;

public class ProcessWatcher {
	private FileObserver mFileObserver;
	private final String mPath;
	private final File mFile;
	private final ShutDownListener shutDownLister;

	public ProcessWatcher(int pid, ShutDownListener shutDownListern) {
		mPath = "/proc/" + pid;
		mFile = new File(mPath);
		this.shutDownLister = shutDownListern;
	}

	public void start() {
		if (mFileObserver == null) {
			mFileObserver = new MyFileObserver(mPath,
					FileObserver.CLOSE_NOWRITE);
		}
		mFileObserver.startWatching();
	}

	public void stop() {
		if (mFileObserver != null) {
			mFileObserver.stopWatching();
		}
	}

	private final class MyFileObserver extends FileObserver {
		private final Object mWaiter = new Object();

		public MyFileObserver(String path, int mask) {
			super(path, mask);
		}

		@Override
		public void onEvent(int event, String path) {
			if ((event & FileObserver.CLOSE_NOWRITE) == FileObserver.CLOSE_NOWRITE) {
				try {
					synchronized (mWaiter) {
						mWaiter.wait(3000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!mFile.exists()) {
					Log.d("DemoLog", "shutdown ProcessWatcher");
					shutDownLister.shutDown(true);
					stopWatching();
				}
			}
		}
	}
}