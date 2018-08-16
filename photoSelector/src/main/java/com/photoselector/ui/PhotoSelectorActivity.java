package com.photoselector.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.photoselector.R;
import com.photoselector.domain.PhotoSelectorDomain;
import com.photoselector.model.AlbumModel;
import com.photoselector.model.PhotoModel;
import com.photoselector.ui.PhotoItem.onItemClickListener;
import com.photoselector.ui.PhotoItem.onPhotoItemCheckedListener;
import com.photoselector.util.AnimationUtil;
import com.photoselector.util.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoSelectorActivity extends Activity implements onItemClickListener, onPhotoItemCheckedListener,
        OnItemClickListener, OnClickListener {

    public static final int REQUEST_PHOTO = 0;
    private static final int REQUEST_CAMERA = 1;

    public static final String RECCENT_PHOTO = "最近照片";
    public static PhotoSelectorActivity instance = null;

    private GridView gvPhotos;
    private ListView lvAblum;
    private Button btnOk;
    private TextView tvAlbum, tvPreview, tvTitle;
    private PhotoSelectorDomain photoSelectorDomain;
    private PhotoSelectorAdapter photoAdapter;
    private AlbumAdapter albumAdapter;
    private RelativeLayout layoutAlbum;
    private ArrayList<PhotoModel> selected;
    private String code = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        setContentView(R.layout.activity_photoselector);
        instance = this;
        //隐藏状态栏
        fullscreenActivity(true);

        DisplayImageOptions options = new DisplayImageOptions.Builder() //
                .considerExifParams(true) // 调整图片方向
                .resetViewBeforeLoading(true) // 载入之前重置ImageView
                .showImageOnLoading(R.drawable.ic_picture_loading) // 载入时图片设置为黑色
                .showImageOnFail(R.drawable.ic_picture_loadfailed) // 加载失败时显示的图片
                .delayBeforeLoading(0) // 载入之前的延迟时间
                .build(); //
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).defaultDisplayImageOptions(options).memoryCacheExtraOptions(480, 800).threadPoolSize(5).build();
        ImageLoader.getInstance().init(config);

        try {
            code = getIntent().getStringExtra("code");
        } catch (Exception e) {
            code = "";
        }
        photoSelectorDomain = new PhotoSelectorDomain(getApplicationContext());
        selected = new ArrayList<PhotoModel>();
        tvTitle = (TextView) findViewById(R.id.tv_title_lh);
        gvPhotos = (GridView) findViewById(R.id.gv_photos_ar);
        lvAblum = (ListView) findViewById(R.id.lv_ablum_ar);
        btnOk = (Button) findViewById(R.id.btn_right_lh);
        tvAlbum = (TextView) findViewById(R.id.tv_album_ar);
        tvPreview = (TextView) findViewById(R.id.tv_preview_ar);
        layoutAlbum = (RelativeLayout) findViewById(R.id.layout_album_ar);

        btnOk.setOnClickListener(this);
        tvAlbum.setOnClickListener(this);
        tvPreview.setOnClickListener(this);

        photoAdapter = new PhotoSelectorAdapter(getApplicationContext(), new ArrayList<PhotoModel>(),
                CommonUtils.getWidthPixels(this), this, this, this);
        gvPhotos.setAdapter(photoAdapter);

        albumAdapter = new AlbumAdapter(getApplicationContext(), new ArrayList<AlbumModel>());
        lvAblum.setAdapter(albumAdapter);
        lvAblum.setOnItemClickListener(this);

        findViewById(R.id.bv_back_lh).setOnClickListener(this); // 返回

        photoSelectorDomain.getReccent(reccentListener); // 更新最近照片
        photoSelectorDomain.updateAlbum(albumListener); // 跟新相册信息
    }

    public int getCount() {
        return selected.size();
    }

    public boolean isMore() {

        if (code != null && code.trim().equals("can't_choose_more")) {
            return false;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_right_lh)
            if (!instance.isMore() && instance.getCount() > 1) {
                Toast.makeText(instance, "仅可选取一张图片!", Toast.LENGTH_SHORT).show();
            } else {
                ok(); // 选完照片
            }
        else if (v.getId() == R.id.tv_album_ar)
            album();
        else if (v.getId() == R.id.tv_preview_ar)
            priview();
        else if (v.getId() == R.id.tv_camera_vc)
            catchPicture();
        else if (v.getId() == R.id.bv_back_lh)
            finish();
    }

    private String capturePath = "";

    /**
     * 拍照
     */
    private void catchPicture() {

        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            String cameraPath = Environment.getExternalStorageDirectory() + "/fang_qian";
            // 新建目录
            File dir = new File(cameraPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            capturePath = cameraPath + System.currentTimeMillis() + ".jpg";
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(capturePath)));
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(getApplicationContext(), "请确认已经插入SD卡", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            //Uri uri = Uri.parse(capturePath);

            //PhotoModel photoModel = new PhotoModel(CommonUtils.query(getApplicationContext(), uri));
            PhotoModel photoModel = new PhotoModel(capturePath);
            selected.clear();
            selected.add(photoModel);
            ok();
        }
    }

    /**
     * 完成
     */
    private void ok() {
        if (selected.isEmpty()) {
            setResult(RESULT_CANCELED);
        } else {
            Intent data = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable("photos", selected);
            data.putExtras(bundle);
            setResult(RESULT_OK, data);
        }

        //显示状态栏，Activity不全屏显示(恢复到有状态的正常情况)
        fullscreenActivity(false);
        PhotoSelectorActivity.instance = null;
        finish();
    }

    /**
     * 预览照片
     */
    private void priview() {
        Bundle bundle = new Bundle();
        bundle.putSerializable("photos", selected);
        CommonUtils.launchActivity(this, PhotoPreviewActivity.class, bundle);
    }

    private void album() {
        if (layoutAlbum.getVisibility() == View.GONE) {
            popAlbum();
        } else {
            hideAlbum();
        }
    }

    /**
     * 弹出相册列表
     */
    private void popAlbum() {
        layoutAlbum.setVisibility(View.VISIBLE);
        new AnimationUtil(getApplicationContext(), R.anim.translate_up_current).setLinearInterpolator().startAnimation(
                layoutAlbum);
    }

    /**
     * 隐藏相册列表
     */
    private void hideAlbum() {
        new AnimationUtil(getApplicationContext(), R.anim.translate_down).setLinearInterpolator().startAnimation(
                layoutAlbum);
        layoutAlbum.setVisibility(View.GONE);
    }

    /**
     * 清空选中的图片
     */
    private void reset() {
        selected.clear();
        tvPreview.setText("预览");
        tvPreview.setEnabled(false);
    }

    @Override
    /** 点击查看照片 */
    public void onItemClick(int position) {
        Bundle bundle = new Bundle();
        if (tvAlbum.getText().toString().equals(RECCENT_PHOTO))
            bundle.putInt("position", position - 1);
        else
            bundle.putInt("position", position);
        bundle.putString("album", tvAlbum.getText().toString());
        CommonUtils.launchActivity(this, PhotoPreviewActivity.class, bundle);
    }

    @Override
    /** 照片选中状态改变之后 */
    public void onCheckedChanged(PhotoModel photoModel, CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            selected.add(photoModel);
            tvPreview.setEnabled(true);

        } else {
            selected.remove(photoModel);
        }

        tvPreview.setText("预览(" + selected.size() + ")");  //修改预览数量

        if (selected.isEmpty()) {
            tvPreview.setEnabled(false);
            tvPreview.setText("预览");
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutAlbum.getVisibility() == View.VISIBLE) {
            hideAlbum();
        } else
            super.onBackPressed();
    }

    @Override
    /** 相册列表点击事件 */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AlbumModel current = (AlbumModel) parent.getItemAtPosition(position);
        for (int i = 0; i < parent.getCount(); i++) {
            AlbumModel album = (AlbumModel) parent.getItemAtPosition(i);
            if (i == position)
                album.setCheck(true);
            else
                album.setCheck(false);
        }
        albumAdapter.notifyDataSetChanged();
        hideAlbum();
        tvAlbum.setText(current.getName());
        tvTitle.setText(current.getName());

        // 更新照片列表
        if (current.getName().equals(RECCENT_PHOTO))
            photoSelectorDomain.getReccent(reccentListener);
        else
            photoSelectorDomain.getAlbum(current.getName(), reccentListener); // 获取选中相册的照片
    }

    /**
     * 获取本地图库照片回调
     */
    public interface OnLocalReccentListener {
        void onPhotoLoaded(List<PhotoModel> photos);
    }

    /**
     * 获取本地相册信息回调
     */
    public interface OnLocalAlbumListener {
        void onAlbumLoaded(List<AlbumModel> albums);
    }

    private OnLocalAlbumListener albumListener = new OnLocalAlbumListener() {
        @Override
        public void onAlbumLoaded(List<AlbumModel> albums) {
            albumAdapter.update(albums);
        }
    };

    private OnLocalReccentListener reccentListener = new OnLocalReccentListener() {
        @Override
        public void onPhotoLoaded(List<PhotoModel> photos) {
            if (tvAlbum.getText().equals(RECCENT_PHOTO))
                photos.add(0, new PhotoModel());
            photoAdapter.update(photos);
            gvPhotos.smoothScrollToPosition(0); // 滚动到顶端
            reset();
        }
    };

    /**
     * 状态栏的隐藏和显现
     *
     * @param enable
     */
    public void fullscreenActivity(boolean enable) {

        if (enable) { //显示状态栏
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        } else { //隐藏状态栏

            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);

            getWindow().setAttributes(lp);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }
}
