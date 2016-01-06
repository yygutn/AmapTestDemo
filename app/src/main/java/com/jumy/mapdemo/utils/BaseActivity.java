package com.jumy.mapdemo.utils;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jumy on 15/12/6 下午6:13.
 * deadline is the first productivity
 */
public class BaseActivity extends AppCompatActivity{
    private static final String TAG = BaseActivity.class.getSimpleName();

    public static int mIndex = 0;//菜单栏下标,默认首页0
    public static int preIndex = 0;//菜单栏下标,上一个菜单栏的状态
    //当前所有activity引用的堆栈
    private static List<WeakReference<BaseActivity>> mPageStack = new ArrayList<>();
    //当前activity的引用--TOP
    private static WeakReference<BaseActivity> mCurrentIntance = null;

    /**
     * 返回最上层的activity
     * @return 栈顶activity的引用
     */
    public static WeakReference<BaseActivity> getTopActivity(){
        if (mPageStack != null){
            int size = mPageStack.size();
            if (size >= 1) {
                WeakReference<BaseActivity> reference = mPageStack.get(size - 1);
                if (reference != null) {
                    return reference;
                }
            }
        }
        return null;
    }

    public static void deleteAllStackBesideTop(){
        int len = mPageStack.size();
        while(mPageStack.size()>1){
            mPageStack.remove(1).get().finish();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置为强制竖屏，不使用横屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //切换堆栈
        //switchStack();
        //当前activity的弱引用
        mCurrentIntance = new WeakReference<BaseActivity>(this);
        //当前activity加到列表当中
        mPageStack.add(mCurrentIntance);
        Log.w("Jumy", "Cur_Stack_Size: " + mPageStack.size());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG,"onDestroy");
        Log.w(TAG, "Cur_Stack_Size: " + mPageStack.size());
    }

    /**
     * 返回事件
     */
    private static long exitTime = 0;

    public void backToPreActivity() {
        int postion = mPageStack.size() - 1;
        WeakReference<BaseActivity> mCurPage = mPageStack.get(postion);
        if (mPageStack.size() <= 1) {
            if (System.currentTimeMillis() - exitTime > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                //结束所有activity并清空堆栈
                ClearCurStack();
                System.exit(0);
            }
        } else if (mPageStack.size() > 1){
            //backToPreActivity to pre page  当前堆栈大于1执行，否则不会执行返回操作
//            mCurPage = mPageStack.remove(postion);
//            mCurPage.get().finish();
            finish();
        }

    }

    @Override
    public void onBackPressed() {
        backToPreActivity();
    }

    /**
     * 清空当前堆栈
     */
    public static void ClearCurStack() {
        for(WeakReference<BaseActivity> activities : mPageStack){
            activities.get().finish();
        }
        mPageStack.clear();
    }

    @Override
    public void finish() {
        Log.w("Jumy","Before finish, the Stack size is :"+mPageStack.size());
        int pos = mPageStack.size() - 1;
        mPageStack.remove(pos);
        super.finish();
    }

    public void showToast(final String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
