package com.jumy.mapdemo.location;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.MyLocationStyle;
import com.jumy.mapdemo.R;
import com.jumy.mapdemo.utils.BaseActivity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;

@EActivity
public class LocationActivity extends BaseActivity implements LocationSource, AMapLocationListener {

    @ViewById(R.id.topBar)
    Toolbar mTopbar;
    @ViewById
    MapView mapView;

    AMap aMap;
    OnLocationChangedListener mListener;
    AMapLocationClient mLocationClient;
    AMapLocationClientOption mLocationClientOption;

    @ColorRes(R.color.white)
    int white;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        mapView.onCreate(savedInstanceState);
    }

    @AfterViews
    void start() {
        mTopbar.setTitle("我在这里！");
        mTopbar.setTitleTextColor(white);
        if (aMap == null) {
            aMap = mapView.getMap();
//            aMap.setMapType(AMap.MAP_TYPE_NORMAL);
            aMap.setLocationSource(this);//定位监听
            aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
            aMap.setMyLocationEnabled(true);//true 表示显示定位层并可出发定位,false表示隐藏定位层,并不可出发定位,默认是false

//            // 自定义系统定位蓝点
//            MyLocationStyle myLocationStyle = new MyLocationStyle();
//            // 自定义定位蓝点图标
//            myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
//                    fromResource(R.drawable.location_marker));
//            // 自定义精度范围的圆形边框颜色
//            myLocationStyle.strokeColor(Color.WHITE);
//            //自定义精度范围的圆形边框宽度
//            myLocationStyle.strokeWidth(5);
//            // 将自定义的 myLocationStyle 对象添加到地图上
//            aMap.setMyLocationStyle(myLocationStyle);
            //
            Log.w("LocationActivity", "aMap.getCameraPosition().zoom:" + aMap.getCameraPosition().zoom);

        }

    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onPostResume() {
        mapView.onResume();
        super.onPostResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        deactivate();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    //激活定位
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            mLocationClientOption = new AMapLocationClientOption();
            //设置定位监听
            mLocationClient.setLocationListener(this);
            //设置定位为高精度模式
            mLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationClientOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mLocationClient.startLocation();
        }
    }

    //停止定位
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    //定位回调
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                Log.w("LocationActivity", aMapLocation.toStr());
                mListener.onLocationChanged(aMapLocation);//显示系统小蓝点
                aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE);//设置定位的类型为定位模式,参见类AMap
                deactivate();
            } else {
                Log.d("LocationActivity", "定位失败");
                Toast.makeText(this, "定位失败" + aMapLocation.getErrorCode() + aMapLocation.getErrorInfo(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
