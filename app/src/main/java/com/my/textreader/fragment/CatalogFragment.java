package com.my.textreader.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.my.textreader.R;
import com.my.textreader.adapter.CatalogueAdapter;
import com.my.textreader.base.BaseFragment;
import com.my.textreader.db.BookCatalogue;
import com.my.textreader.util.PageFactory;


import java.util.ArrayList;
//目录
public class CatalogFragment extends BaseFragment {
    public static final String ARGUMENT = "argument";

    private PageFactory pageFactory;
    ArrayList<BookCatalogue> catalogueList = new ArrayList<>();

    ListView lv_catalogue;

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_catalog;
    }

    @Override
    protected void initData(View view) {

        lv_catalogue = view.findViewById(R.id.lv_catalogue);
        pageFactory = PageFactory.getInstance();
        
        // 清空之前的目录列表
        catalogueList.clear();
        
        // 获取最新的目录列表
        catalogueList.addAll(pageFactory.getDirectoryList());
        
        CatalogueAdapter catalogueAdapter = new CatalogueAdapter(getActivity(), catalogueList);
        catalogueAdapter.setCharter(pageFactory.getCurrentCharter());
        lv_catalogue.setAdapter(catalogueAdapter);
        catalogueAdapter.notifyDataSetChanged();
        
        // 如果目录为空，显示提示信息
        if (catalogueList.isEmpty()) {
            // 可以在这里添加一个提示，但为了简单起见，我们暂时不添加
        }
    }

    @Override
    protected void initListener() {
        lv_catalogue.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pageFactory.changeChapter(catalogueList.get(position).getBookCatalogueStartPos());
                getActivity().finish();
            }
        });
    }

    /**
     * 用于从Activity传递数据到Fragment
     *
     * @param bookpath
     * @return
     */
    public static CatalogFragment newInstance(String bookpath) {
        Bundle bundle = new Bundle();
        bundle.putString(ARGUMENT, bookpath);
        CatalogFragment catalogFragment = new CatalogFragment();
        catalogFragment.setArguments(bundle);
        return catalogFragment;
    }

}
