package edu.pku.sxt.pkuc.client.util;

import java.util.Comparator;
import java.util.Date;

import android.location.Location;
import edu.pku.sxt.pkuc.client.Canteen;

/**
 * Utility to compare canteens according to level and distance.
 * @author songxintong
 *
 */
public class CanteenDistanceComparator implements Comparator<Canteen>{
	
	private double lat, lng;
	private boolean hasLoc;
	
	/**
	 * 
	 * @param loc User location, null if location is unknown.
	 */
	public CanteenDistanceComparator(Location loc){
		if(loc != null && loc.getTime() + 5 * 60 * 1000 > new Date().getTime()){
			hasLoc = true;
			lat = loc.getLatitude();
			lng = loc.getLongitude();
		} else {
			hasLoc = false;
		}
	}
	
	@Override
	public int compare(Canteen l, Canteen r) {
		if(hasLoc){
			float[] ld = new float[5];
			float[] rd = new float[5];
			Location.distanceBetween(lat, lng, l.lat, l.lng, ld);
			Location.distanceBetween(lat, lng, r.lat, r.lng, rd);
			return ld[0] < rd[0] ? -1 : 1;
		} else if(l.level < 0){
			return 1;
		} else if(r.level < 0){
			return -1;
		} else {
			return l.level < r.level ? -1 : 1;
		}
	}
}
