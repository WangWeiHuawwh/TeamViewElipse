package com.example.teamview;

import org.xutils.x;
import org.xutils.common.Callback;
import org.xutils.common.Callback.CancelledException;
import org.xutils.http.RequestParams;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public static void getid(){
    	 RequestParams params = new RequestParams("http://www.baidu.com/");
    	    //params.addQueryStringParameter("wd", "xUtils");
    	    x.http().get(params, new Callback.CommonCallback<String>() {
    	        @Override
    	        public void onSuccess(String result) {
    	            Toast.makeText(x.app(), result, Toast.LENGTH_LONG).show();
    	        }

    	        @Override
    	        public void onError(Throwable ex, boolean isOnCallback) {
    	        	Log.d("DemoLog", "error"+ex.getMessage());
    	            Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
    	        }

    	        @Override
    	        public void onCancelled(CancelledException cex) {
    	            Toast.makeText(x.app(), "cancelled", Toast.LENGTH_LONG).show();
    	        }

    	        @Override
    	        public void onFinished() {

    	        }
    	    });

    }
}
