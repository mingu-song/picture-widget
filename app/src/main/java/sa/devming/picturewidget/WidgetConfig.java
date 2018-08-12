package sa.devming.picturewidget;

import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import sa.devming.picturewidget.database.PictureData;
import sa.devming.picturewidget.database.PictureDbHelper;

public class WidgetConfig extends AppCompatActivity {
    private static final String TAG = WidgetConfig.class.getSimpleName();
    private static final int PERMISSION_OK = 1;
    private static final int SELECT_IMAGE = 2;
    private static final int ADD_IMAGE = 3;
    public static final String WIDGET_ID_PARAM = "WIDGET_ID";
    private final int MAX_COUNT = 127;

    private int mAppWidgetId;
    private int mAppWidgetIdUpdate;

    private TextView mConfigSelectImg;

    private ImageButton mConfigOK;

    private PictureDbHelper mDBHelper;

    private ArrayList<Uri> mImageList;

    private ImageAdapter mImageAdapter;
    private GridView mGridView;

    private ImageButton mAddBT, mSelBT, mDelBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_layout);

        initializeViews();
        //광고 추가
        adMob();
        // 권한확인
        checkAllowPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "==================================================================");
        if (mDBHelper == null) {
            mDBHelper = new PictureDbHelper(this);
        }
        ArrayList<Uri> uriList = mDBHelper.getAllDB();
        if (uriList != null && uriList.size() > 0) {
            Log.e(TAG, "take all uri permission. size() = "+uriList.size() );
            for (Uri uri : uriList) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
        Log.e(TAG, "==================================================================");
    }

    private void initializeViews() {
        mDBHelper = new PictureDbHelper(this);
        mImageList = new ArrayList();
        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mAppWidgetIdUpdate = AppWidgetManager.INVALID_APPWIDGET_ID;

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mAppWidgetId = bundle.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            mAppWidgetIdUpdate = bundle.getInt(WIDGET_ID_PARAM, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && mAppWidgetIdUpdate == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        } else if (mAppWidgetIdUpdate != AppWidgetManager.INVALID_APPWIDGET_ID) {
            //업데이트를 위한 조회
            mImageList = mDBHelper.getAllPictureDate(mAppWidgetIdUpdate);
        }

        mConfigSelectImg = findViewById(R.id.configSelectImg);
        mConfigOK =  findViewById(R.id.configOK);
        mConfigOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configFinish();
            }
        });
        mGridView = findViewById(R.id.gridView);
        mImageAdapter = new ImageAdapter(this);
        mGridView.setAdapter(mImageAdapter);

        mSelBT = findViewById(R.id.configSelBT);
        mAddBT = findViewById(R.id.configAddBT);
        mDelBT = findViewById(R.id.configDelBT);

        mSelBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImageList(SELECT_IMAGE);
            }
        });
        mAddBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImageList(ADD_IMAGE);
            }
        });
        mDelBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delImageList();
            }
        });
        mAddBT.setEnabled(false);
        mDelBT.setEnabled(false);

        if (mAppWidgetIdUpdate != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setTextAndAlarm();
        }
    }

    private void configFinish() {
        if (mImageList == null || mImageList.size() == 0 ){
            Toast.makeText(this,getString(R.string.config_miss),Toast.LENGTH_SHORT).show();
            return;
        }

        //업데이트라면 기존 데이터 삭제 처리 (취소를 위하여 여기서 처리)
        if (mAppWidgetIdUpdate != AppWidgetManager.INVALID_APPWIDGET_ID) {
            mDBHelper.deletePictureData(mAppWidgetIdUpdate);
            mAppWidgetId = mAppWidgetIdUpdate; // 이후는 기존 변수 사용
        }

        //DB에 저장
        for (int i=0 ; i<mImageList.size() ; i++) {
            PictureData pictureData = new PictureData(mAppWidgetId, i, mImageList.get(i).toString());
            mDBHelper.addPictureData(pictureData);
        }

        //위젯 업데이트 처리
        Intent intent = new Intent(WidgetProvider.ACTION_CLICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        sendBroadcast(intent);

        Intent intentFinish = new Intent();
        intentFinish.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, intentFinish);
        finish();
    }

    private void checkAllowPermissions() {
        if (PermissionUtil.checkPermissions(this, PermissionUtil.PERMISSIONS[0])) {
            // 권한획득
        } else {
            PermissionUtil.requestExternalPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionUtil.REQUEST_CODE) {
            if (PermissionUtil.verifyPermission(grantResults)) {
                // 권한획득
            } else {
                Toast.makeText(this, getString(R.string.need_permissions), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void getImageList(int action) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent,getString(R.string.chooser_string)), action);
    }

    private void delImageList() {
        boolean[] isChecked = mImageAdapter.getIsCheck();
        for(int i=isChecked.length-1 ; i>=0 ; i--){
            if (isChecked[i]){
                mImageList.remove(i);
            }
        }
        setTextAndAlarm();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean isExistError = false;
        try {
            // When an Image is picked
            if ((requestCode == SELECT_IMAGE || requestCode == ADD_IMAGE) && resultCode == RESULT_OK && data != null) {
                if (requestCode == SELECT_IMAGE) {
                    mImageList.clear();
                }
                //persist uri permission
                final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;

                if (data.getData() != null) {
                    Uri uri = Uri.parse(data.getData().toString());
                    try {
                        //기존저장 이미지 개수 + 현재 개수 + 1 = 127 이상인지 확인
                        if (mDBHelper.getAllCount(mAppWidgetIdUpdate) + mImageList.size() + 1 > MAX_COUNT) {
                            Toast.makeText(this, getString(R.string.max_load_image), Toast.LENGTH_LONG).show();
                            return;
                        }
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        mImageList.add(uri);
                    } catch (SecurityException e) {
                        Log.e(this.getLocalClassName(), e.getMessage());
                        isExistError = true;
                    }
                } else {
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        //기존저장 이미지 개수 + 현재 개수 + clipData.getItemCount() = 127 이상인지 확인
                        if (mDBHelper.getAllCount(mAppWidgetIdUpdate) + mImageList.size() + clipData.getItemCount() > MAX_COUNT) {
                            Toast.makeText(this, getString(R.string.max_load_image), Toast.LENGTH_LONG).show();
                            return;
                        }
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = Uri.parse(item.getUri().toString());
                            try {
                                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                                mImageList.add(uri);
                            } catch (SecurityException e) {
                                Log.e(this.getLocalClassName(), e.getMessage());
                                isExistError = true;
                            }
                        }
                    }
                }
            }
            setTextAndAlarm();
            if (isExistError) {
                Toast.makeText(this, getString(R.string.no_load_image), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setTextAndAlarm() {
        if (mImageList != null && mImageList.size() > 0) {
            mImageAdapter.setImageList(mImageList);
            mImageAdapter.notifyDataSetChanged();
            mConfigSelectImg.setText(String.format(getString(R.string.select_picture_cnt),mImageList.size()));
            mAddBT.setEnabled(true);
            mDelBT.setEnabled(true);
        } else {
            if (mImageList != null) {
                mImageAdapter.setImageList(mImageList);
                mImageAdapter.notifyDataSetChanged();
            }
            mConfigSelectImg.setText(R.string.config_img);
            Toast.makeText(this, getString(R.string.not_select_picture), Toast.LENGTH_SHORT).show();
            mAddBT.setEnabled(false);
            mDelBT.setEnabled(false);
        }
    }

    private void adMob(){
        AdView mAdView = findViewById(R.id.adView);
        Bundle extras = new Bundle();
        extras.putString("max_ad_content_rating", "G");
        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
        mAdView.loadAd(adRequest);
    }
}
