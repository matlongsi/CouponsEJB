package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.RewardBean;


@Remote
public interface RewardsRemote {

	public static final String NAME = "Rewards";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/Rewards";
	
	public RewardBean find(long rId);

	public RewardBean create(RewardBean r);

	public RewardBean update(RewardBean r);

	public void delete(long rId);

}