package com.photoselector.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.photoselector.R;
import com.photoselector.model.PhotoModel;

import java.util.Random;

public class PhotoItem extends LinearLayout implements OnCheckedChangeListener, OnClickListener {

    private ImageView ivPhoto;
    private CheckBox cbPhoto;
    private onPhotoItemCheckedListener listener;
    private PhotoModel photo;
    private boolean isCheckAll;
    private onItemClickListener l;
    private int position;
    private Context mContext;

    private PhotoItem(Context context) {
        super(context);
        mContext = context;
    }

    public PhotoItem(Context context, onPhotoItemCheckedListener listener) {
        this(context);
        LayoutInflater.from(context).inflate(R.layout.layout_photoitem, this, true);
        this.listener = listener;
        mContext = context;
        setOnClickListener(this);
        ivPhoto = (ImageView) findViewById(R.id.iv_photo_lpsi);
        cbPhoto = (CheckBox) findViewById(R.id.cb_photo_lpsi);
        cbPhoto.setOnCheckedChangeListener(this); // CheckBox选中状态改变监听器
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // 让图片变暗或者变亮

        if (null!=PhotoSelectorActivity.instance&&!PhotoSelectorActivity.instance.isMore()) {
            if (isChecked) {
                if (PhotoSelectorActivity.instance.getCount() > 0) {
                    Toast.makeText(mContext, "仅可选取一张图片!", Toast.LENGTH_SHORT).show();
                    cbPhoto.setChecked(!isChecked);
                } else {
                    if (!isCheckAll) {
                        listener.onCheckedChanged(photo, buttonView, isChecked); // 调用主界面回调函数
                    }
                    setDrawingable();
                    ivPhoto.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                }

            } else {
                listener.onCheckedChanged(photo, buttonView, isChecked);
                ivPhoto.clearColorFilter();
            }
        } else {
            if (!isCheckAll) {
                listener.onCheckedChanged(photo, buttonView, isChecked); // 调用主界面回调函数
            }
            if (isChecked) {
                setDrawingable();
                ivPhoto.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            } else {
                ivPhoto.clearColorFilter();
            }

            photo.setChecked(isChecked);
        }


    }

    /**
     * 设置路径下的图片对应的缩略图
     */
    public void setImageDrawable(final PhotoModel photo) {
        this.photo = photo;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ImageLoader.getInstance().displayImage("file://" + photo.getOriginalPath(), ivPhoto);
            }
        }, new Random().nextInt(10));
    }

    private void setDrawingable() {
        ivPhoto.setDrawingCacheEnabled(true);
        ivPhoto.buildDrawingCache();
    }

    @Override
    public void setSelected(boolean selected) {
        if (photo == null) {
            return;
        }
        isCheckAll = true;
        cbPhoto.setChecked(selected);
        isCheckAll = false;
    }

    public void setOnClickListener(onItemClickListener l, int position) {
        this.l = l;
        this.position = position;
    }

    @Override
    public void onClick(View v) {

        if (l != null)
            l.onItemClick(position);
    }

    /**
     * 图片Item选中事件监听器
     */
    public interface onPhotoItemCheckedListener {
        void onCheckedChanged(PhotoModel photoModel, CompoundButton buttonView, boolean isChecked);
    }

    /**
     * 图片点击事件
     */
    public interface onItemClickListener {
        void onItemClick(int position);
    }
}
