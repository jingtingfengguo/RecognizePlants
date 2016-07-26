package com.app.recognizeplants;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;

import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class myWebClient extends WebViewClient {
	
	public static final String TAG = "MainActivity"; 
	private Context mContext;
	
	public myWebClient(Context context){
		super();
		mContext=context;
		
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		Log.d(TAG,"URL地址:" + url); 
		super.onPageStarted(view, url, favicon);
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		view.loadUrl(url);
		return true;

	}

	@Override
	public void onReceivedSslError(WebView view, SslErrorHandler handler,
			SslError error) {
		handler.proceed(); // 接受所有证书

	}

	@Override
	public void onPageFinished(WebView view, String url) {
		Log.i(TAG, "onPageFinished"); 
		super.onPageFinished(view, url);

	}
	
}