package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.AcquisitionPeriodBean;


@Remote
public interface AcquisitionPeriodsRemote {

	public static final String NAME = "AcquisitionPeriods";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/AcquisitionPeriods";
	
	public AcquisitionPeriodBean find(long apId);

	public AcquisitionPeriodBean create(AcquisitionPeriodBean ap);

	public AcquisitionPeriodBean update(AcquisitionPeriodBean ap);

	public void delete(long apId);

}