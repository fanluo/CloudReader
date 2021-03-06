package com.example.jingbin.cloudreader.ui.wan.child;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.example.jingbin.cloudreader.R;
import com.example.jingbin.cloudreader.adapter.TreeAdapter;
import com.example.jingbin.cloudreader.base.BaseFragment;
import com.example.jingbin.cloudreader.bean.wanandroid.TreeBean;
import com.example.jingbin.cloudreader.databinding.FragmentWanAndroidBinding;
import com.example.jingbin.cloudreader.databinding.HeaderItemTreeBinding;
import com.example.jingbin.cloudreader.http.rx.RxBus;
import com.example.jingbin.cloudreader.http.rx.RxCodeConstants;
import com.example.jingbin.cloudreader.utils.CommonUtils;
import com.example.jingbin.cloudreader.utils.DataUtil;
import com.example.jingbin.cloudreader.utils.ToastUtil;
import com.example.jingbin.cloudreader.viewmodel.wan.TreeViewModel;

import me.jingbin.library.ByRecyclerView;
import me.jingbin.library.decoration.SpacesItemDecoration;

/**
 * @author jingbin
 * @date 2018/09/15.
 * @description 知识体系
 */
public class TreeFragment extends BaseFragment<TreeViewModel, FragmentWanAndroidBinding> {

    private boolean mIsPrepared;
    private boolean mIsFirst = true;
    private TreeAdapter mTreeAdapter;
    private FragmentActivity activity;

    @Override
    public int setContent() {
        return R.layout.fragment_wan_android;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = getActivity();
    }

    public static TreeFragment newInstance() {
        return new TreeFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 准备就绪
        mIsPrepared = true;
        /**
         * 因为启动时先走loadData()再走onActivityCreated，
         * 所以此处要额外调用load(),不然最初不会加载内容
         */
        loadData();
    }

    private void initRefreshView() {
        bindingView.srlWan.setColorSchemeColors(CommonUtils.getColor(R.color.colorTheme));
        bindingView.srlWan.setOnRefreshListener(() -> bindingView.srlWan.postDelayed(this::getTree, 150));
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        bindingView.xrvWan.setLayoutManager(layoutManager);
        mTreeAdapter = new TreeAdapter();
        bindingView.xrvWan.setAdapter(mTreeAdapter);
        HeaderItemTreeBinding oneBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.header_item_tree, null, false);
        bindingView.xrvWan.addHeaderView(oneBinding.getRoot());
        oneBinding.tvPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mTreeAdapter.isSelect()) {
                    oneBinding.tvPosition.setText("选择类别");
                    mTreeAdapter.setSelect(true);
                    mTreeAdapter.notifyDataSetChanged();
                    bindingView.xrvWan.addItemDecoration(new SpacesItemDecoration(activity).setNoShowDivider(1, 0).setDrawable(R.drawable.shape_line));
                } else {
                    oneBinding.tvPosition.setText("发现页内容订制");
                    mTreeAdapter.setSelect(false);
                    mTreeAdapter.notifyDataSetChanged();
                    if (bindingView.xrvWan.getItemDecorationCount() > 0) {
                        bindingView.xrvWan.removeItemDecorationAt(0);
                    }
                }
            }
        });
        bindingView.xrvWan.setOnItemClickListener(new ByRecyclerView.OnItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                if (mTreeAdapter.isSelect()) {
                    if (mTreeAdapter.getSelectedPosition() == position) {
                        ToastUtil.showToastLong("当前已经是\"" + mTreeAdapter.getData().get(position).getName() + "\"");
                        return;
                    }
                    oneBinding.tvPosition.setText("发现页内容订制");
                    mTreeAdapter.setSelect(false);
                    mTreeAdapter.notifyDataSetChanged();
                    if (bindingView.xrvWan.getItemDecorationCount() > 0) {
                        bindingView.xrvWan.removeItemDecorationAt(0);
                    }
                    layoutManager.scrollToPositionWithOffset(position + bindingView.xrvWan.getCustomTopItemViewCount(), 0);
                    RxBus.getDefault().post(RxCodeConstants.FIND_CUSTOM, position);
                }
            }
        });
    }

    @Override
    protected void loadData() {
        if (!mIsPrepared || !mIsVisible || !mIsFirst) {
            return;
        }
        showLoading();
        initRefreshView();
        bindingView.srlWan.postDelayed(this::getTree, 150);
        mIsFirst = false;
    }

    private void getTree() {
        viewModel.getTree().observe(this, new android.arch.lifecycle.Observer<TreeBean>() {
            @Override
            public void onChanged(@Nullable TreeBean treeBean) {
                showContentView();
                if (bindingView.srlWan.isRefreshing()) {
                    bindingView.srlWan.setRefreshing(false);
                }
                if (treeBean != null
                        && treeBean.getData() != null
                        && treeBean.getData().size() > 0) {

                    if (mTreeAdapter.getItemCount() == 0) {
                        DataUtil.putTreeData(activity, treeBean);
                    }
                    mTreeAdapter.setNewData(treeBean.getData());
                    bindingView.xrvWan.loadMoreComplete();
                } else {
                    if (mTreeAdapter.getData().size() == 0) {
                        showError();
                    } else {
                        bindingView.xrvWan.loadMoreComplete();
                    }
                }
            }
        });
    }

    @Override
    protected void onRefresh() {
        bindingView.srlWan.setRefreshing(true);
        getTree();
    }
}
