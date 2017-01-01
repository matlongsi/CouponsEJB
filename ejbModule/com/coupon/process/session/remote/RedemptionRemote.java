package com.coupon.process.session.remote;

import javax.ejb.Remote;

import com.coupon.process.message.RedemptionNotificationMessage;
import com.coupon.process.message.RedemptionValidationRequestMessage;
import com.coupon.process.message.RedemptionValidationResponseMessage;


@Remote
public interface RedemptionRemote {

	public static final String NAME = "Redemption";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/Redemption";
	
	public RedemptionValidationResponseMessage validateRedemption(RedemptionValidationRequestMessage vrrm);
	public void notifyRedemption(RedemptionNotificationMessage rnm);

}