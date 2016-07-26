package com.app.recognizeplants;

import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private WebView webView;
	private ValueCallback<Uri> mUploadMessage;
	private final static int FILECHOOSER_RESULTCODE = 1;
	private static final int REQ_CAMERA = FILECHOOSER_RESULTCODE + 1;
	private static final int REQ_CHOOSE = REQ_CAMERA + 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		webView = (WebView) findViewById(R.id.web_view);
		webView = new WebView(this);
		webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl("http://stu.iplant.cn/web");
		webView.setWebViewClient(new myWebClient(this));
		webView.setWebChromeClient(new WebChromeClient() {
			// For Android 3.0+
			public void openFileChooser(ValueCallback<Uri> uploadMsg,
					String acceptType) {
				Toast.makeText(MainActivity.this, "请选择打开相机或者相册",
						Toast.LENGTH_SHORT).show();
				if (mUploadMessage != null)
					return;
				mUploadMessage = uploadMsg;
				selectImage();
				// Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				// i.addCategory(Intent.CATEGORY_OPENABLE);
				// i.setType("*/*");
				// startActivityForResult( Intent.createChooser( i,
				// "File Chooser" ), FILECHOOSER_RESULTCODE );
			}

			// For Android < 3.0
			public void openFileChooser(ValueCallback<Uri> uploadMsg) {
				openFileChooser(uploadMsg, "");
			}

			// For Android > 4.1.1
			public void openFileChooser(ValueCallback<Uri> uploadMsg,
					String acceptType, String capture) {
				Toast.makeText(MainActivity.this, "请选择打开相机或者相册",
						Toast.LENGTH_SHORT).show();
				openFileChooser(uploadMsg, acceptType);
			}
			
		});
		
		setContentView(webView);

	}
	
	
	/**
	 * 检查SD卡是否存在
	 * 
	 * @return
	 */
	public final boolean checkSDcard() {
		boolean flag = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		if (!flag) {
			Toast.makeText(this, "请插入手机存储卡再使用本功能",Toast.LENGTH_SHORT).show();
		}
		return flag;
	}
	String compressPath = "";
	
	protected final void selectImage() {
		if (!checkSDcard())
			return;
		String[] selectPicTypeStr = { "相机","相册" };
		new AlertDialog.Builder(this)
				.setItems(selectPicTypeStr,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								// 相机拍摄
								case 0:
									openCarcme();
									break;
								// 手机相册
								case 1:
									chosePic();
									break;
								default:
									break;
								}
								compressPath = Environment
										.getExternalStorageDirectory()
										.getPath()
										+ "/fuiou_wmp/temp";
								new File(compressPath).mkdirs();
								compressPath = compressPath + File.separator
										+ "compress.jpg";
							}
						}).show();
	}
	
	String imagePaths;
	Uri  cameraUri;
	private Uri originalUri;
	private Cursor cursor;
	/**
	 * 打开照相机
	 */
	private void openCarcme() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		imagePaths = Environment.getExternalStorageDirectory().getPath()
				+ "/fuiou_wmp/temp/"
				+ (System.currentTimeMillis() + ".jpg");
		// 必须确保文件夹路径存在，否则拍照后无法完成回调
		File vFile = new File(imagePaths);
		if (!vFile.exists()) {
			File vDirPath = vFile.getParentFile();
			vDirPath.mkdirs();
		} else {
			if (vFile.exists()) {
				vFile.delete();
			}
		}
		cameraUri = Uri.fromFile(vFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
		startActivityForResult(intent, REQ_CAMERA);
	}

	/**
	 * 拍照结束后
	 */
	private void afterOpenCamera() {
		File f = new File(imagePaths);
		addImageGallery(f);
		try {
			
			File newFile = FileUtils.compressFile(f.getPath(), compressPath);
		} catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(this, "关闭相机",Toast.LENGTH_SHORT).show();
		}
	}

	/** 解决拍照后在相册中找不到的问题 */
	private void addImageGallery(File file) {
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	/**
	 * 本地相册选择图片
	 */
	private void chosePic() {
		FileUtils.delFile(compressPath);
		Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); // "android.intent.action.GET_CONTENT"
		String IMAGE_UNSPECIFIED = "image/*";
		innerIntent.setType(IMAGE_UNSPECIFIED); // 查看类型
		Intent wrapperIntent = Intent.createChooser(innerIntent, null);
		startActivityForResult(wrapperIntent, REQ_CHOOSE);
	}

	/**
	 * 选择照片后结束
	 * 
	 * @param data
	 */
	private Uri afterChosePic(Intent data) {
		try {
			originalUri = data.getData();
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = managedQuery(originalUri, proj, null, null, null);
			
		} catch (Exception e) {
			// TODO: handle exception
			originalUri = null;
			return null;
		}
		// 获取图片的路径：
		if(cursor == null ){
			Toast.makeText(this, "cursor空",Toast.LENGTH_SHORT).show();
			return null;
		}
		// 按我个人理解 这个是获得用户选择的图片的索引值
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		// 将光标移至开头 ，这个很重要，不小心很容易引起越界
		cursor.moveToFirst();
		// 最后根据索引值获取图片路径
		String path = cursor.getString(column_index);
		if(path != null && (path.endsWith(".png")||path.endsWith(".PNG")||path.endsWith(".jpg")||path.endsWith(".JPG"))){
			File newFile = FileUtils.compressFile(path, compressPath);
			return Uri.fromFile(newFile);
		}else{
//			Toast.makeText(this, "上传的图片仅支持png或jpg格式",Toast.LENGTH_SHORT).show();
		}
		return originalUri;
	}



	/**
	 * 返回文件选择
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
//		if (requestCode == FILECHOOSER_RESULTCODE) {
//			if (null == mUploadMessage)
//				return;
//			Uri result = intent == null || resultCode != RESULT_OK ? null
//					: intent.getData();
//			mUploadMessage.onReceiveValue(result);
//			mUploadMessage = null;
//		}
		
		if (null == mUploadMessage)
			return;
		Uri uri = null;
		if(requestCode == REQ_CAMERA ){
			
			uri = cameraUri;
			afterOpenCamera();
		}else if(requestCode == REQ_CHOOSE){
			uri = afterChosePic(intent);
		}
		mUploadMessage.onReceiveValue(uri);
		mUploadMessage = null;
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView.canGoBack()) {
				webView.goBack();
				return true;
			} else {
				System.exit(0);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
