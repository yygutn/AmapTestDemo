package com.jumy.mapdemo.busline;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.overlay.BusLineOverlay;
import com.amap.api.services.busline.BusLineItem;
import com.amap.api.services.busline.BusLineQuery;
import com.amap.api.services.busline.BusLineQuery.SearchType;
import com.amap.api.services.busline.BusLineResult;
import com.amap.api.services.busline.BusLineSearch;
import com.amap.api.services.busline.BusLineSearch.OnBusLineSearchListener;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.busline.BusStationQuery;
import com.amap.api.services.busline.BusStationResult;
import com.amap.api.services.busline.BusStationSearch.OnBusStationSearchListener;
import com.jumy.mapdemo.R;
import com.jumy.mapdemo.utils.BaseActivity;
import com.jumy.mapdemo.utils.OffLineMapUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.List;

/**
 * AMapV2地图中简单介绍公交线路搜索
 */
@EActivity
public class BuslineActivity extends BaseActivity implements OnMarkerClickListener,
        InfoWindowAdapter, OnItemSelectedListener, OnBusLineSearchListener,
        OnBusStationSearchListener, OnClickListener, LocationSource, AMapLocationListener {


    @ViewById(R.id.busName)
    EditText searchName;// 输入公交线路名称
    @ViewById(R.id.cityName)
    Spinner selectCity;// 选择城市下拉列表
    @ViewById(R.id.searchbyname)
    Button searchByName;
    @ViewById(R.id.map)
    MapView mapView;

    private AMap aMap;
    private ProgressDialog progDialog = null;// 进度框
    private String[] itemCitys = {"杭州-0571", "北京-010", "郑州-0371", "上海-021"};
    private String cityCode = "";// 城市区号
    private int currentpage = 0;// 公交搜索当前页，第一页从0开始
    private BusLineResult busLineResult;// 公交线路搜索返回的结果
    private List<BusLineItem> lineItems = null;// 公交线路搜索返回的busline
    private BusLineQuery busLineQuery;// 公交线路查询的查询类

    private BusStationResult busStationResult;// 公交站点搜索返回的结果
    private List<BusStationItem> stationItems;// 公交站点搜索返回的busStation
    private BusStationQuery busStationQuery;// 公交站点查询的查询类

    private BusLineSearch busLineSearch;// 公交线路列表查询

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.busline_activity);
        /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置;
         * 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
         * 则需要在离线地图下载和使用地图页面都进行路径设置
         * */
        //Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置
        MapsInitializer.sdcardDir = OffLineMapUtils.getSdCacheDir(this);
        mapView.onCreate(bundle);// 此方法必须重写
        init();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        searchByName.setOnClickListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemCitys);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectCity.setAdapter(adapter);
        selectCity.setPrompt("请选择城市：");
        selectCity.setOnItemSelectedListener(this);
    }

    /**
     * 设置marker的监听和信息窗口的监听
     */
    private void setUpMap() {
        aMap.setOnMarkerClickListener(this);
        aMap.setInfoWindowAdapter(this);

        //location listener
        aMap.setLocationSource(this);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);//true 表示显示定位层并可出发定位,false表示隐藏定位层,并不可出发定位,默认是false
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);//设置定位的类型为定位模式,参见类AMap
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    /**
     * 公交线路搜索
     */
    public void searchLine() {
        currentpage = 0;// 第一页默认从0开始
        showProgressDialog();
        String search = searchName.getText().toString().trim();
        if ("".equals(search)) {
            search = "641";
            searchName.setText(search);
        }
        busLineQuery = new BusLineQuery(search, SearchType.BY_LINE_NAME,
                cityCode);// 第一个参数表示公交线路名，第二个参数表示公交线路查询，第三个参数表示所在城市名或者城市区号
        busLineQuery.setPageSize(10);// 设置每页返回多少条数据
        busLineQuery.setPageNumber(currentpage);// 设置查询第几页，第一页从0开始算起
        busLineSearch = new BusLineSearch(this, busLineQuery);// 设置条件
        busLineSearch.setOnBusLineSearchListener(this);// 设置查询结果的监听
        busLineSearch.searchBusLineAsyn();// 异步查询公交线路名称
        // 公交站点搜索事例
        /*
		 * BusStationQuery query = new BusStationQuery(search,cityCode);
		 * query.setPageSize(10); query.setPageNumber(currentpage);
		 * BusStationSearch busStationSearch = new BusStationSearch(this,query);
		 * busStationSearch.setOnBusStationSearchListener(this);
		 * busStationSearch.searchBusStationAsyn();
		 */
    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null) {
            progDialog = new ProgressDialog(this);
        }
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索:\n");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 提供一个给默认信息窗口定制内容的方法
     */
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 提供一个个性化定制信息窗口的方法
     */
    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    /**
     * 点击marker回调函数
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;// 点击marker时把此marker显示在地图中心点
    }

    /**
     * 选择城市
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        String cityString = itemCitys[position];
        cityCode = cityString.substring(cityString.indexOf("-") + 1);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        cityCode = "010";
    }

    /**
     * 公交线路搜索返回的结果显示在dialog中
     */
    public void showResultList(List<BusLineItem> busLineItems) {
        BusLineDialog busLineDialog = new BusLineDialog(this, busLineItems);
        busLineDialog.onListItemClicklistener(new OnListItemlistener() {
            @Override
            public void onListItemClick(BusLineDialog dialog,
                                        final BusLineItem item) {
                showProgressDialog();

                String lineId = item.getBusLineId();// 得到当前点击item公交线路id
                busLineQuery = new BusLineQuery(lineId, SearchType.BY_LINE_ID,
                        cityCode);// 第一个参数表示公交线路id，第二个参数表示公交线路id查询，第三个参数表示所在城市名或者城市区号
                BusLineSearch busLineSearch = new BusLineSearch(
                        BuslineActivity.this, busLineQuery);
                busLineSearch.setOnBusLineSearchListener(BuslineActivity.this);
                busLineSearch.searchBusLineAsyn();// 异步查询公交线路id
            }
        });
        busLineDialog.show();

    }

    interface OnListItemlistener {
        public void onListItemClick(BusLineDialog dialog, BusLineItem item);
    }

    /**
     * 所有公交线路显示页面
     */
    class BusLineDialog extends Dialog implements OnClickListener {

        private List<BusLineItem> busLineItems;
        private BusLineAdapter busLineAdapter;
        private Button preButton, nextButton;
        private ListView listView;
        protected OnListItemlistener onListItemlistener;

        public BusLineDialog(Context context, int theme) {
            super(context, theme);
        }

        public void onListItemClicklistener(
                OnListItemlistener onListItemlistener) {
            this.onListItemlistener = onListItemlistener;

        }

        public BusLineDialog(Context context, List<BusLineItem> busLineItems) {
            this(context, android.R.style.Theme_NoTitleBar);
            this.busLineItems = busLineItems;
            busLineAdapter = new BusLineAdapter(context, busLineItems);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.busline_dialog);
            preButton = (Button) findViewById(R.id.preButton);
            nextButton = (Button) findViewById(R.id.nextButton);
            listView = (ListView) findViewById(R.id.listview);
            listView.setAdapter(busLineAdapter);
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                                        int arg2, long arg3) {
                    onListItemlistener.onListItemClick(BusLineDialog.this,
                            busLineItems.get(arg2));
                    dismiss();

                }
            });
            preButton.setOnClickListener(this);
            nextButton.setOnClickListener(this);
            if (currentpage <= 0) {
                preButton.setEnabled(false);
            }
            if (currentpage >= busLineResult.getPageCount() - 1) {
                nextButton.setEnabled(false);
            }

        }

        @Override
        public void onClick(View v) {
            this.dismiss();
            if (v.equals(preButton)) {
                currentpage--;
            } else if (v.equals(nextButton)) {
                currentpage++;
            }
            showProgressDialog();
            busLineQuery.setPageNumber(currentpage);// 设置公交查询第几页
            busLineSearch.setOnBusLineSearchListener(BuslineActivity.this);
            busLineSearch.searchBusLineAsyn();// 异步查询公交线路名称
        }
    }

    /**
     * 公交站点查询结果回调
     */
    @Override
    public void onBusStationSearched(BusStationResult result, int rCode) {
        dissmissProgressDialog();
        if (rCode == 0) {
            if (result != null && result.getPageCount() > 0
                    && result.getBusStations() != null
                    && result.getBusStations().size() > 0) {
                busStationResult = result;
                stationItems = result.getBusStations();
            } else {
                Toast.makeText(this, "R.string.no_result:" + R.string.no_result, Toast.LENGTH_SHORT).show();
            }
        } else {
            judge(rCode);
        }
    }

    /**
     * 公交线路查询结果回调
     */
    @Override
    public void onBusLineSearched(BusLineResult result, int rCode) {
        dissmissProgressDialog();
        if (rCode == 0) {
            if (result != null && result.getQuery() != null
                    && result.getQuery().equals(busLineQuery)) {
                if (result.getQuery().getCategory() == SearchType.BY_LINE_NAME) {
                    if (result.getPageCount() > 0
                            && result.getBusLines() != null
                            && result.getBusLines().size() > 0) {
                        busLineResult = result;
                        lineItems = result.getBusLines();
                        showResultList(lineItems);
                    }
                } else if (result.getQuery().getCategory() == SearchType.BY_LINE_ID) {
                    aMap.clear();// 清理地图上的marker
                    busLineResult = result;
                    lineItems = busLineResult.getBusLines();
                    BusLineOverlay busLineOverlay = new BusLineOverlay(this,
                            aMap, lineItems.get(0));
                    busLineOverlay.removeFromMap();
                    busLineOverlay.addToMap();
                    busLineOverlay.zoomToSpan();
                }
            } else {

                Toast.makeText(this, "R.string.no_result:" + R.string.no_result, Toast.LENGTH_SHORT).show();
            }
        } else {
            judge(rCode);
        }
    }

    private void judge(int rCode) {
        if (rCode == 27) {
            Toast.makeText(this, "R.string.error_network:" + R.string.error_network, Toast.LENGTH_SHORT).show();
        } else if (rCode == 32) {
            Toast.makeText(this, "R.string.error_key:" + R.string.error_key, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "R.string.error_other:" + R.string.error_other, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 查询公交线路
     */
    @Override
    public void onClick(View v) {
        searchLine();
    }

    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                Log.w("LocationActivity", aMapLocation.toStr());
                mListener.onLocationChanged(aMapLocation);//显示系统小蓝点
                aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
            } else {
                Toast.makeText(this, "定位失败" + aMapLocation.getErrorCode() + aMapLocation.getErrorInfo(), Toast.LENGTH_SHORT).show();
            }
            mLocationClient.stopLocation();//定位一次后暂停定位，防止连续定位加大耗电
        }
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
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
            mLocationClient.startLocation();
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
