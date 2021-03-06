package com.zone.adapter3.decoration;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import com.zone.adapter3.base.Header2FooterRcvAdapter;
import com.zone.adapter3.base.IAdapter;
import com.zone.adapter3.bean.ViewDelegates;
import com.zone.adapter3.bean.Wrapper;
import java.util.ArrayList;
import java.util.List;

/**
 * [2017] by Zone
 */

public class MarginItemDecoration extends RecyclerView.ItemDecoration {

    private final IAdapter adapter;
    private int space;
    private boolean hasTop = true, hasBottom = true, hasLeft = true, hasRight = true;
    private onTransformListener mOnTransformListener;

    public MarginItemDecoration(int space, IAdapter adapter) {
        this.space = space;
        this.adapter = adapter;
        calculate();
        adapter.registerAdapterDataObserver(observer);
    }

    private RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {

        @Override
        public void onChanged() {
            calculate();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            calculate();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            calculate();
        }
    };

    private List<Entity> entityList = new ArrayList<>();
    private int groupCount;
    private int spanIndex;
    private int spanCount;

    class Entity {
        public boolean isFullLine;
        public int groupId;
        public int spanIndex;
        boolean changeLine;
        Rect outRect;
        Rect reduceDectorRect;
    }

    private void calculate() {
        RecyclerView.LayoutManager manager = adapter.getRecyclerView().getLayoutManager();
        if(manager instanceof LinearLayoutManager){

            restore();

            for (int i = 0; i < adapter.getRealItemCount(); i++) {
                boolean isHeader = i < adapter.getHeaderViewsCount();
                boolean isFooter = i > adapter.getHeaderViewsCount() + adapter.getData().size() - 1;
                boolean isDataFullLine = true;
                Rect outRect = null;
                Rect reduceDectorRect = null;
                if (!(isHeader && isFooter)) {
                    Header2FooterRcvAdapter headerAdapter = (Header2FooterRcvAdapter) adapter;
                    for (int j = 0; j < headerAdapter.getDataWraps().size(); j++) {
                        List<Wrapper> warps = headerAdapter.getDataWraps();
                        if (adapter.getItemViewType(i) == warps.get(j).getStyle()) {
                            outRect = warps.get(j).getViewDelegates().dectorRect();
                            reduceDectorRect = warps.get(j).getViewDelegates().reduceDectorRect();
                            break;
                        }
                    }
                    Entity entity = new Entity();
                    entity.isFullLine=isDataFullLine;
                    addEntity(outRect, reduceDectorRect, entity);
                }
            }

        }


        if (manager instanceof GridLayoutManager || manager instanceof StaggeredGridLayoutManager) {

            restore();

            if (manager instanceof GridLayoutManager)
                spanCount = ((GridLayoutManager) manager).getSpanCount();
            else
                spanCount = ((StaggeredGridLayoutManager) manager).getSpanCount();
            for (int i = 0; i < adapter.getRealItemCount(); i++) {
                boolean isHeader = i < adapter.getHeaderViewsCount();
                boolean isFooter = i > adapter.getHeaderViewsCount() + adapter.getData().size() - 1;
                boolean isDataFullLine = false;
                Rect outRect = null;
                Rect reduceDectorRect = null;
                if (!(isHeader && isFooter)) {
                    Header2FooterRcvAdapter headerAdapter = (Header2FooterRcvAdapter) adapter;
                    for (int j = 0; j < headerAdapter.getDataWraps().size(); j++) {
                        List<Wrapper> warps = headerAdapter.getDataWraps();
                        if (adapter.getItemViewType(i) == warps.get(j).getStyle()) {
                            isDataFullLine = warps.get(j).getViewDelegates().isFullspan();
                            outRect = warps.get(j).getViewDelegates().dectorRect();
                            reduceDectorRect = warps.get(j).getViewDelegates().reduceDectorRect();
                            break;
                        }
                    }
                }
                Entity entity = new Entity();
                if (isHeader || isFooter || isDataFullLine) {
                    entity.isFullLine = true;
                    if (i != 0 && !entityList.get(i - 1).changeLine) {
                        groupCount++;
                        spanIndex = 0;
                    }

                    if (i == adapter.getRealItemCount() - 1)
                        entity.groupId = groupCount;
                    else {
                        entity.groupId = groupCount++;
                        entity.changeLine = true;
                    }
                    spanIndex = 0;
                    entity.spanIndex = spanIndex;

                    addEntity(outRect, reduceDectorRect, entity);
                } else {
                    entity.isFullLine = false;
                    entity.groupId = groupCount;
                    entity.spanIndex = spanIndex;

                    addEntity(outRect, reduceDectorRect, entity);
                    spanIndex++;
                    if (entity.spanIndex == spanCount - 1 && i != adapter.getRealItemCount() - 1) {
                        groupCount++;
                        spanIndex = 0;
                        entity.changeLine = true;
                    }
                }

            }
        }
    }

    private void addEntity(Rect outRect, Rect reduceDectorRect, Entity entity) {
        entity.outRect= outRect;
        entity.reduceDectorRect= reduceDectorRect;
        entityList.add(entity);
    }

