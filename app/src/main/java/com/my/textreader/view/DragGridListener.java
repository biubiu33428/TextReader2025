package com.my.textreader.view;


public interface DragGridListener {
    /**
     * 重新排列数据
     *
     * @param oldPosition
     * @param newPosition
     */
    public void reorderItems(int oldPosition, int newPosition);


    /**
     * 设置某个item隐藏
     *
     * @param hidePosition
     */
    public void setHideItem(int hidePosition);


    /**
     * 删除某个item
     *
     * @param deletePosition
     */
    public void removeItem(int deletePosition);

    /**
     * 设置点击打开后的item移动到第一位置
     *
     * @param openPosition
     */
    void setItemToFirst(int openPosition);

    void nitifyDataRefresh();
}
