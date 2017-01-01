package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.bean.DistributionDetailBean;


@Remote
public interface DistributionDetailsRemote {

	public static final String NAME = "DistributionDetails";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/DistributionDetails";
	
	public DistributionDetailBean find(long ddId);

	public DistributionDetailBean findByCouponNumber(GlobalCouponNumber gcn);

	public DistributionDetailBean create(DistributionDetailBean dd);

	public DistributionDetailBean update(DistributionDetailBean dd);

	public void delete(long ddId);

}