    private void restore() {
        entityList.clear();
        groupCount = 0;
        spanIndex = 0;
        spanCount = 0;
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (adapter.getRealItemCount() == 0) return;
        int position = parent.getChildAdapterPosition(view);
        if (position < 0) return;

        RecyclerView.LayoutManager manager = parent.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            if (((GridLayoutManager) manager).getOrientation() == GridLayoutManager.VERTICAL) {
                gridV(outRect, position);
            } else
                throw new IllegalStateException("Grid 横向暂时不支持!");
        } else if (manager instanceof LinearLayoutManager) {
            if (((LinearLayoutManager) manager).getOrientation() == LinearLayoutManager.VERTICAL)
                linearV(outRect, position);
            else
                linearH(outRect, position);
        } else if (manager instanceof StaggeredGridLayoutManager) {
            if (((StaggeredGridLayoutManager) manager).getOrientation() == StaggeredGridLayoutManager.VERTICAL) {
                gridV(outRect, position);
            } else
                throw new IllegalStateException("StaggeredGridLayoutManager 横向暂时不支持!");
        } else
            throw new IllegalStateException("其他类型的Manager暂时不支持!");

        if(entityList.size()!=0){
            Entity item = entityList.get(position);
            if(item.outRect!=null){
                if(item.outRect.top!= ViewDelegates.ORG_DECTOR)
                    outRect.top=item.outRect.top;

                if(item.outRect.bottom!= ViewDelegates.ORG_DECTOR)
                    outRect.bottom=item.outRect.bottom;

                if(item.outRect.left!= ViewDelegates.ORG_DECTOR)
                    outRect.left=item.outRect.left;

                if(item.outRect.right!= ViewDelegates.ORG_DECTOR)
                    outRect.right=item.outRect.right;
            }

            if(item.reduceDectorRect!=null){
                outRect.top+=item.reduceDectorRect.top;
                outRect.bottom+=item.reduceDectorRect.bottom;
                outRect.right+=item.reduceDectorRect.right;
                outRect.left+=item.reduceDectorRect.left;
            }

        }

        transformRect(position, entityList.size()!=0?entityList.get(position):null, outRect);
    }

    private void gridV(Rect outRect, int position) {
        Entity item = entityList.get(position);
        //考虑上下
        if (hasTop && item.groupId == 0)
            outRect.top = space;
        if (hasBottom && item.groupId == groupCount)
            outRect.bottom = space;
        if (item.groupId != 0)
            outRect.top = space;

        //考虑左右
        if (item.isFullLine) {
            if (hasLeft)
                outRect.left = space;
            if (hasRight)
                outRect.right = space;
        } else {
            if (item.spanIndex == 0) {//first
                outRect.left = space;
                outRect.right = space / 2;
            } else if (item.spanIndex == spanCount - 1) {//last
                outRect.left = space / 2;
                outRect.right = space;
            } else {
                outRect.left = space / 2;
                outRect.right = space / 2;
            }
        }
    }

    private void linearH(Rect outRect, int postion) {
        if (hasLeft && postion == 0)
            outRect.left = space;
        if (hasRight && postion == adapter.getRealItemCount() - 1)
            outRect.right = space;
        if (postion != 0)
            outRect.left = space;
        if (hasTop)
            outRect.top = space;
        if (hasBottom)
            outRect.bottom = space;
    }

    private void linearV(Rect outRect, int postion) {
        if (hasLeft)
            outRect.left = space;
        if (hasRight)
            outRect.right = space;
        if (hasTop && postion == 0)
            outRect.top = space;
        if (hasBottom && postion == adapter.getRealItemCount() - 1)
            outRect.bottom = space;
        if (postion != 0)
            outRect.top = space;
    }


    public int getSpace() {
        return space;
    }

    public MarginItemDecoration space(int space) {
        this.space = space;
        return this;

    }

    public boolean isHasTop() {
        return hasTop;
    }

    public MarginItemDecoration hasTop(boolean hasTop) {
        this.hasTop = hasTop;
        return this;
    }

    public boolean isHasBottom() {
        return hasBottom;
    }

    public MarginItemDecoration hasBottom(boolean hasBottom) {
        this.hasBottom = hasBottom;
        return this;
    }

    public boolean isHasLeft() {
        return hasLeft;
    }

    public MarginItemDecoration hasLeft(boolean hasLeft) {
        this.hasLeft = hasLeft;
        return this;
    }

    public boolean isHasRight() {
        return hasRight;
    }

    public MarginItemDecoration hasRight(boolean hasRight) {
        this.hasRight = hasRight;
        return this;
    }


    public onTransformListener getOnTransformListener() {
        return mOnTransformListener;
    }

    public MarginItemDecoration setOnTransformListener(onTransformListener mOnTransformListener) {
        this.mOnTransformListener = mOnTransformListener;
        return this;
    }

    private void transformRect(int position, Entity item, Rect outRect) {
        if (mOnTransformListener != null)
            mOnTransformListener.transformRect(position, item, outRect);
    }


    public interface onTransformListener {
        void transformRect(int position, Entity item, Rect outRect);
    }
}
