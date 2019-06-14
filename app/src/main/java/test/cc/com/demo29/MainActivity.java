package test.cc.com.demo29;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import test.cc.com.demo29.adapter.JokeLVAdapter;
import test.cc.com.demo29.http.HttpRequest;
import test.cc.com.demo29.model.JokesNew;
import test.cc.com.demo29.model.SJJokeNow;
import test.cc.com.demo29.systemstatusbar.StatusBarCompat;

/**
 * activity
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * activity
     */
    Activity activity = this;

    /**
     * 请求的key
     */
    public static String Joke_APPKEY = "e9bbc8a5de090451bd5da96dc574a94a";
    /**
     * 请求随机获取笑话的URL地址
     */
    public static final String HTTPURLS = "http://v.juhe.cn/joke/randJoke.php?";

    /**
     * 执行动画对象
     */
    private static Animation rotateAnimation;

    /**
     * 网络请求返回码
     */
    static final int SUCC_CODE = 0;

    /**
     * 返回按钮和加载中按钮
     */
    ImageView joke_img_back, joke_img_load;

    /**
     * 加载内容的RV
     */
    RecyclerView joke_rv;

    /**
     * 添加数据的适配器
     */
    JokeLVAdapter adapter;

    /**
     * 自定义刷新和加载的标识，默认为false
     */
    boolean isRef, isLoad = false;

    /**
     * swf：这个是上拉刷新和加载框架
     */
    RefreshLayout activity_joke_refreshLayout;

    /**
     * 使用handler请求网络数据并在handleMessage里面处理返回操作
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //如果是刷新和加载的请求标识，直接刷新adapter加载数据
            if (msg.arg1 == SUCC_CODE && isLoad || isRef) {
                adapter.notifyDataSetChanged();
            }
            //否则的话就相当于首次进入加载，先关闭动画，然后把数据加载到RV上
            else if (msg.arg1 == SUCC_CODE) {
                joke_img_load.clearAnimation();
                joke_img_load.setVisibility(View.GONE);
                adapter = new JokeLVAdapter(activity, datas);
                joke_rv.setLayoutManager(new LinearLayoutManager(activity));
                joke_rv.setAdapter(adapter);
                //当rv的item点击之后进入此方法，并在openWindow处理逻辑
                adapter.setLinster(new JokeLVAdapter.ItemOnClickLinster() {
                    @Override
                    public void textItemOnClick(View view, int position) {
                        Log.i("activity", "----->position=" + position);
                        //打开一个窗口
                        openWindow(position);
                    }
                });
            } else {
                //数据加载失败，关闭动画，并提示
                joke_img_load.clearAnimation();
                joke_img_load.setVisibility(View.GONE);
                Toast.makeText(activity, R.string.getDataError, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 通过position去查找唯一的一条信息
     *
     * @param position
     */
    private void openWindow(int position) {
        Toast.makeText(activity, "当前点击item的下标为" + position, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置一个集合，用来存储网络请求到的数据
     */
    List<JokesNew> datas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        //修改状态栏颜色
        StatusBarCompat.setStatusBarColor(activity, ContextCompat.getColor(activity, R.color.cyan));
    }

    public void initView() {
        //获取控件id
        joke_img_back = findViewById(R.id.joke_img_back);
        joke_img_load = findViewById(R.id.joke_img_load);
        joke_rv = findViewById(R.id.joke_rv);
        activity_joke_refreshLayout = findViewById(R.id.activity_joke_refreshLayout);

        //设置refreshLayout的一些操作
        //越界回弹
        activity_joke_refreshLayout.setEnableOverScrollBounce(false);

        //在刷新或者加载的时候不允许操作视图
        activity_joke_refreshLayout.setDisableContentWhenRefresh(true);
        activity_joke_refreshLayout.setDisableContentWhenLoading(true);

        //监听列表在滚动到底部时触发加载事件（默认true）
        activity_joke_refreshLayout.setEnableAutoLoadmore(false);


        /**
         * 正在下拉刷新数据中
         */
        activity_joke_refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                Log.i("activity", "下拉刷新");
                //数据加载完后调用这行结束刷新
                isRef = true;
                handler.post(getRefreshDatas);
            }
        });

        /**
         * 正在上拉加载数据中
         */
        activity_joke_refreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                Log.i("activity", "上拉加载");
                isLoad = true;
                handler.post(getLoadmoreDatas);
            }
        });

        //退出
        joke_img_back.setOnClickListener(this);
    }

    public void initData() {
        //将xml的控件设置为可见状态，并开启一个动画去过渡加载数据中的空白页面
        joke_img_load.setVisibility(View.VISIBLE);
        openA(activity, joke_img_load);
        //请求
        handler.post(getDatas);
    }

    /**
     * getDatas
     */
    Runnable getDatas = new Runnable() {
        @Override
        public void run() {
            HttpRequest.get(HTTPURLS + "&key=" + Joke_APPKEY, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("activity", "数据获取成功");
                    String result = response.body().string();
                    JsonDta(result);
                }
            });
        }
    };

    /**
     * 解析json
     *
     * @param result
     */
    private void JsonDta(String result) {
        Message message = handler.obtainMessage();
        //解析json数据并赋值给SJJokeNow对象
        SJJokeNow obj = new Gson().fromJson(result, SJJokeNow.class);
        //不成功时，通知handler数据加载失败
        if (obj.getError_code() != 0) {
            message.arg1 = obj.getError_code();
            handler.sendMessage(message);
        } else {
            //成功时，判断位
            if (isRef) {
                Log.i("activity", "------>" + obj.getReason());
                List<JokesNew> json = new ArrayList<>();
                for (int i = 0; i < obj.getResult().size(); i++) {
                    JokesNew info = new JokesNew();
                    info.setHashId(obj.getResult().get(i).getHashId());
                    info.setContent(obj.getResult().get(i).getContent());
                    info.setUnixtime(obj.getResult().get(i).getUnixtime());
                    json.add(info);
                }
                for (int i = 0; i < datas.size(); i++) {
                    json.add(datas.get(i));
                }
                datas.clear();
                for (int i = 0; i < json.size(); i++) {
                    datas.add(json.get(i));
                }
                isRef = false;
            } else {
                Log.i("activity", "------>" + obj.getReason());
                for (int i = 0; i < obj.getResult().size(); i++) {
                    JokesNew info = new JokesNew();
                    info.setHashId(obj.getResult().get(i).getHashId());
                    info.setContent(obj.getResult().get(i).getContent());
                    info.setUnixtime(obj.getResult().get(i).getUnixtime());
                    datas.add(info);
                }
            }
            message.arg1 = obj.getError_code();
            handler.sendMessage(message);
        }
    }

    /**
     * 加载刷新的数据
     */
    Runnable getRefreshDatas = new Runnable() {
        @Override
        public void run() {
            HttpRequest.get(HTTPURLS + "&key=" + Joke_APPKEY, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity_joke_refreshLayout.finishRefresh(0000, false);
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("activity", "数据获取成功");
                    activity_joke_refreshLayout.finishRefresh(0000, true);
                    String result = response.body().string();
                    JsonDta(result);
                }
            });
        }
    };

    /**
     * 加载上拉的数据
     */
    Runnable getLoadmoreDatas = new Runnable() {
        @Override
        public void run() {
            HttpRequest.get(HTTPURLS + "&key=" + Joke_APPKEY, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    activity_joke_refreshLayout.finishLoadmore(0000, false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("activity", "数据获取成功");
                    activity_joke_refreshLayout.finishLoadmore(0000, true);
                    String result = response.body().string();
                    JsonDta(result);
                }
            });
        }
    };

    @Override
    public void onClick(View v) {
        int temdId = v.getId();
        if (temdId == R.id.joke_img_back) {
            finish();
        }
    }

    /**
     * 开启一个动画
     *
     * @param img
     */
    public static void openA(Activity activity, ImageView img) {
        //加载loading动画
        rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.loading);
        LinearInterpolator interpolator = new LinearInterpolator();
        rotateAnimation.setInterpolator(interpolator);
        img.startAnimation(rotateAnimation);
    }

}
