package com.coupon.process.session.remote;

import javax.ejb.Remote;

import com.coupon.process.message.AcquireCouponMessage;
import com.coupon.process.message.AcquisitionConfirmationMessage;


@Remote
public interface DistributionRemote {

	public static final String NAME = "Distribution";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/Distribution";
	
	public AcquisitionConfirmationMessage acquire(AcquireCouponMessage acm);

}