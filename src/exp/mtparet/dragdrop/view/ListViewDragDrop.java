/*
*Copyright 2011 Matthieu Paret
*
*This file is part of DragAndDrop.
*
*DragAndDrop is free software: you can redistribute it and/or modify
*it under the terms of the GNU Lesser General Public License as published by
*the Free Software Foundation, either version 3 of the License, or
*(at your option) any later version.
*
*DragAndDrop is distributed in the hope that it will be useful,
*but WITHOUT ANY WARRANTY; without even the implied warranty of
*MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*GNU General Public License for more details.
*
*You should have received a copy of the GNU Lesser General Public License
*along with DragAndDrop.  If not, see <http://www.gnu.org/licenses/>.
*/

package exp.mtparet.dragdrop.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Extends ListView, implement some additionnal listener
 * @author Matthieu Paret
 *
 */
public class ListViewDragDrop extends ListView{
	
	private OnTouchListener mOnItemOutUpListener;
	private OnTouchListener mOnItemMoveListener; //It is an hacked touchLister, in fact it is OnMoveListener
	private OnItemClickListener mOnItemReceiver;
	private boolean isScroll = false;
	private boolean isMove = false;
	private View childSelected;
	private int xInit;
	private int yInit;
	private int leftBound;
	private int mStartPosition;
	public int mDragPointOffset;		//Used to adjust drag view location
	private int mDragPoint;    // at what offset inside the item did the user grab it
	private int mCoordOffset;  // the difference between screen coordinates and coordinates in this view
	public int mItemPosition;
	private Context mContext;
	private boolean isDraggable = true;
	private boolean isEnable = true;
	public ListViewDragDrop(Context context) {
		super(context);
	}

	public ListViewDragDrop(Context context, AttributeSet attrs) {
       super(context, attrs);
  }

   public ListViewDragDrop(Context context, AttributeSet attrs, int defStyle) {
         super(context, attrs, defStyle);
   }
   
   /**
    * When an item is moved horizontally out of this position, this listener is called. Before this OnItemSelectedListener is called.
    * @param listener
    */
	public void setOnItemMoveListener(AdapterView.OnTouchListener listener){
		mOnItemMoveListener = listener;
	}

	/**
	 * When a gesture on a item is terminated out of the the listView
	 * @param listener
	 */
	public void setOnItemUpOutListener(AdapterView.OnTouchListener listener){
		this.mOnItemOutUpListener = listener;
	}

	/**
	 * When an outsider item is moved and up on this listview. Return position where to add this item.
	 * @param listener
	 */
	public void setOnItemReceiverListener(AdapterView.OnItemClickListener listener){
		this.mOnItemReceiver = listener;
	}
	

	
   @Override
   public boolean onTouchEvent(MotionEvent ev) {
	   boolean handled = false;
	   //如果touch event沒被handle的話 交給super來處理
		if(mOnItemMoveListener != null && !handled)
			handled = onMove(ev);
		
		if(!handled)
       return super.onTouchEvent(ev);
		
	return handled;
   }

   /**
    * 
    * @param e
    * @return
    */
   public boolean onUpReceive(MotionEvent e) {

		if(e.getAction() == MotionEvent.ACTION_UP){
			
			int x = (int)e.getRawX();
			//55是上方的title Text的高度
			int y = (int)e.getRawY() - 55;

			for(int i=0;i<getChildCount();i++){
				//抓取每個child並把他的範圍抓出來並偵測如果x y有在這個child裡面
				Rect viewRect = new Rect();
				View child = getChildAt(i);
				int left = child.getLeft() + this.getLeft();
				int right = child.getRight() + this.getLeft();
				int top = child.getTop() + this.getTop();
				//如果範圍是在這個child的上半部
				int bottom = child.getTop() + child.getHeight()/2 + this.getTop();
				viewRect.set(left, top, right, bottom);

				//當x > 最右邊的時候 代表它不落在ListView的裡面
				if(x > right)
					return false;
				if(viewRect.contains(x,y)){
					//如果是drag的那個listview抓取那個item
					if(getOnItemSelectedListener() != null){
						getOnItemSelectedListener().onItemSelected(ListViewDragDrop.this, child, i , getItemIdAtPosition(i));
					}
					//如果是drop的那個listview加入item到i position
					if(mOnItemReceiver != null){
						if(i + getFirstVisiblePosition() == 0){
							return true;
						}
						else{
							mOnItemReceiver.onItemClick(ListViewDragDrop.this, child, i + getFirstVisiblePosition(), getItemIdAtPosition(i));
						}
					}
					return true;
				}
				
				Rect viewRect2 = new Rect();
				left = child.getLeft() + this.getLeft();
				right = child.getRight() + this.getLeft();
				//如果範圍是在這個child的下半部
				top = child.getTop()  + child.getHeight()/2 + this.getTop();
				bottom = child.getBottom() + this.getTop();
				viewRect2.set(left, top, right, bottom);
				
				if(viewRect2.contains(x,y)){
					//如果是drag的那個listview抓取那個item
					if(getOnItemSelectedListener() != null){
						getOnItemSelectedListener().onItemSelected(ListViewDragDrop.this, child, i + getFirstVisiblePosition(), getItemIdAtPosition(i));
					}
					//如果是drop的那個listview加入item到i + 1 position
					if(mOnItemReceiver != null){
						mOnItemReceiver.onItemClick(ListViewDragDrop.this, child, i + 1 + getFirstVisiblePosition(), getItemIdAtPosition(i));
					}
					return true;
				}
			}

			//抓取listview整個的範圍
			//給receive ListView用
			int left = this.getLeft();
			int right = this.getRight();
			int top = this.getTop();
			int bottom = this.getBottom();
			Rect rect = new Rect(left, top, right, bottom);

			if(rect.contains(x,y)){
				//如果有item
				if(this.getChildCount() > 0){
					//抓取最下面的child的位置(最下方)
					int  maxY = this.getChildAt(this.getChildCount() - 1).getBottom();
					//抓取最上面child的位置(最上方)
					int minY = this.getChildAt(0).getTop();

					if(y < minY && getFirstVisiblePosition() != 0){
						mOnItemReceiver.onItemClick(ListViewDragDrop.this, null, 0, 0);
					}else if(y > maxY){
						mOnItemReceiver.onItemClick(ListViewDragDrop.this, null, this.getChildCount() , 0);
					}
					return true;
				//如果沒有item
				}else{
					//加入成第一個item
					if(mOnItemReceiver != null){
						mOnItemReceiver.onItemClick(ListViewDragDrop.this, null, 0, 0);
					}
					return true;
				}
			}
		}
		return false;
	}

