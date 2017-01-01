package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.RewardLoyaltyPointBean;


@Remote
public interface RewardLoyaltyPointsRemote {

	public static final String NAME = "RewardLoyaltyPoints";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/RewardLoyaltyPoints";
	
	public RewardLoyaltyPointBean find(long rlpId);

	public RewardLoyaltyPointBean create(RewardLoyaltyPointBean rlp);

	public RewardLoyaltyPointBean update(RewardLoyaltyPointBean rlp);

	public void delete(long rlpId);

}