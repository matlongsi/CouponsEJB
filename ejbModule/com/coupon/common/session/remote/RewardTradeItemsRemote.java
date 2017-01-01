package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.RewardTradeItemBean;


@Remote
public interface RewardTradeItemsRemote {

	public static final String NAME = "RewardTradeItems";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/RewardTradeItems";
	
	public RewardTradeItemBean find(long rtiId);

	public RewardTradeItemBean create(RewardTradeItemBean rti);

	public RewardTradeItemBean update(RewardTradeItemBean rti);

	public void delete(long rtiId);

}