	public boolean onMove(MotionEvent event) {
		if(!isEnable){
			return true;
		}
		int x = (int)event.getX();
		int y = (int)event.getY();

		if(event.getAction() == MotionEvent.ACTION_DOWN){
			//抓取點選的item position並計算手指跟螢幕的offset
			setmStartPosition(pointToPosition(x,y));
			if (getmStartPosition() != INVALID_POSITION) {
				mItemPosition = getmStartPosition() - getFirstVisiblePosition();
				View item = (View) getChildAt(mItemPosition);
				mDragPoint = y - item.getTop();
				mCoordOffset = ((int)event.getRawY()) - y;
                mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                mDragPointOffset -= ((int)event.getRawY()) - y;
                View child = getChildAt(mItemPosition);
                childSelected = child;
                xInit = x;
                yInit = y;
				Rect listBounds=new Rect();
				getGlobalVisibleRect(listBounds, null);
				leftBound = listBounds.left;
                if(getOnItemSelectedListener() != null){
					getOnItemSelectedListener().onItemSelected(ListViewDragDrop.this, child, getmStartPosition(), 0);
				}
			}
			return super.onTouchEvent(event);
		}

		if(event.getAction() == MotionEvent.ACTION_MOVE){
			if(this.getChildCount() == 0){
				return true;
			}
			if(isScroll){
				return super.onTouchEvent(event);
			}
			if(!isDraggable){
				return super.onTouchEvent(event);
			}
			if(!isMove){
				//如果x移動超過0 px, 來判斷使用者是想要scroll listview還是移動icon
				//10這個變數可以再做調整 --> 改為判斷x位移大於y位移表示拖拉item，加3是為了讓scroll時更不容易拖拉
				if( Math.abs(x - xInit) > Math.abs(y - yInit)+3){
					if(mOnItemMoveListener != null){
						mOnItemMoveListener.onTouch(ListViewDragDrop.this, event);
					}
		            xInit = x;
		            yInit = y;
					isMove = true;
					return true;

				}else if(Math.abs(x - xInit) < 10 && Math.abs(y - yInit) < 5){
					//如果onMove跟onDown間的距離太接近時 不處理
					return super.onTouchEvent(event);
				}else{
					isScroll = true;
					isMove = false;
					return super.onTouchEvent(event);
				}
				
			}else{
				//如果已經在拖
	            xInit = x;
	            yInit = y;
				mOnItemMoveListener.onTouch(ListViewDragDrop.this, event);
				return true;
			}
		}
		
		if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
			isScroll = false;
			if(mOnItemMoveListener != null){
				mOnItemMoveListener.onTouch(ListViewDragDrop.this, event);
			}
		}

		if(event.getAction() == MotionEvent.ACTION_UP && isMove){
			int left = this.getLeft();
			int right = this.getRight();
			int top = this.getTop();
			int bottom = this.getBottom();
			Rect rect = new Rect(left, top, right, bottom);
			
			if(mOnItemMoveListener != null){
				mOnItemMoveListener.onTouch(ListViewDragDrop.this, event);
			}
			//當item被拖移開原本的listview後 檢查是不是有在接收的listview上
			if(!rect.contains(x, y)){
				
				if(mOnItemOutUpListener != null){
					mOnItemOutUpListener.onTouch(this.childSelected, event);
				}

			}
			isMove = false;
			return false;
		}
		
		return false;
	}

	public ImageView getImageAtPosition() {
		View item = getChildAt(mItemPosition);
		item.setDrawingCacheEnabled(true);
		// Create a copy of the drawing cache so that it does not get recycled
		// by the framework when the list tries to clean up memory
		Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
		item.setDrawingCacheEnabled(false);
		WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = leftBound;
        mWindowParams.y = yInit + mDragPoint + mCoordOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        Context context = null;
        if(mContext == null){
        	context= getContext();
		}else{
			context = mContext;
		}
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);     
        WindowManager mWindowManager = (WindowManager)context.getSystemService("window");
		mWindowManager.addView(v, mWindowParams);
		return v;
	}

	public void setmContext(Context mContext) {
		this.mContext = mContext;
	}

	public Context getmContext() {
		return mContext;
	}

	public void setmStartPosition(int mStartPosition) {
		this.mStartPosition = mStartPosition;
	}

	public int getmStartPosition() {
		return mStartPosition;
	}

	public void setDraggable(boolean isDraggable) {
		this.isDraggable = isDraggable;
	}

	public boolean isEnable() {
		return isEnable;
	}

	public void setEnable(boolean isEnable) {
		this.isEnable = isEnable;
	}

	public boolean isDraggable() {
		return isDraggable;
	}

}
