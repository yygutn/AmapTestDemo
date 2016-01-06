package com.jumy.mapdemo;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.jumy.mapdemo.busline.BuslineActivity_;
import com.jumy.mapdemo.location.LocationActivity_;
import com.jumy.mapdemo.route.RouteActivity;
import com.jumy.mapdemo.route.RouteActivity_;
import com.jumy.mapdemo.utils.BaseActivity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity
public class MainActivity extends BaseActivity implements LocationSource, AMapLocationListener {
    @ViewById(R.id.topBar)
    Toolbar mTopBar;
    @ViewById(R.id.route)
    Button mRoute;
    @ViewById(R.id.gogogo)
    Button mGogogo;
    @ViewById(R.id.where)
    Button mWhere;
    @ViewById(R.id.show)
    TextView mShow;
    @ViewById(R.id.mapView)
    MapView mapView;


    LocationSource.OnLocationChangedListener mListener;
    AMapLocationClient mLocationClient;

    AMap aMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView.onCreate(savedInstanceState);
    }

    @AfterViews
    void start() {
        mShow.setVisibility(View.GONE);
        mapView.setVisibility(View.GONE);
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setLocationSource(this);
            aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
            aMap.setMyLocationEnabled(true);//true 表示显示定位层并可出发定位,false表示隐藏定位层,并不可出发定位,默认是false
            aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);//设置定位的类型为定位模式,参见类AMap
        }
    }

    @Click({R.id.where,R.id.show,R.id.route,R.id.gogogo})
    void click(View view) {
        switch (view.getId()) {
            case R.id.where: {
                mShow.setVisibility(View.VISIBLE);
                if (mLocationClient == null){
                    Toast.makeText(this, "client is null", Toast.LENGTH_SHORT).show();
                } else {
                    mLocationClient.startLocation();
                }
                break;
            }
            case R.id.show:{
                LocationActivity_.intent(this).start();
                break;
            }
            case R.id.route:{
                RouteActivity_.intent(this).start();
                break;
            }
            case R.id.gogogo:{
                BuslineActivity_.intent(this).start();
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        if (mLocationClient != null){
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

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                Log.w("LocationActivity", aMapLocation.toStr());
                mShow.setText(aMapLocation.getAddress());
//                mListener.onLocationChanged(aMapLocation);//显示系统小蓝点
            } else {
                Toast.makeText(this, "定位失败" + aMapLocation.getErrorCode() + aMapLocation.getErrorInfo(), Toast.LENGTH_SHORT).show();
            }
            mLocationClient.stopLocation();//定位一次后暂停定位，防止连续定位10
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            AMapLocationClientOption mLocationClientOption = new AMapLocationClientOption();
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
            //mLocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }
}
