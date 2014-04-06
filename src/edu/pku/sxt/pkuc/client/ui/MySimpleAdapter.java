package edu.pku.sxt.pkuc.client.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/**
 * Customized adapter for ListView or GridView.
 * Define how to bind data with view in method:
 * 		bindView(int, View)
 * @author songxintong
 *
 */
public class MySimpleAdapter extends SimpleAdapter {
	private static final String LOG_TAG = "MySimpleAdapter";
	
	private int[] mTo;
	private String[] mFrom;
	private ViewBinder mViewBinder;
	private List<? extends Map<String, ?>> mData;
	private int mResource;
	private LayoutInflater mInflater;
	private Context context;
	
	public MySimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
		this.context = context;
		mData = data;
		mResource = resource;
		mFrom = from;
		mTo = to;
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public View getView(int position, View convertView, ViewGroup parent){
		return createViewFromResource(position, convertView, parent, mResource);
	}

	private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
		View v;
		if(convertView == null){
			v = mInflater.inflate(resource,  parent, false);
			
			final int[] to =mTo;
			final int count = to.length;
			final View[] holder = new View[count];
			
			for(int i=0; i<count; i++){
				holder[i] = v.findViewById(to[i]);
			}
			
			v.setTag(holder);
		} else {
			v = convertView;
		}
		bindView(position, v);
		
		return v;
	}
	
	private void bindView(int position, View view){
		final Map dataSet = mData.get(position);
		if(dataSet == null){
			return;
		}
		final ViewBinder binder = mViewBinder;
		final View[] holder = (View[]) view.getTag();
		final String[] from = mFrom;
		final int[] to = mTo;
		final int count = to.length;
		
		for (int i = 0; i < count; i++) {  
            final View v = holder[i];         
            if (v != null) {  
                final Object data = dataSet.get(from[i]);  
                String text = data == null ? "" : data.toString();  
                if (text == null) {  
                    text = "";  
                }  
                boolean bound = false;  
                if (binder != null) {  
                    bound = binder.setViewValue(v, data, text);  
                }  
                if (!bound) {  
                    if (v instanceof Checkable) {  
                        if (data instanceof Boolean) {  
                            ((Checkable) v).setChecked((Boolean) data);  
                        } else {  
                            throw new IllegalStateException(v.getClass().getName() +  
                                    " should be bound to a Boolean, not a " + data.getClass());  
                        }  
                    } else if(v instanceof ImageButton){
	                	if(data instanceof Object[]){
	                		ImageButton ib = (ImageButton) v;
	                		Object[] obj = (Object[])data;
	                		ib.setImageResource((Integer)obj[0]);
	                		ib.setEnabled((Boolean)obj[1]);
	                		ib.setOnClickListener((Button.OnClickListener)obj[2]);
	                	}
	                } else if (v instanceof TextView) {  
                        // Note: keep the instanceof TextView check at the bottom of these  
                        // ifs since a lot of views are TextViews (e.g. CheckBoxes).  
	                	((TextView) v).setText(text);  
                    } else if (v instanceof ImageView) {  
                    	Bitmap bitmap;
                        if (data instanceof Integer) {  
                        	((ImageView)v).setImageResource((Integer)data);
                        	continue;
                        	 //bitmap = BitmapFactory.decodeResource(null, (Integer) data);
                        }   
                        else if(data instanceof byte[]) {      //备注1 
                        	byte[] image = (byte[])data; 
                        	bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);   
                        } else if (data instanceof Uri) {
                        	try {
								bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), (Uri)data);
							} catch (IOException e) {
								Log.e(LOG_TAG, e.getMessage());
								return;
							}
                        } else if(data instanceof Bitmap) {
                        	bitmap = (Bitmap)data;
                        } else {
                        	continue;
                        }
                        ((ImageView) v).setImageBitmap(bitmap); 
                    }
	                else if(v instanceof RatingBar){ 
	                    float score = Float.parseFloat(data.toString());  //备注2  
	                    ((RatingBar)v).setRating(score);  
	                }
	                else {  
	                    throw new IllegalStateException(v.getClass().getName() + " is not a " +  
	                            " view that can be bounds by this SimpleAdapter");  
                    }   
	            } 
	        }
	    }
	}
}
