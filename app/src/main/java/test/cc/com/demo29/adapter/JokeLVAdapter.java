package test.cc.com.demo29.adapter;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import test.cc.com.demo29.R;
import test.cc.com.demo29.model.JokesNew;

/**
 * 添加数据的适配器
 */
public class JokeLVAdapter extends RecyclerView.Adapter<JokeLVAdapter.ViewHolder> {

    /** 上下文 */
    Activity context;

    /** 数据源 */
    List<JokesNew> data;

    /** 控件 */
    LayoutInflater inflater;

    /**
     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。
     */
    LruCache<String, BitmapDrawable> mMemoryCache;

    /**
     * 这里的data作为数据源从activity传入
     * @param activity
     * @param datas
     */
    public JokeLVAdapter(Activity activity, List<JokesNew> datas){
        this.context = activity;
        this.data = datas;

        //获取布局
        inflater = LayoutInflater.from(activity);


        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable drawable) {
                return drawable.getBitmap().getByteCount();
            }
        };
    }


    /**
     * 加载布局，相当于activity的onCreate方法
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.joke_lv_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 绑定数据
     * @param viewHolder
     * @param position
     */
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        viewHolder.joke_lv_txtconent.setText(data.get(position).getContent());
        viewHolder.joke_lv_txttime.setText("时间戳：" + data.get(position).getUnixtime() + "");

        //设置tag
        viewHolder.itemView.setTag(position);
    }

    /**
     * 数据源的内容大小
     * @return
     */
    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * //自定义的ViewHolder，持有每个Item的的所有界面元素
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        /** 获取item的控件 */
        public TextView joke_lv_txttime;
        public TextView joke_lv_txtconent;
        public LinearLayout lin_alljoke;

        public ViewHolder(View rootView) {
            super(rootView);
            this.joke_lv_txtconent = rootView.findViewById(R.id.joke_lv_txtconent);
            this.joke_lv_txttime = rootView.findViewById(R.id.joke_lv_txttime);
            this.lin_alljoke = rootView.findViewById(R.id.lin_alljoke);

            //设置item的点击事件
            this.lin_alljoke.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Linster.textItemOnClick(v, getPosition());
                }
            });
        }
    }

    public ItemOnClickLinster Linster;

    public void setLinster(ItemOnClickLinster linster) {
        Linster = linster;
    }

    public interface ItemOnClickLinster{
        void textItemOnClick(View view, int position);
    }


}
