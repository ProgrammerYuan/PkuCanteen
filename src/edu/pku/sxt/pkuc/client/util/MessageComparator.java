package edu.pku.sxt.pkuc.client.util;

import java.util.Comparator;

import edu.pku.sxt.pkuc.client.Messages;

public class MessageComparator implements Comparator<Messages>{
	@Override
	public int compare(Messages m1, Messages m2) {
		// TODO Auto-generated method stub
		int ret = m1.time < m2.time ? 1:0;
		return ret;
	}

}
