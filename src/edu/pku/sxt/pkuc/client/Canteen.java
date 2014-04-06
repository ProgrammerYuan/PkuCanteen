package edu.pku.sxt.pkuc.client;

import android.graphics.Bitmap;

/**
 * Data structure of canteen information.
 * @author songxintong
 *
 */
public class Canteen {
	
	public int id;
	public String title;
	public double lat;
	public double lng;
	public double level;	// latest crowd level
	public String comment;	// latest comment
	public long time;	// time of the latest information summarized
	public Bitmap bitmap;
	public int reward;
	public int iid;	// information id
	public int fb;	// feedback	
}
