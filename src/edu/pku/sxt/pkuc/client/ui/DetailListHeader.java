package edu.pku.sxt.pkuc.client.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import edu.pku.sxt.pkuc.client.R;

/**
 * Header View of Comments List in Detail Activity.
 * @author songxintong
 *
 */
public class DetailListHeader extends LinearLayout {
	
	ImageView canteenImg;
	TextView canteenTitle;
	TextView canteenTime;
	TextView canteenLevel;
	RatingBar canteenLvRate;
	ImageButton ibb, ibg;
	
	public DetailListHeader(Context context) {
		super(context);
		
		View view = LayoutInflater.from(context)
				.inflate(R.layout.detail_list_header, null); 
		
		addView(view);
		
		canteenImg = (ImageView)view.findViewById(R.id.canteen_img);
		canteenTitle = (TextView)view.findViewById(R.id.canteen_title);
		canteenLvRate = (RatingBar)view.findViewById(R.id.canteen_lv_rate);
		canteenLevel = (TextView)view.findViewById(R.id.canteen_level);
		
		ibg = (ImageButton)view.findViewById(R.id.canteen_ib_good);
		ibb = (ImageButton)view.findViewById(R.id.canteen_ib_bad);
	}

}
