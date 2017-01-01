package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.RedemptionPeriodBean;


@Remote
public interface RedemptionPeriodsRemote {

	public static final String NAME = "RedemptionPeriods";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/RedemptionPeriods";
	
	public RedemptionPeriodBean find(long rpId);

	public RedemptionPeriodBean create(RedemptionPeriodBean rp);

	public RedemptionPeriodBean update(RedemptionPeriodBean rp);

	public void delete(long rpId);

}